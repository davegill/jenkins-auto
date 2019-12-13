import os
import boto3
from botocore.exceptions import ClientError
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.application import MIMEApplication
import sys
import os
SENDER = 'jenkins@no-reply.scalacomputing.com'
SENDERNAME = 'no-reply-jenkins'
FILE_NAME = sys.argv[1]
BUILD_STATUS=sys.argv[2]
JOB_NAME=sys.argv[3]
BUILD_NUMBER=sys.argv[4]
RECIPIENT=sys.argv[5]
COMMIT_ID=sys.argv[6]
REQUESTOR=sys.argv[7]
PULLNUMBER=sys.argv[8]
AWS_REGION = "us-east-1"
DAVID_GILL="dave@ucar.edu"
HEMANT="hemant.kumar@svam.com"
# The subject line for the email.
#Subject Line
SUBJECT =("{}-{}-{}").format(BUILD_STATUS,JOB_NAME,BUILD_NUMBER)
#Pass HTML Body
HTML_BODY_PASS="""
<html>
<head></head>
<body>
  <h1>{}: {}-BUILD-{}</h1>
	<p>Please find result of the WRF regression test cases in the attachment.This build is for Commit ID: {}, requested by: {} for PR: https://github.com/wrf-model/WRF/pull/{}.
        For any query please send e-mail to <a href="mailto:gill@ucar.edu">David Gill</a></p>
</body>
</html>""".format(BUILD_STATUS,JOB_NAME,BUILD_NUMBER,COMMIT_ID,REQUESTOR,PULLNUMBER)

#Fail/Aborted HTML Body
HTML_BODY_FAIL="""
<html>
<head></head>
<body>
  <h1>{}: {}-BUILD-{}</h1>
 <p>This WRF-Model build has {}. This build is for Commit ID: {}, requested by: {} for PR: https://github.com/wrf-model/WRF/pull/{}.
    For any query please send e-mail to <a href="mailto:gill@ucar.edu">David Gill</a></p>
</body>
</html>""".format(BUILD_STATUS,JOB_NAME,BUILD_NUMBER,BUILD_STATUS,COMMIT_ID,REQUESTOR,PULLNUMBER)

# The full path to the file that will be attached to the email.
ATTACHMENT = FILE_NAME
CHARSET = "utf-8"
client = boto3.client('ses',region_name=AWS_REGION)
msg = MIMEMultipart('mixed')
# Add subject, from and to lines.
msg['Subject'] = SUBJECT
msg['From'] = SENDER
msg['To'] = RECIPIENT
if (BUILD_STATUS=="SUCCESS"):
    msg_body = MIMEMultipart('alternative')
    htmlpart = MIMEText(HTML_BODY_PASS.encode(CHARSET), 'html', CHARSET)
    msg_body.attach(htmlpart)
    # Define the attachment part and encode it using MIMEApplication.
    att = MIMEApplication(open(ATTACHMENT, 'rb').read())
    att.add_header('Content-Disposition','attachment',filename=os.path.basename(ATTACHMENT))
    msg.attach(msg_body)

    # Add the attachment to the parent container.
    msg.attach(att)
    try:
        #Provide the contents of the email.
        response = client.send_raw_email(
            Source=SENDER,
            Destinations=[
                RECIPIENT,DAVID_GILL,HEMANT
            ],
            RawMessage={
                'Data':msg.as_string(),
            },
        )
    # Display an error if something goes wrong.
    except ClientError as e:
        print(e.response['Error']['Message'])
    else:
        print("Email sent! Message ID:"),
        print(response['MessageId'])
if (BUILD_STATUS=="FAILURE"):
    msg_body = MIMEMultipart('alternative')
    htmlpart = MIMEText(HTML_BODY_FAIL.encode(CHARSET), 'html', CHARSET)
    msg_body.attach(htmlpart)
    # Define the attachment part and encode it using MIMEApplication.
    msg.attach(msg_body)
    try:
        #Provide the contents of the email.
        response = client.send_raw_email(
            Source=SENDER,
            Destinations=[
                RECIPIENT,DAVID_GILL,HEMANT
            ],
            RawMessage={
                'Data':msg.as_string(),
            },
        )
    # Display an error if something goes wrong.
    except ClientError as e:
        print(e.response['Error']['Message'])
    else:
        print("Email sent! Message ID:"),
        print(response['MessageId'])
