# Baby Ready
By Ang Kang Rong, Roy and Kyle Zheng Ching Chan, with help from Tang Yetong

NUS High School of Math & Science, Singapore
## Code Architecture
https://docs.google.com/presentation/d/1KGU18a1N-L4RJavcVPifYgLgZQPCfYvwJ1LAZQHNNzY/edit?usp=sharing

## How to run
### Creating model
Add data to BabyReady/BabyReady - Python/data/

Run BabyReady/BabyReady - Python/train_set.py

Run BabyReady/BabyReady - Python/train_model.py

A sample model has already been created in BabyReady/BabyReady - Python/output/. You may continue with this if you do not want to create a new model yourself.
### Running server and client
Start server by running **BabyReady/BabyReady - Python/try_firebase.py** on your computer

Start client by running android **apk** built from Android Project in BabyReady/BabyReady - Android/

Alternatively, just run BabyReady/BabyReady - Python/BabyCryRecording on your computer

## Warning
You will have to install the neccessary modules in order to run the python codes (FFmpeg, pyrebase etc)

Auth tokens for firebase and google cloud have been removed to prevent misuse of our google cloud service account

## Misc
Code for each reason for baby cry:

bp - belly pain

bu - burp

ch - temperature

dc - discomfort

hu - hungry

lo - lonely

sc - scared

ti - tired
