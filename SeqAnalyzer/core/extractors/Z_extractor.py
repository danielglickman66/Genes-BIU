from extractors.hash_collection import HashCollection
from extractors.suffixhash import count_all_by_len
from suffixhash import *
from util.utility import file2dict , dict2file
from extractors import Extractor

#number of sub-sequences to display.
default_thres = 10 ** 7


class Z_Extractor(Extractor):

    def extract(self , sequnce_list , max_len = None ,appearance_threshold = 0 , probability = 1.0):
        if max_len == None:
            max_len = self.max_len

        #if not isinstance(sequnce_list, list): #need for scitlearn
            #sequnce_list = [sequnce_list]

        d = count_all_by_len(sequnce_list, max_len + 1 , probability)
        d = compute_zscore(d, appearance_threshold)
        return d



class Count_Extractor(Extractor):

    def extract(self , sequnce_list , max_len = None ,appearance_threshold = 0 , probability = 1.0):
        if max_len == None:
            max_len = self.max_len

        d = count_all_by_len(sequnce_list, max_len , probability)
        return HashCollection(d)