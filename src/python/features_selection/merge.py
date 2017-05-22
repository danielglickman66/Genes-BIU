
def merge(f1 , f2):
    f1 , f2 = file_decorator(f1) , file_decorator(f2)
    a , b = next(f1 , False) , next(f2 , False)

    l = []
    while True:
        if a == False and b == False :
            break

        if a == False:
            l.append(b)
            l.extend(f2)
            break
        if b == False:
            l.append(a)
            l.extend(f1)
            break


        if a == b:
            l.append( a+b) #cange
            a = next(f1 , False)
            b = next(f2, False)

        elif a < b:
            l.append(a)
            a = next(f1 , False)
        else :
            l.append(b)
            b = next(f2 , False)
    return l



#return true strings
def file_decorator(file):
    with open(file) as f:
        for line in f:
            line = line.replace('\x00', '').replace(',', '').strip().split()
            if line:
                yield parser(line)


def parser(line):
    ( score , key) = (float(line[1]) , line[2])
    return (score , key)



import heapq
class Heap:
    def __init__(self , max = True , k = 10**6):
        self.k = k
        if max:
            self.factor = 1.
        else: self.factor = -1.
        self.heap = []

    def append(self , (score , key)):
        self.heap.append((score * self.factor , key))
        if len(self.heap) > self.k:
            heapq.heapify(self.heap)
            self.append = self.__append__

    def __append__(self , (score , key)):
        heapq.heappushpop(self.heap , (score * self.factor , key))

    def extend(self , list):
        for item in list:
            self.append(item)

class MinHeap(Heap):
    def __init__(self , k = 10**6):
        Heap.__init__(self , False , k)

class MaxHeap(Heap):
    def __init__(self , k = 10**6):
        Heap.__init__(self , True , k)


class MinMaxFeatures:
    def __init__(self , transformer):
        self.minh = MinHeap()
        self.maxh = MaxHeap()
        self.transformer = transformer

    def append(self , a=None , b = None):
        t = (score , key) = self.transformer(a , b)
        self.maxh.append(t)
        self.minh.append(t)

    def extend(self , list):
        self.append()