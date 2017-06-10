from suffixhash import *
from utility.utility import file2dict , dict2file
from utility.hash_collection import HashCollection
from extractors import Extractor


class Count_Extractor(Extractor):

    def extract(self , sequnce_list , max_len = None ,appearance_threshold = 0 , probability = 1.0):
        if max_len == None:
            max_len = self.max_len

        d = count_all_by_len(sequnce_list, max_len , probability)
        return HashCollection(d)









