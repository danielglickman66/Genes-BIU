from suffixhash import *

#number of sub-sequences to display.
default_thres = 10 ** 6 


class Z_Extractor():
    def __init__(self):pass

    def extract(self , filename , max_len = 30 ,appearance_threshold = 3):
        s = f2array(filename)
        d = count_all_by_len(s, max_len)
        d = compute_zscore(d, appearance_threshold)
        return d




def f2array(filename):
    f=  open(filename)
    s = []
    for line in f:
        s.append( ''.join(line.split()))
    return s


""" TODO : safely delete this"""
def make_zscore_dict(filename , max_len = 30 , appearance_threshold = 3):
    s = f2array(filename)
    d= count_all_by_len(s , max_len)
    d = compute_zscore(d , appearance_threshold)
    return d





