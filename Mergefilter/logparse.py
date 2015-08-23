import re

logfile = "log4j2.txt"
pattern = re.compile(r"Found [^0] prefixes superstrings")
with open(logfile, "r") as file:
    [print(line) for line in file if re.search(pattern, line)]
