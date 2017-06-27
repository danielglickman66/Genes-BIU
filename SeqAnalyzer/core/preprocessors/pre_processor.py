
class Pre_Processor:
    def __init__(self):
        self.funcs = []

    def batch_process(self , list_of_texts):
        result = []
        for txt in list_of_texts:
            result += self.process(txt)
        return result

    def transform(self, list_of_texts): return self.batch_process(list_of_texts)

    def fit(self,X,y=None ): return self

    """ takes the original  input
    and calls each of the processing functions on it by the order in which they were added using add_func.
    returns the new processed input"""
    def process(self,txt):
        for func in self.funcs:
            txt = func(txt)
        return txt

    def add_func(self , func): self.funcs.append(func)