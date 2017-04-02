import string
import pre_processor

def remove_punct(txt):
    # remove punctuation
    for char in string.punctuation :
        txt = txt.replace(char, '')
    return txt



def remove_spaces(txt):
    char = ' '
    return txt.replace(char, '')

def cut_by_length(txt, n_col=600):
    txt = txt.replace('\n', '')
    txt = txt.replace('\r', '')

    l = []
    for i in range(0, len(txt), n_col):
        l.append( txt[i:i + n_col])
    return l


class Text_PreProcessor(pre_processor.Pre_Processor):
    def __init__(self , seq_len):
        pre_processor.Pre_Processor.__init__(self)
        self.seq_len = seq_len
        self.funcs += [remove_punct , remove_spaces ,string.lower, lambda txt : cut_by_length(txt , self.seq_len)]
