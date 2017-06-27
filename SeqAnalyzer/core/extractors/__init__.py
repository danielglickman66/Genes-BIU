

class Extractor:
    def __init__(self , max_len = 20):
        self.max_len = max_len

    def extract(self , input , max_len = None ):
        pass

    def transofrm(self , X , max_len = None): return self.extract(X , max_len)

    def fit(self , X , y=None):return self

""" just some definitions to make imports cleaner """
from Java_extractor import Java_Extractor , Suffix_File_Extractor
from Py_extractor import Python_Extractor
from Z_extractor import Z_Extractor
import Params

exts = { 'java' : Java_Extractor , 'python' : Python_Extractor}

def get_extractor(max_len = 15):
    type = Params.EXTRACTOR_TYPE
    return exts[type]()