#!/usr/bin/python

import sys
import os
import httplib
import json
import time
from urlparse import urlparse

def get(uriText):
    #print "making request for " + uriText
    uri = urlparse(uriText)
    host = uri.netloc
    path = uri.path
    client = httplib.HTTPConnection(host)
    client.request('GET', path)
    response = client.getresponse().read()
    return response

def post(uriText):
    uri = urlparse(uriText)
    host = uri.netloc
    path = uri.path
    client = httplib.HTTPConnection(host)
    client.request('POST', path)
    response = client.getresponse().read()
    return response

destinationUri = sys.argv[1]
print "destination: " + destinationUri

if len(sys.argv) > 2:
    print "length is " + str(len(sys.argv))
    user = sys.argv[2]
else:
    user = None

#bridge = sys.argv[1]
#destination = sys.argv[2]

#print "destination: " + destination
#print "bridge: " + bridge


response = post(destinationUri)
print "Response:" + response
data = json.loads(response)

pushLocation = data['pushLocation']
resultLocation = data['resultLocation']

if user != None :
    pushLocation = pushLocation \
                    .replace("sftp://", "sftp://" + user + "@") \
                    .replace("ssh://", "ssh://" + user + "@")

print "Pushing to " + pushLocation

if os.path.exists(".git"):
    cmd = "git push " + pushLocation
elif os.path.exists(".bzr"):
    cmd = "bzr push " + pushLocation
else:
    raise Exception("This doesn't appear to be a directory that is tracked by a known scm")
    
os.system(cmd)

while(True):
  urlParts = urlparse(destinationUri)
  status = get(urlParts.scheme + "://" + urlParts.netloc + resultLocation)
  if(status.startswith("WORKING") == True):
      sys.stdout.write(".")
      sys.stdout.flush()
      time.sleep(.25)
  else:
      print "done"
      print status
      break



