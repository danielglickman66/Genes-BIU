from preprocessors.text_processor import Text_PreProcessor
import scipy.stats as st
from Z_extractor import Z_Extractor
import string
import math
from collections import defaultdict
"""
processor = Text_PreProcessor(600)
txt = open('./files/stuff/1984.txt').read()
s = processor.process(txt)

give txt rom eval
extractor = Z_Extractor(15)
d = extractor.extract(s )
"""
def q(prefix , char):
    total = 0.
    for c in string.lowercase:
        total += math.e ** d[prefix+c]

    return 1. - (math.e ** d[prefix+char]) / total

def q_ignore(prefix , char): return 1.

def segment(seq , length , p , q=q_ignore):
    probs , path = viterbi(seq , length , p , q )
    return _segment(seq , path)

"""to do : fix 2 to much"""
def viterbi(seq , length , p , q ):
    probs = [-float('inf') for letter in seq]
    probs[0] = math.log(p(seq[0]))   #best segmentation probability ending at char i.
    best = range(len(seq)) #for backtracking path

    for i in range(0 , len(seq)):
        limit = max(0 , i - length)
        for j in range(i  ,limit - 1 , -1 ):
            sub_seq = seq[j:i+1]

            if j>0: p_best_before = probs[j-1]
            else: p_best_before = math.log(1.) #before start of sequence

            #p_curr = p(sub_seq) * p_best_before   * q( seq[ best[j -1 ] : j ] , seq[j]  )
            #use addition and log to avoid small numbers
            p_curr = p_best_before + math.log(p(sub_seq))  + math.log(q(seq[best[j - 1]: j], seq[j]))
            if p_curr > probs[i]:
                probs[i] = p_curr
                best[i] = j

    return (probs , best)


def _segment(seq , path):
    spaces = []
    i = len(seq) - 1
    while i >= 0:
        spaces.append((path[i],i))
        i = path[i] - 1

    spaces = sorted(spaces)
    new_seq = ''
    for start,end in spaces:
        new_seq += seq[start:end+1] + ' '

    return new_seq


counts = {}
def count_words(all_seqs,len ,prob ):

    z = lambda x,y : 1.
    functions = [z , q]
    for func in functions:
        for sub in s:
            v = segment(sub, 13, prob, func).split()
            for word in v:
                if word in counts:
                    counts[word] += 1
                else:
                    counts[word] = 1

    total = sum(counts.values())
    for key in counts:
        counts[key] = float(counts[key]) / total

    prob_new = lambda x: counts[x] * prob(x)
    return prob_new


def word_per_sentence(seq_list , length , p , q):
    dict_list = []
    for seq in seq_list:
        d = defaultdict(int)
        segmentation = segment(seq , length , p , q)
        segmentation = segmentation.split()
        for word in segmentation:
            d[word] +=1

        dict_list.append(d)
    return dict_list


def feature_in_var_range(matrix, low, high):
    def get_var_indexs(matrix, thres):
        sel = VarianceThreshold(thres)
        sel.fit(matrix)
        return sel.get_support(True)

    high_index = get_var_indexs(matrix, high)
    low_index = get_var_indexs(matrix, low)
    return [a for a in low_index if a not in high_index]