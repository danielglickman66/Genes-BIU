""" classify tech/news with words seperated using only counts"""
from preprocessors.text_processor import Text_PreProcessor
from sklearn.datasets import fetch_20newsgroups as fetch
from feature_manager import  Feature_Manager

seq_for_stats = 300 #amount to take from each corpus
seq_for_train = 600
seq_for_test  = 300
num_features = 30000 #sub_sequence that the classifer will use
num_as_item = 3

processor = Text_PreProcessor(60000)
talk_cat = [ 'talk.politics.guns',
 'talk.politics.mideast',
 'talk.politics.misc',
 'talk.religion.misc']
tech_cat = ['comp.graphics',
 'comp.sys.ibm.pc.hardware',
 'comp.sys.mac.hardware',
 'comp.windows.x'
]


news_talk_train = fetch(subset='all' , categories =talk_cat)
news_tech_train = fetch(subset='all' , categories = tech_cat)

X = []
Y = []
talk_tag , tech_tag = 0 ,1

for text in news_tech_train.data[seq_for_stats:seq_for_stats+seq_for_train]:
    X.append(text)
    Y.append(tech_tag)
for text in news_talk_train.data[seq_for_stats:seq_for_stats+seq_for_train]:
    X.append(text)
    Y.append(talk_tag)


from sklearn.utils import shuffle
X ,  Y = shuffle(X, Y, random_state=0)

X_train = X[0:seq_for_train]
y_train = Y[0:seq_for_train]
X_test = X[seq_for_train:seq_for_train+seq_for_test]
y_test = Y[seq_for_train:seq_for_train+seq_for_test]

from sklearn.feature_extraction.text import CountVectorizer
count_vec = CountVectorizer()
count_vec.fit(X_train)


from sklearn import datasets, neighbors, linear_model
logistic = linear_model.LogisticRegression(solver='sag')

logistic.fit(count_vec.transform(X_train) , y_train)
print logistic.score(count_vec.transform(X_test) , y_test)

from sklearn.ensemble import RandomForestClassifier
clf = RandomForestClassifier(n_estimators=10)
clf.fit(count_vec.transform(X_train) , y_train)
print clf.score(count_vec.transform(X_test) , y_test)