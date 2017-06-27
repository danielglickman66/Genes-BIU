import string
import pre_processor

def remove_punct(txt):
    # remove punctuation
    for char in string.punctuation + '\t' :
        txt = txt.replace(char, '')
    return txt



def remove_spaces(txt):
    char = ' '
    return txt.replace(char, '')

#try and restore paul cohen's segmentation.
#cut text to segments of length n_cols chars.
#only english chars count in n_cols , but spaces do not ,
# so string length of returned asnwer will be > n_col.( and equal n_col + n_spaces)
#returns 1 string.
def cut_by_length_ignore_spaces(txt , n_col):
    chars_seen = 0
    for i  in range(len(txt)):
        if txt[i] in string.letters+string.digits:
            chars_seen += 1
        if chars_seen == n_col:
            return txt[0:i]


def cut_by_length(txt, n_col=600):
    txt = txt.replace('\n', '')
    txt = txt.replace('\r', '')

    l = []
    for i in range(0, len(txt), n_col):
        l.append( txt[i:i + n_col])
    return l


class Text_PreProcessor(pre_processor.Pre_Processor):
    def __init__(self , seq_len = 100000000 ):
        pre_processor.Pre_Processor.__init__(self)
        self.seq_len = seq_len
        self.funcs += [remove_punct , remove_spaces ,string.lower, lambda txt : cut_by_length(txt , self.seq_len)]
