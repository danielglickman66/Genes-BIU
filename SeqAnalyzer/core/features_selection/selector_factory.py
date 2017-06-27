functions = {}
def register(cls):
    functions[cls.__name__] = cls
    return cls

import features_selector
import Params
import selection_functions

functions.update( { 'diff' :selection_functions.difference , 'chi':selection_functions.chi2 , 'gini':selection_functions.gini
              ,'combined' : selection_functions.combined})

def get_selector_from_params():
    func = functions[Params.SELECTION_TYPE]
    selector = features_selector.Tuples_Merge_Feature_Selector(func)
    return selector

