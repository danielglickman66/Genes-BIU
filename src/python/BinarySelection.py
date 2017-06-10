import extractors
import features_selection
import sys

#should use featuers manager here

def main(file1 , file2 , num_features , function = features_selection.difference):
    extractor = extractors.Java_Extractor(max_len = 20)
    selector = features_selection.Merge_Feature_Selector(function)

    #it1 is an iterable "vector" that returns (key , z-score)
    it1 = extractor.extract(file1)
    it2 = extractor.extract(file2)

    feats = selector.select(it1,it2 , num_features=num_features)
    return feats

if __name__ == '__main__':
    feats = main(sys.argv[1] , sys.argv[2] , int(sys.argv[3]))
    print feats[0]
    print '\n\n'
    print feats[1]