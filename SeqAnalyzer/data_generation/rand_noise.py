import string
import random
import sys



def id_generator(size=8, chars=string.ascii_lowercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))

def rand_string(lo = 3 , hi = 10):
    size = random.randint(lo,hi)
    return id_generator(size)

def noise_string( amount , total_noise_precnt,lo = 3 , hi=10):
    noise_per_each = total_noise_precnt/amount
    s = ''
    for i in range(amount):
        s += str(noise_per_each) +' ' + rand_string(lo,hi)+' '
    return s

if len(sys.argv) > 1:
    amount = int(sys.argv[1])
    total_noise_precnt = float(sys.argv[2])
    print noise_string(amount , total_noise_precnt)





       


