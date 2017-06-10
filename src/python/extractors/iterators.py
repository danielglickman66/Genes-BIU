
#wrapper class to allow calls for iteritems on the decorators , as it is required by some sk-learn apis.
class IteritemsWraper:
    def __init__(self , decorator):
        self.deco = decorator
    def __iter__(self):return self
    def iteritems(self):return self.deco.__iter__()
    def next(self):return next(self.deco.__iter__())


#return true strings
def file_decorator(file):
    with open(file) as f:
        for line in f:
            line = line.replace('\x00', '').replace(',', '').strip().split()
            if line:
                yield parser(line)


def dict_decorator(dict):
    return dict.iteritems()

def hash_collection_decorator(hc):
    for key in sorted(hc):
        yield (key , hc[key])

def parser(line):
    ( score , key) = (float(line[1]) , line[2])
    return (key , score)

