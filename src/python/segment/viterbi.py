from preprocessors.text_processor import Text_PreProcessor
from sklearn.datasets import fetch_20newsgroups as fetch
from feature_manager import  Feature_Manager
from utility import hash_collection
from suffixhash import *
import scipy.stats as st
from Z_extractor import Z_Extractor

processor = Text_PreProcessor(600)
txt = open('./files/stuff/1984.txt').read()
s = processor.process(txt)


extractor = Z_Extractor(10)
d = extractor.extract(s )

def prob(str): return st.norm._cdf(d[str])


def viterbi(seq , length , p , q ):
    probs = [0. for letter in seq]
    probs[0] = p(seq[0])   #best segmentation probability ending at char i.
    best = range(len(seq)) #for backtracking path

    for i in range(0 , len(seq)):
        limit = max(0 , i - length)
        for j in range(i  ,limit - 1 , -1 ):
            sub_seq = seq[j:i+1]

            if j>0: p_best_before = probs[j-1]
            else: p_best_before = 1. #before start of sequence

            p_curr = p(sub_seq) * p_best_before  # * q( seq[ best[j -1 ] : j ]  )
            if p_curr > probs[i]:
                probs[i] = p_curr
                best[i] = j

    return (probs , best)
