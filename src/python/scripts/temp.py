from extractors import Java_Extractor
from features_selection import features_selector
import feature_manager

max_len = 7
n_feats = 20
file1 , file2 = 'files/input1' , 'files/input2'

extractor = Java_Extractor(7)
selector = features_selector.Merge_Feature_Selector(features_selector.difference)
fm = feature_manager.Feature_Manager(extractor , selector)

fm.init_features(file1 , file2 , n_feats)


