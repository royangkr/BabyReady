package com.example.kyle0.babyready;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.widget.MediaController;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private static int[] mSampleRates = new int[]{8000, 11025, 22050, 44100};
    private int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int RECORDER_SAMPLERATE = 16000;
    private byte RECORDER_BPP = (byte) 16;

    private TextView tv;

    private VideoView video;

    private AudioRecord audioRecorder;

    private FirebaseDatabase database;

    private TextToSpeech textToSpeech;

    private String key;

    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupTts();

        video = (VideoView) findViewById(R.id.video);

        FirebaseApp.initializeApp(this);

        database = FirebaseDatabase.getInstance();



        requestRecordAudioPermission();
        arm();



    }

    public void audioPlayer(String fileName){
        //set up MediaPlayer
        MediaPlayer mp = new MediaPlayer();

        File f = new File(getCacheDir()+"/" + fileName);
        if (!f.exists()) try {

            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();


            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) { throw new RuntimeException(e); }

        try {

            if(fileName.equalsIgnoreCase("baby_shark.mp4")){
                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(video);
                video.setVisibility(View.VISIBLE);
                video.setMediaController(mediaController);
                video.setKeepScreenOn(true);
                video.setVideoPath(f.getPath());
                video.start();
                video.requestFocus();
                video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        thread.run();
                        video.setVisibility(View.INVISIBLE);
                    }
                });
            }
            else {
                mp.setDataSource(f.getPath());
                mp.prepare();
                mp.start();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        thread.run();
                    }
                });
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTts() {
        // TODO: Setup TTS
        textToSpeech = new TextToSpeech(this, null);
    }

    private void startTts(String text) {

        // TODO: Start TTS
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        // TODO: Wait for end and start hotword
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (textToSpeech.isSpeaking()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e("tts", e.getMessage(), e);
                    }
                }

                //
                // arm();

                thread.run();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }




    public void arm() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                // Get the minimum buffer size required for the successful creation of an AudioRecord object.
                int bufferSizeInBytes = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING);

                // Initialize Audio Recorder.
                //audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE,
                //        RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSizeInBytes);

                int bufferSize = bufferSizeInBytes;
                byte[] audioBuffer = new byte[bufferSize];
                audioRecorder = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        RECORDER_SAMPLERATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        RECORDER_AUDIO_ENCODING,
                        bufferSize
                );

                if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("hotword", "audio record fail to initialize");
                    return;
                }

                // Start Recording.
                audioRecorder.startRecording();

                int numberOfReadBytes = 0;
                audioBuffer = new byte[bufferSize];
                boolean recording = false;
                boolean in6sec = false;
                float tempFloatBuffer[] = new float[3];
                int tempIndex = 0;
                int totalReadBytes = 0;
                byte totalByteBuffer[] = new byte[60 * RECORDER_SAMPLERATE * 2];
                long time = 0;
                boolean silenceFlag = false;
                long silenceTime = 0;

                int threshold = 1000;


                // While data come from microphone.
                while (true) {
                    float totalAbsValue = 0.0f;
                    short sample = 0;

                    numberOfReadBytes = audioRecorder.read(audioBuffer, 0, bufferSize);

                    // Analyze Sound.
                    for (int i = 0; i < bufferSize; i += 2) {
                        sample = (short) ((audioBuffer[i]) | audioBuffer[i + 1] << 8);
                        totalAbsValue += Math.abs(sample) / (numberOfReadBytes / 2);
                    }

                    // Analyze temp buffer.
                    tempFloatBuffer[tempIndex % 3] = totalAbsValue;
                    float temp = 0.0f;
                    for (int i = 0; i < 3; ++i)
                        temp += tempFloatBuffer[i];

                    if (in6sec == true && System.currentTimeMillis() - time >= 3000) {
                        in6sec = false;
                    }
                    if(silenceFlag){
                        if(System.currentTimeMillis() - silenceTime >= 700){
                            silenceFlag = false;
                            in6sec = false;
                        }
                    }

                    if ((temp >= 0 && temp <= threshold) && recording == true && in6sec == true && silenceFlag == false) {
                        Log.i("TAG", "3");

                        silenceFlag = true;
                        silenceTime = System.currentTimeMillis();
                        tempIndex++;
                        continue;
                    }
                    else if((temp >= 0 && temp <= threshold) && recording == false){
                        Log.i("TAG", "1");

                        tempIndex++;
                        continue;
                    }
                    if (temp > threshold && silenceFlag == true) {
                        Log.i("TAG", "4");
                        silenceFlag = false;

                    }

                    if (temp > threshold && recording == false) {
                        Log.i("TAG", "2");

                        recording = true;
                        in6sec = true;
                        time = System.currentTimeMillis();
                    }

                    if ((temp >= 0 && temp <= threshold) && recording == true && in6sec == false) {
                        Log.i("TAG", "Save audio to file.");


                        // Save audio to file.
                        String filepath = Environment.getExternalStorageDirectory().getPath();
                        File file = new File(filepath, "AudioRecorder");
                        if (!file.exists())
                            file.mkdirs();


                        String fn = file.getAbsolutePath() + "/" + "recording.wav";
                        key = String.valueOf(System.currentTimeMillis());

                        long totalAudioLen = 0;
                        long totalDataLen = totalAudioLen + 36;
                        long longSampleRate = RECORDER_SAMPLERATE;
                        int channels = 1;
                        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;
                        totalAudioLen = totalReadBytes;
                        totalDataLen = totalAudioLen + 36;
                        byte finalBuffer[] = new byte[totalReadBytes + 44];

                        finalBuffer[0] = 'R'; // RIFF/WAVE header
                        finalBuffer[1] = 'I';
                        finalBuffer[2] = 'F';
                        finalBuffer[3] = 'F';
                        finalBuffer[4] = (byte) (totalDataLen & 0xff);
                        finalBuffer[5] = (byte) ((totalDataLen >> 8) & 0xff);
                        finalBuffer[6] = (byte) ((totalDataLen >> 16) & 0xff);
                        finalBuffer[7] = (byte) ((totalDataLen >> 24) & 0xff);
                        finalBuffer[8] = 'W';
                        finalBuffer[9] = 'A';
                        finalBuffer[10] = 'V';
                        finalBuffer[11] = 'E';
                        finalBuffer[12] = 'f'; // 'fmt ' chunk
                        finalBuffer[13] = 'm';
                        finalBuffer[14] = 't';
                        finalBuffer[15] = ' ';
                        finalBuffer[16] = 16; // 4 bytes: size of 'fmt ' chunk
                        finalBuffer[17] = 0;
                        finalBuffer[18] = 0;
                        finalBuffer[19] = 0;
                        finalBuffer[20] = 1; // format = 1
                        finalBuffer[21] = 0;
                        finalBuffer[22] = (byte) channels;
                        finalBuffer[23] = 0;
                        finalBuffer[24] = (byte) (longSampleRate & 0xff);
                        finalBuffer[25] = (byte) ((longSampleRate >> 8) & 0xff);
                        finalBuffer[26] = (byte) ((longSampleRate >> 16) & 0xff);
                        finalBuffer[27] = (byte) ((longSampleRate >> 24) & 0xff);
                        finalBuffer[28] = (byte) (byteRate & 0xff);
                        finalBuffer[29] = (byte) ((byteRate >> 8) & 0xff);
                        finalBuffer[30] = (byte) ((byteRate >> 16) & 0xff);
                        finalBuffer[31] = (byte) ((byteRate >> 24) & 0xff);
                        finalBuffer[32] = (byte) (2 * 16 / 8); // block align
                        finalBuffer[33] = 0;
                        finalBuffer[34] = RECORDER_BPP; // bits per sample
                        finalBuffer[35] = 0;
                        finalBuffer[36] = 'd';
                        finalBuffer[37] = 'a';
                        finalBuffer[38] = 't';
                        finalBuffer[39] = 'a';
                        finalBuffer[40] = (byte) (totalAudioLen & 0xff);
                        finalBuffer[41] = (byte) ((totalAudioLen >> 8) & 0xff);
                        finalBuffer[42] = (byte) ((totalAudioLen >> 16) & 0xff);
                        finalBuffer[43] = (byte) ((totalAudioLen >> 24) & 0xff);

                        for (int i = 0; i < totalReadBytes; ++i)
                            finalBuffer[44 + i] = totalByteBuffer[i];

                        FileOutputStream out;
                        try {
                            out = new FileOutputStream(fn,false);
                            try {
                                out.write(finalBuffer);
                                out.close();
                                totalByteBuffer= new byte[60 * RECORDER_SAMPLERATE * 2];
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        } catch (FileNotFoundException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }

                        // */
                        tempIndex++;

                        firebase(fn);


                        audioRecorder.release();
                        recording = false;
                        break;

                    }

                    // -> Recording sound here.
                    Log.i("TAG", "Recording Sound.");
                    for (int i = 0; i < numberOfReadBytes; i++)
                        totalByteBuffer[totalReadBytes + i] = audioBuffer[i];
                    totalReadBytes += numberOfReadBytes;
                    // */

                    tempIndex++;
                }
            }
        };


        //Threadings.runInBackgroundThread(runnable);
        thread = new Thread(runnable);
        thread.start();

    }

    private void firebase(String filename){
        /*MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(filename);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference uploadRef = storageRef.child("recording.wav");

        Uri file = Uri.fromFile(new File(filename));

        UploadTask uploadTask = uploadRef.putFile(file);


        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads

                thread.run();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
                firebaseDB();
            }
        });




    }

    private void firebaseDB(){
        DatabaseReference myRef = database.getReference("output");
        DatabaseReference myRef2 = database.getReference("input");
        myRef2.child(key).setValue("recording.wav");


        myRef.child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d("TAG", "Value is: " + value);

                if(value != null) {
                    if (value.equalsIgnoreCase("blank")) {
                        thread.run();
                    } else if (value.equalsIgnoreCase("code: calm")) {
                        audioPlayer("baby_shark.mp4");
                    } else if (value.equalsIgnoreCase("code: lullaby")) {
                        audioPlayer("lullaby_goodnight.mp3");
                    } else {
                        startTts(value);
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException());
            }
        });
    }


    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user asynchronously -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user asynchronously -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user asynchronously -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("Activity", "Granted!");
                    arm();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Activity", "Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}