import  features_selection
from control.feature_manager import  Feature_Manager
from extractors.Z_extractor import Z_Extractor
from preprocessors.text_processor import Text_PreProcessor
from sklearn import linear_model
from sklearn.datasets import fetch_20newsgroups as fetch
from sklearn.utils import shuffle
from sklearn.pipeline import Pipeline


num_features = 2000 #sub_sequence that the classifer will use
max_sub_seq_len = 7
talk_tag , tech_tag = 0 ,1

#prepare data
talk_cat = [ 'talk.politics.guns','talk.politics.mideast','talk.politics.misc','talk.religion.misc']
tech_cat = ['comp.graphics','comp.sys.ibm.pc.hardware','comp.sys.mac.hardware','comp.windows.x']
news_talk_train = fetch(subset='train' , categories =talk_cat).data
news_tech_train = fetch(subset='train' , categories = tech_cat).data
news_talk_test = fetch(subset='test' , categories =talk_cat).data
news_tech_test = fetch(subset='test' , categories = tech_cat).data


def join_tag_and_shuffle(talk_set , tech_set):
    X = talk_set + tech_set
    Y = [talk_tag] * len(talk_set) + [tech_tag] * len(tech_set)
    X,  Y = shuffle(X, Y, random_state=0)
    return (X,Y)


X_train , Y_train = join_tag_and_shuffle(news_talk_train , news_tech_train)
X_test , Y_test = join_tag_and_shuffle(news_talk_test , news_tech_test)



extractor = Z_Extractor(max_sub_seq_len)
selector = features_selection.get_selector_from_params()
""" automates extraction , selection and filtering"""
fm = Feature_Manager(extractor , selector , num_features)
""" preprocess and format input for extraction"""
preprocessor = Text_PreProcessor()
""" standard sk-learn classifier """
logistic = linear_model.LogisticRegression(solver='sag')

pipe = Pipeline([('processor' , preprocessor) , ('vectorizer',fm),('classifier' , logistic)])
pipe.fit(X_train , Y_train)
print pipe.score(X_test , Y_test)