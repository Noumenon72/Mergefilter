import time

filename = "32000randomprefixes.txt"
prefixes = list()
start = time.time()
with open(filename) as file:
    prefixes = [x.strip('\n') for x in file]
elapsed = time.time() - start
print(elapsed)
