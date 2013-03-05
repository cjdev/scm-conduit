#!/usr/bin/python
import sys
import os
import commands
import httplib
import json
import time
from urlparse import urlparse

def exit_with(msg):
    """ Exit with a non-zero status and print a message to stdout """
    print(msg)
    exit(1)


def request(url, method):
    """ use httplib to connect to a url for a given method """
    (host, path) = urlparse(url)[1:3]
    if host and path:
        try:
            client = httplib.HTTPConnection(host)
            client.request(method, path)
            return client.getresponse().read()
        except Exception, e:
            print(colerr(str(e)))
            exit_with("Couldn't %s to %s" % (method, url))
    else:
        exit_with("I wasn't able to parse: " + colorize(url, "red") + "\n" +
                  "Be sure to use the format: " + colorize("scheme://hostname/conduit", "green") +
                  "\n\n" +
                  "parsed host: %s\n" % host +
                  "parsed repo: %s" % path)


def get(url):
    return request(url, "GET")

for line in sys.stdin:
    # do something...
    print line,

repo = "THE_REPO"
print("talking to " + repo + "\n")
while True:
	status = get(repo)
	if status.startswith("WORKING"):
	    sys.stdout.write(".")
	    sys.stdout.flush()
	    time.sleep(0.25)
	else:
	    exit_with("RESULT " + status + ".")
