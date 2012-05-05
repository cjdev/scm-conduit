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

#bridge = sys.argv[1]
#destination = sys.argv[2]

#print "destination: " + destination
#print "bridge: " + bridge


response = post(destinationUri)
print "Response:" + response
data = json.loads(response)

pushLocation = data['pushLocation']
resultLocation = data['resultLocation']

print "Pushing to " + pushLocation

os.system('bzr push ' + pushLocation)

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



