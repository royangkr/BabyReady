
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
 
 
def send_email(reason, email):
    

    fromaddr = "" # insert sender email address here
    toaddr = email
    msg = MIMEMultipart()
    msg['From'] = fromaddr
    msg['To'] = toaddr
    msg['Subject'] = "Notification from Baby Ready"
     
    body = "Your child is currently crying likely due to " + reason
    msg.attach(MIMEText(body, 'plain'))
     
    server = smtplib.SMTP('smtp.gmail.com', 587)
    server.starttls()
    server.login(fromaddr, "") # insert sender email password here
    text = msg.as_string()
    server.sendmail(fromaddr, toaddr, text)
    server.quit()
    

if __name__ == "__main__":
    send_email("","" ) # insert reciever email here
