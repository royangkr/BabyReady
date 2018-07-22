from sys import byteorder
from array import array
from struct import pack

import pyaudio
import wave
import datetime
import sys
import test_model

# Importing external python files
from TTS import *
from nlp import *
from Respond import *
from speech_recogniser import *
from test_model import main

THRESHOLD = 1500
CHUNK_SIZE = 1024
FORMAT = pyaudio.paInt16
RATE = 44100
BABY_REC = False
REC = True

def is_silent(snd_data):
    "Returns 'True' if below the 'silent' threshold"
    return max(snd_data) < THRESHOLD

def normalize(snd_data):
    "Average the volume out"
    MAXIMUM = 16384
    times = float(MAXIMUM)/max(abs(i) for i in snd_data)

    r = array('h')
    for i in snd_data:
        r.append(int(i*times))
    return r

def trim(snd_data):
    "Trim the blank spots at the start and end"
    def _trim(snd_data):
        snd_started = False
        r = array('h')

        for i in snd_data:
            if not snd_started and abs(i)>THRESHOLD:
                snd_started = True
                r.append(i)

            elif snd_started:
                r.append(i)
        return r

    # Trim to the left
    snd_data = _trim(snd_data)

    # Trim to the right
    snd_data.reverse()
    snd_data = _trim(snd_data)
    snd_data.reverse()
    return snd_data

def add_silence(snd_data, seconds):
    "Add silence to the start and end of 'snd_data' of length 'seconds' (float)"
    r = array('h', [0 for i in range(int(seconds*RATE))])
    r.extend(snd_data)
    r.extend([0 for i in range(int(seconds*RATE))])
    return r

def record():
    """
    Record a word or words from the microphone and 
    return the data as an array of signed shorts.

    Normalizes the audio, trims silence from the 
    start and end, and pads with 0.5 seconds of 
    blank sound to make sure VLC et al can play 
    it without getting chopped off.
    """
    print("Listening")
    p = pyaudio.PyAudio()
    stream = p.open(format=FORMAT, channels=1, rate=RATE,
        input=True, output=True,
        frames_per_buffer=CHUNK_SIZE)

    num_silent = 0
    snd_started = False
    num_passed=0

    r = array('h')

    while 1:
        # little endian, signed short
        snd_data = array('h', stream.read(CHUNK_SIZE))
        if byteorder == 'big':
            snd_data.byteswap()
        r.extend(snd_data)

        silent = is_silent(snd_data)

        if silent and snd_started:
            num_silent += 1
        elif not silent and not snd_started:
            snd_started = True

        if not silent:
            num_passed+=1
            num_silent=0

        if snd_started and num_silent > 86: #2 seconds of complete silence
            print("2 seconds silence")
            break
        if num_passed>258: #6 seconds since suddden non-silent
            print("6 seconds over")
            break

    sample_width = p.get_sample_size(FORMAT)
    stream.stop_stream()
    stream.close()
    p.terminate()
    
    r = trim(r)
    r = normalize(r)
    r = add_silence(r, 0.5)
    return sample_width, r

def record_to_file(path):
    global BABY_REC
    "Records from the microphone and outputs the resulting data to 'path'"
    count={'bp':0,'bu':0,'ch':0,'dc':0,'hu':0,'lo':0,'sc':0,'ti':0}
    finalPrediction="";
    while finalPrediction=="":
        sample_width, data = record()
		
        
        		
        data = pack('<' + ('h'*len(data)), *data)
        wf = wave.open(path, 'wb')
        wf.setnchannels(1)
        wf.setsampwidth(sample_width)
        wf.setframerate(RATE)
        wf.writeframes(data)
        wf.close()
		
        response = recognize_speech_from_file("recording.wav")
		
		# If speech recognizer is unable to decipher audio file, it would either mean that 1) The baby is crying, or 2) The person speaking sucks at speaking.
		# In our case we will assume the user has perfect recognizable speech
		# If speech is recognized, then we will assume it is the user that is issuing commands to the application
        
        if(BABY_REC and response["error"]=="Unable to recognize speech"):
            prediction = test_model.main()  #predict here
            print("Guess: "+prediction)
            count[prediction]=count[prediction]+1
            if count[prediction]>1:
                finalPrediction=prediction
        elif (not response["error"]=="Unable to recognize speech"):
            inputCommand=response["transcription"]
            output=detect_intent_texts("sa2018-8267e","1",[inputCommand,], "en")
            if output=="code: start":
                if (BABY_REC == False):
                        BABY_REC = True
                        tts("Starting to listen for baby cry")
                else:
                        tts("Already started listening for baby cry")
            elif (output=="code: stop"):
                    BABY_REC = False
                    tts("Stopped listening for baby cry")
            elif (output=="code: log"):
                try:
                    f = open('Baby_Log.txt')
                    text=f.read()
                    if (text == ""):
                        tts("Log file is currently empty.")
                    else:
                        babyLogs = text.split("\n")
                        no_of_cries_today=-1
                        for x in babyLogs:
                            no_of_cries_today += 1
                        print(text)
                        
                        tts("Your baby has cried " + str(no_of_cries_today) + "times.")
                        f.close()
                except IOError:
                    tts("There are no current logs.")
            elif(output=="code: clearLogs"):
                f = open('Baby_Log.txt','w').close
                tts("Logs cleared.")
            else:
                print(output)
    print("Final prediction: "+finalPrediction)
    print(count)
    print(respond(finalPrediction))

if __name__ == '__main__':
    BABY_REC=False
    print("Welcome to Baby Ready, the chatbot that talks to both caregivers and babies.")
    tts("Welcome to Baby Ready.")
    lastPrediction=datetime.datetime.now() - datetime.timedelta(minutes=5);
    while(1):
        
        record_to_file('recording.wav')
