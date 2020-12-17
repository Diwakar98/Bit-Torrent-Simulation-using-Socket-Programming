import numpy as np 
import matplotlib.pyplot as plt 

file = open("analysis.txt", "r")
domains = []
time = []
lines = file.readlines()
fig = plt.figure(figsize = (10, 5)) 
for x in lines:
	x = x[0:len(x)-1]
	lst = x.split(":")
	domains.append(lst[0])
	time.append(float(lst[1]))
	if(lst[0].split(" ")[0]=='vayu.iitd.ac.in'):
		plt.bar(lst[0].split(" ")[1],float(lst[1]),color = 'blue', width = 0.7)
	else:
		plt.bar(lst[0].split(" ")[1],float(lst[1]),color = 'red', width = 0.7)
plt.savefig("graph.png",dpi=100)
plt.show()

 
  
