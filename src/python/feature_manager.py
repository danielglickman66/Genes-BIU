from sklearn.feature_extraction import DictVectorizer

class Feature_Manager():
    def __init__(self , extractor , selector):
        self.extractor = extractor
        self.selector = selector

    #build the features base for the first time.
    def init_features(self , source_A , source_B , num_features):
        self.features = self.selector.select( self.extractor.extract(source_A) , self.extractor.extract(source_B) , num_features)
        self.vectorizer = DictVectorizer()
        self.vectorizer.fit(self.features)


    """
    gets a single input for extractor.
    """
    def extract_features(self , source):
        feats = self.extractor.extract(source)
        #return self.vectorizer.transform(feats.hash.values())

        if not isinstance(feats, list): #need for scitlearn
            feats = [feats]
        return self.vectorizer.transform(feats)

    """ gets a list of inputs for extraction"""
    def transform(self, X):
        extracted_vectors = []
        if not isinstance(X, list): #need for scitlearn
            X = [X]

        for x in X:
            extracted_vectors.append(self.extractor.extract(x))

        return self.vectorizer.transform(extracted_vectors)


        """
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
        """