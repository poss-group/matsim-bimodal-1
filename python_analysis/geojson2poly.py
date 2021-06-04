import sys
import re

in_file = open(sys.argv[1], "r")
out_file = open(re.match("(.*)\..*", sys.argv[1])[1]+".poly", "w")
data = in_file.read()
in_file.close()

out_file.write(re.match("(.*)\..*", sys.argv[1])[1])
out_file.write("\narea1\n")

data = data.replace("\n", "")
data = data.replace("],[","\n\t")
data = data.replace(",","\t")
data = data[1:-1]
data = "\t" + data

out_file.write(data)
out_file.write("\nEND\n")
out_file.write("END\n")
out_file.close()
