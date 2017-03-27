import random
import string
import subprocess

import rand_noise

line_len = 600

"""
inserts random noise between words
precent is the amount of total noise words in the text.
"""
def insert_random_noise(filename , amount , precent):
    noise = rand_noise.noise_string(amount, precent, 8, 8)
    command = 'python data_generation.py ' + filename +' ' + noise
    
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
    process.wait()

def format(filename):
    command = 'python data_generation.py format ' + 'noisy_'+filename +' noisy_'+filename + ' '+str(line_len)  
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
    process.wait()



#inserts noise similar to the way done in the paper from 2007
def format_file_2yo(orignal , new ):
    text = open(orignal).read()
    text = text.lower()
    for char in string.punctuation :
         text = text.replace(char, '')
    text = text.split()
    with open(new , 'wb') as f:
        s = ''
        for i in range(0,len(text) ):
            if len(text[i]) > 8: continue
            fill = 8 - len(text[i])
            noise = rand_noise.rand_string(fill, fill)
            rand_index = random.randint(0,fill)
            noise_word = noise[0:rand_index] + text[i] + noise[rand_index:]
            s += noise_word +'\n'
        f.write(s)