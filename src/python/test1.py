from preprocessors.text_processor import Text_PreProcessor
from sklearn.datasets import fetch_20newsgroups as fetch
from feature_manager import  Feature_Manager


processor = Text_PreProcessor()
talk_cat = [ 'talk.politics.guns',
 'talk.politics.mideast',
 'talk.politics.misc',
 'talk.religion.misc']
tech_cat = ['comp.graphics',
 'comp.sys.ibm.pc.hardware',
 'comp.sys.mac.hardware',
 'comp.windows.x'
]
news_talk_train = fetch(subset='train' , categories =talk_cat)
news_tech_train = fetch(subset='train' , categories = tech_tag)

news_talks_train = list of texts(full text)

fm = Feature_Manager()
fm.init_features(news_talk_train , news_tech_train , num_features= 10000)


data = []
talk_tag , tech_tag = 0 ,1
for text in news_tech_train:
    data.append( (fm.extract_features(text) , tech_tag ))
for text in news_talk_train:
    data.append( (fm.extract_features(text) , talk_tag ))

shuffle data
train classifier
