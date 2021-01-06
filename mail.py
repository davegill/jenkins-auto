import os
import boto3
from botocore.exceptions import ClientError
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.application import MIMEApplication
import sys

#Variables
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
FILE=sys.argv[9]
PRCOMMENT=sys.argv[10]
E=sys.argv[11]
F=sys.argv[12]
G=sys.argv[13]
H=sys.argv[14]
I=sys.argv[15]
J=sys.argv[16]
K=sys.argv[17]
L=sys.argv[18]
M=sys.argv[19]
N=sys.argv[20]
O=sys.argv[21]
P=sys.argv[22]

AWS_REGION = "us-east-1"
DAVID_GILL="hstone@scalacomputing.com"
VIKRAM="vlakshmanan@scalacomputing.com"
SCALA="ncar-dev@scalacomputing.com"
f = open(FILE,"r")
contents=f.read()
print(contents)

#If either I or J is non-zero, then the title of the email message uses the word FAIL. 
#If both I and J are zero, then the title of the email message uses the word PASS.
if (I=="0") and (J=="0"):
    SUBJECT =("WRF Jenkins-PASS {} {} {}").format(REQUESTOR,PULLNUMBER,PRCOMMENT)
    #Pass HTML Body
    HTML_BODY_PASS="""
    <html>
    <head></head>
    <body>
    <p>Please find result of the WRF regression test cases in the attachment.  This build is for Commit ID: {0}, requested by: {1} for PR: https://github.com/scala-computing/WRF/pull/{2}.
            For any query please send e-mail to <a href="mailto:gill@ucar.edu">David Gill. </a></p>
    <pre>
    Test Type              | Expected  | Received |  Failed
    = = = = = = = = = = = = = = = = = = = = = = = =  = = = =
    Number of Tests        : {11}           {3}
    Number of Builds       : {12}           {4}
    Number of Simulations  : {13}           {5}        {7}
    Number of Comparisons  : {14}           {6}        {8}

    Failed Simulations are: 
    {9}
    Which comparisons are not bit-for-bit: 
    {10}
    </pre>
    </body>
    </html>""".format(COMMIT_ID,REQUESTOR,PULLNUMBER,E,F,G,H,I,J,K,L,M,N,O,P)
else :
    SUBJECT =("WRF Jenkins-FAIL {} {} {}").format(REQUESTOR,PULLNUMBER,PRCOMMENT)
    #Pass HTML Body
    HTML_BODY_PASS="""
    <html>
    <head></head>
    <body>
    <p>Please find result of the WRF regression test cases in the attachment.  This build is for Commit ID: {0}, requested by: {1} for PR: https://github.com/scala-computing/WRF/pull/{2}.
            For any query please send e-mail to <a href="mailto:gill@ucar.edu">David Gill. </a></p>
    <pre>
    Test Type              | Expected  | Received |  Failed
    = = = = = = = = = = = = = = = = = = = = = = = =  = = = =
    Number of Tests        : {11}           {3}
    Number of Builds       : {12}           {4}
    Number of Simulations  : {13}           {5}         {7}
    Number of Comparisons  : {14}           {6}         {8}

    Failed Simulations are: 
    {9}
    Which comparisons are not bit-for-bit: 
    {10}
    </pre>
    </body>
    </html>""".format(COMMIT_ID,REQUESTOR,PULLNUMBER,E,F,G,H,I,J,K,L,M,N,O,P)

#Fail/Aborted HTML Body
HTML_BODY_FAIL="""
<html>
<head></head>
<body>
  <h1>{}: {}-BUILD-{}</h1>
 <p>This WRF-Model build has {}. This build is for Commit ID: {}, requested by: {} for PR: https://github.com/scala-computing/WRF/pull/{}.
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
                RECIPIENT,DAVID_GILL,VIKRAM,SCALA
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
                RECIPIENT,DAVID_GILL,VIKRAM,SCALA
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
