import datetime
import play_audio
import send_email


email = "kylezcc@gmail.com"

def relieveHunger():
    send_email.send_email("Hunger", email)
    return
def relieveBurping():
    send_email.send_email("Burping", email)
    return
def relieveBellyPain():
    send_email.send_email("Belly Pain", email)
    return
def relieveDiscomfort():
    send_email.send_email("Discomfort", email)
    return
def relieveTired():
    # play_audio.play_audio("lullaby_goodnight.mp3")
    return
def relieveTemp():
    send_email.send_email("The Temperature", email)
    return	
def relieveScared():
    # play_audio.play_audio("twinkle_twinkle_little_star.mp3")
    return	
def relieveLonely():
    # play_audio.play_audio("twinkle_twinkle_little_star.mp3")
    return	
def respond(reason):
    try:
            f = open('Baby_Log.txt','a')
    except IOError:
            f = open('Baby_Log.txt', 'w')
    
    if (reason == "hu"):
            relieveHunger()
            f.write("Time:" + str(datetime.datetime.now().strftime("%d-%m-%Y %H:%M")) + '      Reason:Hunger\n')
    elif (reason == "bu"):
            relieveBurping()
            f.write("Time:" + str(datetime.datetime.now().strftime("%d-%m-%Y %H:%M")) + '      Reason:Burping\n')
    elif (reason == "bp"):
            relieveBellyPain()
            f.write("Time:" + str(datetime.datetime.now().strftime("%d-%m-%Y %H:%M")) + '      Reason:Belly Pain\n')
    elif (reason == "dc"):
            relieveDiscomfort()
            f.write("Time:" + str(datetime.datetime.now().strftime("%d-%m-%Y %H:%M")) + '      Reason:Discomfort\n')
    elif (reason == "ti"):
            relieveTired()
            f.write("Time:" + str(datetime.datetime.now().strftime("%d-%m-%Y %H:%M")) + '      Reason:Tired\n')
            f.close()
            return "code: lullaby"
    elif (reason == "lo"):
            relieveLonely()
            f.write("Time:" + str(datetime.datetime.now().strftime("%d-%m-%Y %H:%M")) + '      Reason:Lonely\n')
            f.close()
            return "code: calm"
    elif (reason == "ch"):
            relieveTemp()
            f.write("Time:" + str(datetime.datetime.now().strftime("%d-%m-%Y %H:%M")) + '      Reason:Too Hot/Cold\n')
    elif (reason == "sc"):
            relieveScared()
            f.write("Time:" + str(datetime.datetime.now().strftime("%d-%m-%Y %H:%M")) + '      Reason:Scared\n')
            f.close()
            return "code: calm"
    else:
            print("Unknown reason: " + reason)
    f.close()
    return "blank"
    
if __name__ == "__main__":
    relieveHunger()
