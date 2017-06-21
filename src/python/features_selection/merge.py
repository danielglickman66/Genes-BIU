
key , val = 0 , 1

"""
merges scores from 2 sources(e.g substrings , z-score file)
input:
f1,f2 iterables. next should returns (key , val)
l - data structure to hold and filter the data
"""
def merge(f1 , f2 , l):


    a , b = next(f1 , False) , next(f2 , False)


    while True:
        if a == False and b == False :
            break

        if a == False:
            l.append(a,b)
            b = next(f2, False)
            continue

        if b == False:
            l.append(a,b)
            a = next(f1, False)
            continue


        if a[key] == b[key]:
            l.append( a , b)
            a = next(f1 , False)
            b = next(f2, False)
        elif a[key] < b[key]:
            l.append(a , False)
            a = next(f1 , False)
        else :
            l.append(False , b)
            b = next(f2 , False)


    return l






import heapq
class Heap:
    #k is max heap size
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
    def __init__(self , k ):
        Heap.__init__(self , False , k)

class MaxHeap(Heap):
    def __init__(self , k ):
        Heap.__init__(self , True , k)



class MinMaxFeatures:


    # transformer - functions to rank the features of 2 items. gets 2 score floats returns 1 float
    def __init__(self , transformer , size):
        self.minh = MinHeap(size / 2)
        self.maxh = MaxHeap(size / 2 )
        self.transformer = transformer


    def append(self , a , b ):
        if a and b:
            score = self.transformer(a[val] , b[val])
            name = a[key]  # == b[key]
        elif a:
            score = self.transformer(a = a[val])
            name = a[key]  # == b[key]
        else:
            score = self.transformer(b = b[val])
            name = b[key]  # == b[key]

        self.maxh.append((score , name))
        self.minh.append((score , name))



