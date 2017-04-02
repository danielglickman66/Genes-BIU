
class Pre_Processor:
    def __init__(self):
        self.funcs = []

    def process(self,txt):
        for func in self.funcs:
            txt = func(txt)
        return txt

    def add_func(self , func): self.funcs.append(func)