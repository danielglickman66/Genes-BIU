""" tech/talk text classification , z-score with difference selector"""

import  features_selection.difference_selector
from control.feature_manager import  Feature_Manager
from extractors.Z_extractor import Z_Extractor
from preprocessors.text_processor import Text_PreProcessor
from sklearn import linear_model
from sklearn.datasets import fetch_20newsgroups as fetch
from sklearn.ensemble import RandomForestClassifier
from sklearn.utils import shuffle

cut_len = 99999
seq_for_stats = 300 #amount to take from each corpus
seq_for_train = 600
seq_for_test  = 400
num_features = 30000 #sub_sequence that the classifer will use
sub_seq_len = 10
talk_tag , tech_tag = 0 ,1
remove = () #('headers' , 'footers' , 'quotes')

processor = Text_PreProcessor(cut_len)
#prepare data
talk_cat = [ 'talk.politics.guns','talk.politics.mideast','talk.politics.misc','talk.religion.misc']
tech_cat = ['comp.graphics','comp.sys.ibm.pc.hardware','comp.sys.mac.hardware','comp.windows.x']
news_talk_train = fetch(subset='all' , categories =talk_cat , remove=remove)
news_tech_train = fetch(subset='all' , categories = tech_cat , remove=remove)
news_talk_train = processor.batch_process(news_talk_train.data)
news_tech_train = processor.batch_process(news_tech_train.data)

X , Y= [] , []
for text in news_tech_train[seq_for_stats:seq_for_stats+seq_for_train]:
    X.append([text])
    Y.append(tech_tag)
for text in news_talk_train[seq_for_stats:seq_for_stats+seq_for_train]:
    X.append([text])
    Y.append(talk_tag)

X ,  Y = shuffle(X, Y, random_state=0)
X_train = X[0:seq_for_train]
y_train = Y[0:seq_for_train]
X_test = X[seq_for_train:seq_for_train+seq_for_test]
y_test = Y[seq_for_train:seq_for_train+seq_for_test]


def test():
    fm = Feature_Manager(extractor, selector)
    fm.init_features(news_talk_train[0:seq_for_stats] , news_tech_train[0:seq_for_stats] , num_features)
    logistic = linear_model.LogisticRegression(solver='sag')

    logistic.fit(fm.transform(X_train) , y_train)
    logistic_score = str( logistic.score(fm.transform(X_test) , y_test))


    clf = RandomForestClassifier(n_estimators=10)
    clf.fit(fm.transform(X_train) , y_train)
    random_forest_score = str( clf.score(fm.transform(X_test) , y_test))


    s = 'classifying tech/talk using Zscore and ' + selector.transformer.__name__  +'extractor: \n#samples for stats   #train samples   #logistic_score     #forest_score     #features       #maxsubseqlen\n'
    s += str(seq_for_stats) +'          ' +str(seq_for_train) +'             ' + logistic_score +'         ' + random_forest_score +'            ' + str(num_features) +'             ' +str(sub_seq_len)
    print s+'\n'
    return fm



extractor = Z_Extractor(sub_seq_len)
selector = features_selection.Tuples_Merge_Feature_Selector(features_selection.combined)
#selector = features_selection.difference_selector.Difference_Selector()
fm = Feature_Manager(extractor , selector)



def search():
    global seq_for_stats , seq_for_train , seq_for_test
    funcs = [features_selection.difference , features_selection.chi2 , features_selection.gini , features_selection.combined]

    for f in funcs:
        selector.transformer = f
        test()

    seq_for_stats, seq_for_train, seq_for_test = 1400 , 1000 , 700
    for f in funcs:
        selector.transformer = f
        test()




