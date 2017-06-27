import context
import features_selection
from control.feature_manager import  Feature_Manager
from extractors.Z_extractor import Z_Extractor
from preprocessors.text_processor import Text_PreProcessor
from sklearn import linear_model
from sklearn.datasets import fetch_20newsgroups as fetch
from sklearn.utils import shuffle

num_features = 1000 #sub_sequence that the classifer will use
max_sub_seq_len =  7
talk_tag , tech_tag = 0 ,1

#prepare data
talk_cat = [ 'talk.politics.guns','talk.politics.mideast','talk.politics.misc','talk.religion.misc']
tech_cat = ['comp.graphics','comp.sys.ibm.pc.hardware','comp.sys.mac.hardware','comp.windows.x']
news_talk_train = fetch(subset='train' , categories =talk_cat).data
news_tech_train = fetch(subset='train' , categories = tech_cat).data
news_talk_test = fetch(subset='test' , categories =talk_cat).data
news_tech_test = fetch(subset='test' , categories = tech_cat).data



processor = Text_PreProcessor()
extractor = Z_Extractor(max_sub_seq_len)
selector = features_selection.get_selector_from_params()

logistic = linear_model.LogisticRegression(solver='sag')

def join_tag_and_shuffle(talk_set , tech_set):
    X = talk_set + tech_set
    Y = [talk_tag] * len(talk_set) + [tech_tag] * len(tech_set)
    X,  Y = shuffle(X, Y, random_state=0)
    return (X,Y)


#process the 2 training sets
news_talk_train = processor.transform(news_talk_train)
news_tech_train = processor.transform(news_tech_train)
#now let feature manager extract and select useful features
fm = Feature_Manager(extractor , selector)
fm.init_features(news_talk_train , news_tech_train , num_features)

#shuffle the 2 sets to one list
X_train , Y_train = join_tag_and_shuffle(news_talk_train , news_tech_train)
#extract features from the set and leave only those choosen in selection.
X_train_vectors = fm.transform([[x] for x in X_train])
#train the classifier
logistic.fit(X_train_vectors , Y_train)

#now process the test data we have not seen yet
news_talk_test = processor.transform(news_talk_test)
news_tech_test = processor.transform(news_tech_test)
#transform each sentence to a vector of selected features
X_test , Y_test = join_tag_and_shuffle(news_talk_test , news_tech_test)
X_test_vectors = fm.transform([[x] for x in X_test])
#predict class of the sentence's vectors
print logistic.score(X_test_vectors , Y_test)


