#!/usr/bin/env python
import core
from core import extractors
import features_selection
import argparse


#should use featuers manager here

def main(file1 , file2 , num_features , max_len , load = False ):

    if load:
        extractor = extractors.Suffix_File_Extractor(max_len)
    else:
        extractor = extractors.get_extractor(max_len=max_len)

    #it1 is an iterable "vector" that returns (key , z-score)
    it1 = extractor.extract(file1 , max_len = max_len)
    it2 = extractor.extract(file2 , max_len = max_len)

    selector = features_selection.get_selector_from_params()
    (featsA , featsB) = selector.select(it1,it2 , num_features=num_features)
    return (featsA , featsB)




promote = """
:parameters
dir of first class files
dir of second class files
number of features to extract(half are taken from each class)
max length of subsequences(features)
optional : add "load" as input if loading directly from  suffixarray output files

"""
parser = argparse.ArgumentParser()
parser.add_argument('file1',
                    help='path of input dir of class A files')
parser.add_argument('file2',
                    help='path of input dir of class B files')
parser.add_argument('--max_len', type=int, default=10,
                    help='max length of subsequences to extract')
parser.add_argument('--n_features', type=int, default=100,
                    help='number of features , half taken from each group.')
parser.add_argument('--load', default=False,action= 'store_true',
                    help='load directly from  suffixarray output files/')
args = parser.parse_args()


if True:#try :
    (featsA, featsB) = main(args.file1 , args.file2 , args.n_features , args.max_len , args.load)
    print 'Feats from ' + args.file1 + ':        Feats from ' + args.file2 + ':'
    #print features of each set in descending order
    for k1,k2 in zip(sorted(featsA.items() , key=lambda(a,b):-b) , sorted(featsB.items() , key=lambda(a,b):-b)):
        space = ' '*(30 - len(str(k1)[1:-1]))
        print str(k1)[1:-1] , space  , str(k2)[1:-1]

#except : print promote
