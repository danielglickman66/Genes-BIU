from sklearn.feature_extraction import DictVectorizer

class Feature_Manager():
    def __init__(self , extractor , selector ,num_features = None):
        self.extractor = extractor
        self.selector = selector
        self.__fit__ = False
        self.num_features = num_features

    #build the features base for the first time.
    def init_features(self , source_A , source_B , num_features):
        self.features = self.selector.select( self.extractor.extract(source_A) , self.extractor.extract(source_B) , num_features)
        self.vectorizer = DictVectorizer()
        self.vectorizer.fit(self.features)
        self.__fit__ = True


    def fit(self , X,y):
        if not self.num_features:
            raise RuntimeError
        class_a = y[0]
        A,B = [] , []
        for i in range(len(X)):
            if y[i] == class_a:
                A.append(X[i])
            else: B.append(X[i])

        self.init_features(A,B ,self.num_features)
        return self

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
    """ the function expects lists of lists , where each inner list is a single input.
    for example to transform a database containing DNA sequences of 10 people , with 50 sequences per person ,
    the function will receive a list of size 10X50 and will return data of length 10
    P.S , the inner items are lists beacuse python extractors expects a list."""
    def transform(self, X):
        if not self.__fit__: raise RuntimeError
        extracted_vectors = []
        if not isinstance(X, list): #need for scitlearn
            X = [X]

        for x in X:
            extracted_vectors.append(self.extractor.extract([x]))

        return self.vectorizer.transform(extracted_vectors)


