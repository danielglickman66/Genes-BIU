from preprocessors.text_processor import Text_PreProcessor
from sklearn.datasets import fetch_20newsgroups as fetch
from feature_manager import  Feature_Manager


processor = Text_PreProcessor(600)

news_talks_train = processor.batch_process(news_talk_train.data)

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
