import pyttsx3


def tts(text):

    print(text)
    engine = pyttsx3.init()
    engine.say(text)
    engine.runAndWait()
    engine.stop()



if __name__ == "__main__":
    tts("Hello")