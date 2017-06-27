from extractors.suffixhash import *
from preprocessors.text_processor import Text_PreProcessor

processor = Text_PreProcessor(600)
txt = open('./files/stuff/1984.txt').read()
s = processor.process(txt)
from extractors.Z_extractor import Z_Extractor

zextractor = Z_Extractor()
z = zextractor.extract(s , 11 , 0)

d = count_all_by_len(s, max_len=11 , prob=1.0)
d = hash_collection.HashCollection(d)
sum([len(d.hash[x]) for x in d.hash])
import scipy.stats as st
def prob(p): return st.norm._cdf(p)
txt = txt.split()
def p(s): return prob(d[s.lower()])


joint = [(txt[i] , txt[i]+txt[i+1] , p(txt[i]) , p(txt[i+1]) , p(txt[i]+txt[i+1])) for i in range(len(txt) - 1) if p(txt[i])*p(txt[i+1]) <= p(txt[i]+txt[i+1])]



import matplotlib.pyplot as plt



def g(x):return x

def draw_dist(len , f = g , q = g):

    length_distir = len
    l6 = [(d.hash[length_distir][key], key) for key in d.hash[length_distir]]



    from collections import defaultdict
    z = defaultdict(float)
    for val, key in l6:
        z[val] = z[val] + 1

    bla = sorted(z.items())
    plt.plot([ f(count_in_text) for count_in_text,amount_with_count in bla] , [ q(amount_with_count) for count_in_text,amount_with_count in bla] , label=str(len))
    return z