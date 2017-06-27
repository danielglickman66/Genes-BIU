""" remove non asci chars from file , 
delete old file and makes a new file with the same name"""

import string
def clean(filename):
    special = '\n'
    numbers = ''.join([str(n) for n in range(0,10)])
    legit = string.letters + numbers + special
    f = open(filename ).read()
    txt = [char for char in f if char in legit]
    txt = ''.join(txt)
    
    #delete old file

    open(filename ,'wb').write(txt)
    
