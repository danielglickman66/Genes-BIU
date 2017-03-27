import sys
import subprocess
from collections import defaultdict


def flag_in_string(string, flag):
    return flag in string

#returns the values of the flag as a string.
def get_flag_value(string , flag , default=False):
    if not flag in string: return default
    return string[string.index(flag) + 1]

def num_lines(fname):
    with open(fname) as f:
        for i, l in enumerate(f):
            pass
    return i + 1


def launch_shell_waiting(command):
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
    process.wait()


def launch_shell_no_wait(command):
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)



"""
extracts from each line of the file the the first filed and the field specifed in column(default is last column).
creates a dictionary where the first filed of each line is key and the other is value.

for example for the file:
a   100     x
b   200     y

file2dict(file , 2) will return {a:x , b:y}

"""
def file2dict(filename , column=-1):

    dic = defaultdict(float)
    with open(filename,'r') as f:
        for line in f:
            line = line.split()
            string = line[0]
            score = float(line[column])
            dic[string] = score

    return dic


#input is dict of dicts
def dict2file(dic,outfile):
    with open(outfile , 'wb') as f:
        for key in dic:
            s = ''
            vector = dic[key]
            for word in vector:
                s += word +'    ' + str(vector[word]) + '\n'
            f.write(s)
