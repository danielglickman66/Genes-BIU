class HashCollection:
    def __init__(self,hash_by_lens):
        self.hash = hash_by_lens
        self.iter = self.hash.__iter__()
        self.curr_iter = self.hash[self.iter.next()].__iter__()

    def __getitem__(self, key):
        return self.hash[len(key)][key]

    def __iter__(self): return self

    def next(self):
        try:
            val = self.curr_iter.next()
            return val
        except StopIteration:
            try:
                self.curr_iter = self.hash[self.iter.next()].__iter__()
            except StopIteration:
                #end of hashses , reset iterator
                self.iter = self.hash.__iter__()
                raise StopIteration
            else: return self.next()

