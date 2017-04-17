
class Pre_Processor:
    def __init__(self):
        self.funcs = []

    def batch_process(self , list_of_texts):
        result = []
        for txt in list_of_texts:
            result += self.process(txt)
        return result


    def process(self,txt):
        for func in self.funcs:
            txt = func(txt)
        return txt

    def add_func(self , func): self.funcs.append(func)