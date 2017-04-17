from Z_extractor import Z_Extractor
from features_selection import difference_selector
from sklearn.feature_extraction import DictVectorizer

class Feature_Manager():
    def __init__(self):
        self.extractor = Z_Extractor(15)
        self.selector = difference_selector.Difference_Selector()

    #build the features base for the first time.
    def init_features(self , source_A , source_B , num_features):
        self.features = self.selector.select( self.extractor.extract(source_A) , self.extractor.extract(source_B) , num_features)
        self.vectorizer = DictVectorizer()
        self.vectorizer.fit(self.features)


    """
    gets a list of strings.
    """
    def extract_features(self , source):
        feats = self.extractor.extract(source)
        return self.vectorizer.transform(feats.hash.values())

    """ gets a list of lists of strings"""
    def transform(self, X):
        def collection2dict(hash_collection):
            z = {}
            for _dict in hash_collection.hash.values():
                z.update(_dict)
            return  z

        d = []
        for x in X:
            a = self.extractor.extract(x)
            a= collection2dict(a)
            d.append(a)
        return self.vectorizer.transform(d)