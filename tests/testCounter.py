#!/usr/bin/env python
import os
import subprocess
import time

# Which server to use:
serverPath = "http://localhost:8888"
serverPath = "http://gaetxtest.appspot.com"

# Which of the servelets to test:
apiPath = "/counter"

# Amount of time (seconds) to sleep between requests. Use negative value to completely disable sleep
sleepDuration = -10.1

# Clear all Data first
os.spawnlp(os.P_WAIT, 'curl', 'curl', '--data', "dummy=dummy", serverPath+apiPath+"/clearAll")

mobSize = 400

print "Settings:"
print "Server path:", serverPath

for i in range(mobSize):
  data = 'dummy=dummy'
  fd = open("out/testCounter"+str(i)+".txt", "w+")
  subprocess.Popen(["curl", "-s", "--data", data, serverPath+apiPath+"/increment"], stdout=fd)
  if (sleepDuration >= 0):
    time.sleep(sleepDuration)
