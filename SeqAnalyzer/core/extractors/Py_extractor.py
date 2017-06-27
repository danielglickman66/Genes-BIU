from extractors.Z_extractor import Z_Extractor , Extractor
import os

class Python_Extractor(Extractor):
    def extract(self, file, max_len=10 ,sort_by_name = True):
        input_list = []
        if os.path.isdir(file):
            for f in os.listdir(file):
                input_list += open(os.path.join(file , f)).read().split()
        else : open(file).read().split()

        extractor = Z_Extractor()
        d =  extractor.extract(input_list , max_len)
        if sort_by_name:
            return d
        return sorted(d.iteritems() ,key = lambda (k,v) : -v)
