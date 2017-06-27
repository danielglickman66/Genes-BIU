
from extractors.Z_extractor import Z_Extractor, Count_Extractor
from preprocessors.text_processor import *

filename = './files/stuff/1984.txt'
#filename = 'br87.txt'

len_text = 50 *1000
subseq_len = 25


txt = open(filename).read()
txt = txt.replace('\n\n' , ' ').replace('\n','') #1984.txt specificly needs this.
txt = remove_punct(txt)

gold_sentence = cut_by_length_ignore_spaces(txt , len_text)




processor = Text_PreProcessor(len_text)

#processor.funcs.remove(string.lower)

s = processor.process(gold_sentence)

"""
gold_sentence = open(filename).read().splitlines()
s = [sent.replace(' ','') for sent in gold_sentence]
"""


z_extractor = Z_Extractor(subseq_len)
extractor = Count_Extractor(subseq_len)

d = extractor.extract(s )
d_z = z_extractor.extract(s)


total_count = { i:sum(d.hash[i].values()) for i in d.hash}




ratios = [(1, 8576.3888888888887, 10069.846854947633),
 (2, 491.60287081339715, 960.5860652226288),
 (3, 50.264619405423062, 141.08401978874943),
 (4, 9.5833853256800605, 27.893749121432705),
 (5, 3.6927022503702456, 9.6496270206010184),
 (6, 2.2085449246927116, 4.5870452864746643),
 (7, 1.6397540851689323, 2.7030610996480138),
 (8, 1.3728696888440981, 1.6570200473167001),
 (9, 1.229645835519783, 1.0938103927539347)]


