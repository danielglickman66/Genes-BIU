from Z_extractor import Z_Extractor
from features_selection import difference_selector
from sklearn.feature_extraction import DictVectorizer

class Feature_Manager():
    def __init__(self):
        self.extractor = Z_Extractor()
        self.selector = difference_selector.Difference_Selector()

    #build the features base for the first time.
    def init_features(self , source_A , source_B , num_features):
        self.features = self.selector.select( self.extractor.extract(source_A) , self.extractor.extract(source_B) , num_features)
        self.vectorizer = DictVectorizer()
        self.vectorizer.fit(self.features)


    def extract_features(self , source):
        feats = self.extractor(source)
        return self.vectorizer.transform(feats)

