from collections import defaultdict , Counter
import string
from sklearn.feature_extraction import DictVectorizer
from utility import hash_collection
import random

def count_substrings(string , dic, max_len = 30):
    for i in range(len(string)):
        dic[str2quad(string[i])] += 1
        k = max(0 , i-max_len)
        for j in range(k , i):
            dic [str2quad(string[j:i+1] ) ] += 1

def count_substrings_str(string , dic, max_len = 30 , prob = 1.0):
    for i in range(len(string)):
        dic[string[i]] += 1
        k = max(0 , i-max_len)
        for j in range(k , i):
            dic [string[j:i+1]] += 1

def count_by_len(string , dic ,max_len=30 , prob=1.0):
    for k in range(1,max_len):
        k_dic = dic[k]
        for i in range(k-1, len(string)):
            if random.random() < prob:
                k_dic[ string[i-k+1 : i+1] ] += 1

def count_all_by_len(arr_of_strings , max_len = 30, prob=1.0):
    dic = defaultdict(Counter)
    #handle a single strng
    if type(arr_of_strings) == str:
        count_by_len(arr_of_strings, dic, max_len, prob)
        return dic

    for string in arr_of_strings:
        count_by_len(string , dic , max_len , prob)
    return dic


def compute_zscore(dic_by_len , threshold):
    epsilon = 10 ** - 4
    remove = []
    v = DictVectorizer(sparse=False)
    for length in dic_by_len:
        dic_t = dic_by_len[length]
        dic = {key:dic_t[key] for key in dic_t if dic_t[key] >= threshold}
        if len(dic) < 1:
            continue
        vector = v.fit_transform(dic)
        mean = vector.mean()
        std = vector.std()

        if std < epsilon:
            remove.append(length)
            continue

        for key in dic:
            dic[key] = (dic[key] - mean) /std
        dic_by_len[length] = Counter(dic)

    for l in remove:
        del dic_by_len[l]

    return hash_collection.HashCollection(dic_by_len)


alphabet = list(string.ascii_lowercase) +['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']
base = len(alphabet)
char2index = {alphabet[i]:i for i in range(len(alphabet))}

def str2quad(string):
    as_quad = 0L
    exp = 1
    for i in range(len(string)):
        as_quad += exp * char2index[string[i]]
        exp *= base
    return as_quad
