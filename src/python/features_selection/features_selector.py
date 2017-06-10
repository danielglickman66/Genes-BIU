import merge


class Features_Selector(object):
    def __init__(self):
        pass



import merge
class Merge_Feature_Selector(Features_Selector):
    def __init__(self , transformer):
        self.transformer = transformer

    def select(self, f1, f2, num_features):
        h = merge.MinMaxFeatures(self.transformer , num_features)
        merge.merge(f1 , f2 , h)
        return [{key: val for val, key in h.maxh.heap}, {key: val for val, key in h.minh.heap}]

#allow using the python dict extractor with merge
class Dict_Merge_Feature_Selector(Merge_Feature_Selector):
    def select(self, f1, f2, num_features):
        return super(Dict_Merge_Feature_Selector,self).select(f1.iteritems() , f2.iteritems() , num_features)

eps = 0.1
smooth = 0.2

#the difference
def difference( a =0. , b = 0.):
    return a -b

import math
def chi2(a=0. , b=0. , directed =   True ):
    dom =  (1/(a+b)) + a + b
    g =  (a-b)**2 / dom
    if directed and b > a:
        return -g
    return g

#directed returns -g . needed to make this selection functions compitable with other function types
def gini(a=0. , b= 0. , directed = True):
    dom = (1 / (a + b)) + a + b
    p = a / dom
    g =  p * (1 - p) #real gini impurity
    g = 1 - g #we want high impiurity
    if directed and b > a:
        return -g
    return g

def combined(a=0. , b=0. , directed = True):
    return gini(a,b,False)*difference(a,b)