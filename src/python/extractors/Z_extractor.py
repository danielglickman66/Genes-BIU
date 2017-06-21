from suffixhash import *
from utility.utility import file2dict , dict2file
from extractors import Extractor

#number of sub-sequences to display.
default_thres = 10 ** 7


class Z_Extractor(Extractor):

    def extract(self , sequnce_list , max_len = None ,appearance_threshold = 0 , probability = 1.0):
        if max_len == None:
            max_len = self.max_len

        d = count_all_by_len(sequnce_list, max_len , probability)
        d = compute_zscore(d, appearance_threshold)
        return d









