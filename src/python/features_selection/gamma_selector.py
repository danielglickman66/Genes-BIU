from collections import defaultdict
from features_selector import Features_Selector

# number of sub-sequences to display.
default_thres = 10 ** 6

class Gamma_Selector(Features_Selector):
    def __init__(self):
        Features_Selector.__init__(self)

    def substract_dicts(self , d1, d2):
        d3 = defaultdict(float)
        for key in d1:
            d3[key] = d1[key] - d2[key] / (d1[key] + d2[key])

        # also add keys that are in d2 but not in d1.
        for key in d2:
            if not d1.__contains__(key):
                d3[key] = - d2[key]

        return d3


    def select(self ,d1 , d2, num_features):
        d3 = self.substract_dicts(d1, d2)
        d3 = sorted(d3.iteritems(), key=lambda (k, v): (v, k))
        return [ {key:val for key,val in d3[0:num_features/2] }   , {key:val for key,val in d3[-num_features/2:] }  ]




