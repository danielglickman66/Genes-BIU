from suffixhash import *

#number of sub-sequences to display.
default_thres = 10 ** 6 


class Z_Extractor():
    def __init__(self):pass

    def extract(self , sequnce_list , max_len = 30 ,appearance_threshold = 3):
        d = count_all_by_len(sequnce_list, max_len)
        d = compute_zscore(d, appearance_threshold)
        return d






""" TODO : safely delete this"""
def make_zscore_dict(filename , max_len = 30 , appearance_threshold = 3):
    s = f2array(filename)
    d= count_all_by_len(s , max_len)
    d = compute_zscore(d , appearance_threshold)
    return d





