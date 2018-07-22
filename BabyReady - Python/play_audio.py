import keyboard
import os
from pygame import mixer # Load the required library


def play_audio(filename):
    
    print(os.path.dirname(os.path.abspath(__file__)) + "\\" + filename)

    mixer.init()
    mixer.music.load(os.path.dirname(os.path.abspath(__file__)) + "\\" + filename)
    mixer.music.play()
    while mixer.music.get_busy() == True:
        try: 
            if keyboard.is_pressed('esc'):
                print('Music Stopped')
                mixer.music.stop()
                break
            else:
                pass
        except:
            continue

if __name__ == "__main__":
    play_audio("lullaby_goodnight.mp3")