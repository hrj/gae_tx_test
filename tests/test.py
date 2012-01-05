import os
import subprocess

# Which server to use:
serverPath = "http://localhost:8888"
serverPath = "http://gaetxtest.appspot.com"

# Which of the servelets to test:
apiPath = "/seatreservation"
apiPath = "/seatreservationkeybased"

# Clear all Data first
os.spawnlp(os.P_WAIT, 'curl', 'curl', '--data', "dummy=dummy", serverPath+apiPath+"/clearAll")

numSeats = 20
mobSize = 100

for i in range(mobSize):
  data = 'ownerName=xyz'+str(i)+'&seatId=s'+str(i%numSeats + 1)+''
  fd = open("out/test"+str(i)+".txt", "w+")
  subprocess.Popen(["curl", "-s", "--data", data, serverPath+apiPath+"/reserve"], stdout=fd)
