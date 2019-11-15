import smtplib
import email.utils
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
import datetime
from os.path import basename
from email import encoders
import sys

#Setting command line arguments
SENDER = 'no-reply-jenkins@scalacomputing.com'
SENDERNAME = 'no-reply-jenkins'
FILE_NAME = sys.argv[1]
BUILD_STATUS=sys.argv[2]
JOB_NAME=sys.argv[3]
BUILD_NUMBER=sys.argv[4]
RECEPIENT=sys.argv[5]
RECIPIENTS  = ['hkumar@scalacomputing.com',RECEPIENT]
HOST = "email-smtp.us-west-2.amazonaws.com"
PORT = 587
# Replace smtp_username with your Amazon SES SMTP user name.
USERNAME_SMTP = sys.argv[6]

# Replace smtp_password with your Amazon SES SMTP password.
PASSWORD_SMTP = sys.argv[7]

#Pass HTML Body
HTML_BODY_PASS="""
<html>
<head></head>
<body>
  <h1>{}: {}-BUILD-{}</h1>
	<p>Please find result of the test cases in the attachment. For any query please send e-mail to <a href="mailto:gill@ucar.edu">David Gill</a></p>
</body>
</html>""".format(BUILD_STATUS,JOB_NAME,BUILD_NUMBER)

#Fail/Aborted HTML Body
HTML_BODY_FAIL="""
<html>
<head></head>
<body>
  <h1>{}: {}-BUILD-{}</h1>
 <p>This WRF-Model build has {}. For any query please send e-mail to <a href="mailto:gill@ucar.edu">David Gill</a></p>
</body>
</html>""".format(BUILD_STATUS,JOB_NAME,BUILD_NUMBER,BUILD_STATUS)

#Subject Line
SUBJECT =("{}-{}-{}").format(BUILD_STATUS,JOB_NAME,BUILD_NUMBER)
if (BUILD_STATUS=="SUCCESS"):
	BODY_TEXT=HTML_BODY_PASS
        # Create message container - the correct MIME type is multipart/alternative.
    	msg = MIMEMultipart('alternative')
    	msg['Subject'] = SUBJECT
    	msg['From'] = email.utils.formataddr((SENDERNAME, SENDER))
    	msg['To'] = ', '.join(RECIPIENTS)
    	part1 = MIMEText(BODY_TEXT, 'html')
    	msg.attach(part1)
    	attach_file = open(FILE_NAME, 'rb')
    	payload = MIMEBase('application', 'octate-stream')
    	payload.set_payload((attach_file).read())
    	encoders.encode_base64(payload)
    	payload.add_header('Content-Disposition','attachment; filename="{}"'.format(FILE_NAME.rsplit("/",1)[1]))
    	msg.attach(payload)
elif(BUILD_STATUS=="FAILURE"):
	BODY_TEXT=HTML_BODY_FAIL
 	# Create message container - the correct MIME type is multipart/alternative.
    	msg = MIMEMultipart('alternative')
    	msg['Subject'] = SUBJECT
    	msg['From'] = email.utils.formataddr((SENDERNAME, SENDER))
    	msg['To'] = ', '.join(RECIPIENTS)
    	part1 = MIMEText(BODY_TEXT, 'html')
    	msg.attach(part1)
    	payload = MIMEBase('application', 'octate-stream')
    	encoders.encode_base64(payload)
    	msg.attach(payload)
elif(BUILD_STATUS=="ABORTED"):
	BODY_TEXT=HTML_BODY_FAIL
    	# Create message container - the correct MIME type is multipart/alternative.
    	msg = MIMEMultipart('alternative')
    	msg['Subject'] = SUBJECT
	msg['From'] = email.utils.formataddr((SENDERNAME, SENDER))
    	msg['To'] = ', '.join(RECIPIENTS)
    	part1 = MIMEText(BODY_TEXT, 'html')
    	msg.attach(part1)
    	payload = MIMEBase('application', 'octate-stream')
    	encoders.encode_base64(payload)
    	msg.attach(payload)
for idx,RECIPIENT in enumerate(RECIPIENTS):
# Try to send the the message.
    try:
        server = smtplib.SMTP(HOST, PORT)
        server.ehlo()
        server.starttls()
        server.ehlo()
        server.login(USERNAME_SMTP, PASSWORD_SMTP)
        server.sendmail(SENDER, RECIPIENTS[idx], msg.as_string())
        server.close()
    # Display an error message if something goes wrong.
    except Exception as e:
        print ("Error: ", e)
    else:
        print ("Email sent successfully to {},{}").format("hkumar@scalacomputing.com",RECEPIENT)
