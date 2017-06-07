

import matplotlib.pyplot as plt
import math


def tups2counts(tups_list):
    return [ tup[-1] for tup in tups_list]

def counts_to_probs(counts_list):
    n = sum(counts_list)
    for i in range(len(counts_list)):
        counts_list[i] = float(counts_list[i]) / n
    return counts_list


#gets list of tuples (len , mean , std)
def ratios_to_list(tuples):
    return [ (t[0] , float(t[2]) / t[1]) for t in tuples]



def text_to_len_ratios(txt):
    from collections import defaultdict
    d = defaultdict(int)
    words = txt.split()
    for word in words:
        d[len(word)] +=1
    return d


def dict_mean_std(d):
    mean = 0.
    n = sum(d.values())
    for item in d.values():
        mean += float(item * item) /n



    var = 0.
    for item in d.values():
        var += ((float(item) - mean)**2) * (float(item) / n)

    return (mean , math.sqrt(var))

