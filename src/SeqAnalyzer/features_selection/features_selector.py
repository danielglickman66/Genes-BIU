import merge

class Features_Selector(object):
    def __init__(self):
        pass

    def select(self, f1, f2, num_features):
        raise NotImplementedError

class Merge_Feature_Selector(Features_Selector):
    def __init__(self , transformer):
        self.transformer = transformer
    """ f1 and f2 are iterables of (feautre-name , feautre-score) 1"""
    def select(self, f1, f2, num_features):
        h = merge.MinMaxFeatures(self.transformer , num_features)
        merge.merge(f1 , f2 , h)
        return [{key: val for val, key in h.maxh.heap}, {key: val for val, key in h.minh.heap}]

#allow using the python dict extractor with merge
class Tuples_Merge_Feature_Selector(Merge_Feature_Selector):
    def select(self, f1, f2, num_features):
        return super(Tuples_Merge_Feature_Selector, self).select(f1.iteritems(), f2.iteritems(), num_features)



