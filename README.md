# Baby Ready
BabyReady uses a Convolutional Neural Network to predict the reason a baby is crying with a mp3 recording as input.
BabyReady was developed in 2018 by `Ang Kang Rong Roy` and `Kyle Zheng Ching Chan` with help from `Tang Yetong`. It won the champion and healthtech award at Splash Awards 2018 and first runner-up in CodeXtremeApps 2018.

## How BabyReady works
<p align="center">
	<img src="https://raw.githubusercontent.com/royangkr/BabyReady/master/FFT.JPG" height="150">
</p>
After trimming the recording of the baby cry, Fast Fourier transform is performed on the spectrogram of the audio.
<p align="center">
	<img src="https://raw.githubusercontent.com/royangkr/BabyReady/master/CNN.JPG" height="150">
</p>
The FFT is than sent as input into the pre-trained machine learning model to predict the reason for the baby's cry. We implemented a Convolutional Neural Network to extract features from the spectrogram that would differentiate the different reasons for crying.

## Interaction Lifecycle
1. Run the BabyReady android app and place device beside baby.
2. When baby cries, the cry is recorded and uploaded to the online server which predicts the reason
3. The reason is sent to the android device through an online database and the device will act based on the predicted reason. If the baby is scared or lonely, [Baby Shark](https://www.youtube.com/watch?v=gsw-de5xcCU) is played. If the baby is tired, a lullaby is played.
## How to run
### Installation
You must install both the server and android app.
1. Download the entire `BabyReady` repository.
2. Create your own [Firebase project](https://console.firebase.google.com/) and [download](https://support.google.com/firebase/answer/7015592?hl=en) the `google-servies.json` file. Add it to both `BabyReady/BabyReady - Python/auth.json` and `BabyReady/BabyReady - Android/app/google-services.json`. Create an environmental variable on your computer GOOGLE_APPLICATION_CREDENTIALS and enter as value the file location of `BabyReady/BabyReady - Python/auth.json`.
3. `pip install -r requirements.txt`.
4. Build the apk from the Android project and install in android device.
### Training the machine learning model
1. Add data to `BabyReady/BabyReady - Python/data/`
2. Run `BabyReady/BabyReady - Python/train_set.py`
3. Run `BabyReady/BabyReady - Python/train_model.py`

A sample model has already been created in `BabyReady/BabyReady - Python/output/`. You may continue with this if you do not want to create a new model yourself.
### Running server and client
Start server by running `BabyReady/BabyReady - Python/try_firebase.py` on your computer
Start client by running android `apk` built from Android Project in BabyReady/BabyReady - Android/

Alternatively, just run BabyReady/BabyReady - Python/BabyCryRecording.py on your computer

## Misc
Codes for each reason for baby cry:
bp - belly pain

bu - burp

ch - temperature

dc - discomfort

hu - hungry

lo - lonely

sc - scared

ti - tired
