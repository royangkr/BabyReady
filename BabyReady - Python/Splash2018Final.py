import pyrebase
from sys import byteorder
from array import array
from struct import pack

import pyaudio
import wave
import datetime
import sys
import test_model
import os

from nlp import *
from Respond import *
from speech_recogniser import *
from test_model import main

config = {
  "apiKey": "AIzaSyDLb3AiOCoWh0AXxztlf6IeAFWccaR22YA",
  "authDomain": "sa2018-8267e.firebaseapp.com",
  "databaseURL": "https://sa2018-8267e.firebaseio.com",
  "storageBucket": "sa2018-8267e.appspot.com",
  "serviceAccount": "firebase.json"
}

BABY_REC = False
count={'bp':0,'bu':0,'ch':0,'dc':0,'hu':0,'lo':0,'sc':0,'ti':0}

def stream_handler(message):
    global BABY_REC
    global count
    if message["event"]=="put" and message["path"]!="/" and message["data"]!=None:
        print(message["path"])
        storage.child(message["data"]).download("firebase.wav")
        response = recognize_speech_from_file("firebase.wav") #change file name to file from firebase
		
	# If speech recognizer is unable to decipher audio file, it would either mean that 1) The baby is crying, or 2) The person speaking sucks at speaking.
	# In our case we will assume the user has perfect recognizable speech
	# If speech is recognized, then we will assume it is the user that is issuing commands to the application
        print(response)
        if(BABY_REC and response["error"]=="Unable to recognize speech"):
            prediction = test_model.main()  #predict here
            print("Prediction: "+prediction)
            responseReturn=respond(prediction)
            if not responseReturn=="":
                data = {str(message["path"])[1:]: responseReturn}
                db.child("output").set(data)
            else:
                data = {str(message["path"])[1:]: "blank"}
                db.child("output").set(data)
        elif (not response["error"]=="Unable to recognize speech"):
            inputCommand=response["transcription"]
            output=detect_intent_texts("sa2018-8267e","1",[inputCommand,], "en")
            if output=="code: start":
                if (BABY_REC == False):
                        BABY_REC = True
                        print("Starting to listen for baby cry")
                        data = {str(message["path"])[1:]: "Starting to listen for baby cry"}
                        db.child("output").set(data)
                else:
                        print("Already started listening for baby cry")
                        data = {str(message["path"])[1:]: "Already started listening for baby cry"}
                        db.child("output").set(data)
            elif (output=="code: stop"):
                    BABY_REC = False
                    print("Stopped listening for baby cry")
                    data = {str(message["path"])[1:]: "Stopped listening for baby cry"}
                    db.child("output").set(data)
            elif (output=="code: log"):
                try:
                    f = open('Baby_Log.txt')
                    text=f.read()
                    if (text == ""):
                        print("Log file is currently empty.")
                        data = {str(message["path"])[1:]: "Log file is currently empty."}
                        db.child("output").set(data)
                    else:
                        babyLogs = text.split("\n")
                        no_of_cries_today=-1
                        for x in babyLogs:
                            no_of_cries_today += 1
                        # print(text)
                        
                        print("Your baby has cried " + str(no_of_cries_today) + "times.")
                        data = {str(message["path"])[1:]: "Your baby has cried " + str(no_of_cries_today) + "times."}
                        db.child("output").set(data)
                        f.close()
                except IOError:
                    print("There are no current logs.")
                    data = {str(message["path"])[1:]: "There are no current logs."}
                    db.child("output").set(data)
            elif(output=="code: clearLogs"):
                f = open('Baby_Log.txt','w').close
                print("Logs cleared.")
                data = {str(message["path"])[1:]: "Logs cleared."}
                db.child("output").set(data)
            else:
                print(output)
                data = {str(message["path"])[1:]: output}
                db.child("output").set(data)
        else:
            data = {str(message["path"])[1:]: "blank"}
            db.child("output").set(data)
        db.child("input").child(message["path"]).remove()
        os.remove("firebase.wav")
firebase = pyrebase.initialize_app(config)
storage=firebase.storage()
db = firebase.database()
print("Starting to listen to firebase")
my_stream = db.child("input").stream(stream_handler)
print("Listening")

