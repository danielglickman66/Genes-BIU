
import math
import sys
from presteps import *
import len_dist





def prob(str , counter ,  eps=0.01 ): return max(st.norm._cdf(counter(str)), eps)
def prob_weak(str ,counter, eps=0.01): return max(st.norm._cdf(counter(str)/2), eps)

from scipy.stats import poisson
def pos(str , mu=4.8):
    return poisson.pmf(len(str)  , mu)


def my_z(str , counter=lambda x:d[x]): return (float(counter(str)) - means[len(str)])/stds[len(str)]
def my_z_prob(str , counter=lambda x:d[x]): return st.norm._cdf((float(counter(str)) - means[len(str)])/stds[len(str)])

def sum_as_prob(str , counter , exp=True):
    if exp : return 2** counter(str)
    return counter(str)

def q_exp(prefix , char , counter):
    total = 0.
    for c in string.lowercase:
        total += math.e ** counter(prefix+c)

    return max(1. - (math.e ** counter(prefix+char)) / total , sys.float_info.min)

def q_cond(prefix , char , counter):
    return

def q_ignore(prefix , char , counter=0): return 1.


def q(prefix , c , counter , alpha =1.):
    if prefix == '': return 1./4

    return float(counter(prefix + c)) / (alpha+ counter(prefix))



alpha = 1.

def linear_prob(str , counter): return (float(counter(str)) /total_count[len(str)]) ** alpha

len_prob = {i[0]:i[2]/i[1] for i in ratios}
total = sum(len_prob.values())
len_prob = {key:val/total for (key,val) in len_prob.items()}

def q_prop(str , counter =0 ): return len_prob[len(str)]

def linear_prob_wlen_prior(str , counter): return linear_prob(str , counter) * q_prop(str)


class Counter :
    def __init__(self , dict):
        self. d = dict
    def __call__(self, *args, **kwargs):
        return self.d[args[0]]

    def __getitem__(self, item):
        return self.d[item]





class Probability:
    def __init__(self , counter , function ):
        self.counter = counter
        self.f = function

    def __call__(self, *args, **kwargs):
        return self.f(*args , counter=self.counter )


class Q_Prob(Probability):
    def __init__(self, counter , function , alpha =1.):
        Probability.__init__(self , counter , function)
        self.alpha = alpha

    def __call__(self, *args, **kwargs):
        return self.f(*args , counter=self.counter , alpha = self.alpha )



class Neg_Prob:
    def __init__(self , prob): self.f = prob

    def __call__(self, *args, **kwargs): return 1 - self.f(*args , **kwargs)


class Local_Qp():
    get qp and functions , do last step of segment_probability
class Segment_Probability:
    #function can be a probability class
    def __init__(self,qp , function):
        self.qp = qp
        self.f = function
        self.eps = 0.01
        self.new_word_prob = 1.

    def __call__(self, s):
        pi = 1.


        #stop before last char
        for i in range(len(s) - 1):
            prev = s[0:i]
            curr = s[0:i+1]

            p = self.f(curr) #substring up to i


            #q1 = float(self.counter(s[0:i+2])) / self.counter(s[0:i+1])

            q_next = self.qp(curr , s[i+1])

            q_prev = self.qp(prev , s[i])

            p_word_at_i = ( p * q_prev * (1 - q_next))

            pi = pi * ( 1 - p_word_at_i)

        p = self.f(s)
        q_prev = self.qp(s[:-1] , s[-1])
        pi = pi * p * q_prev

        return pi * self.new_word_prob



d_count = Counter(d)
z_count = Counter(d_z)



qp = Q_Prob(d_count , q)
p1 = Probability(z_count , prob)

q1 = Probability(d_count , q_exp)

p_my_z = Probability(d_count , my_z_prob)
p_linear = Probability(d_count , linear_prob)

segs = [p1 , q1 , p_my_z , p_linear]


l = len_dist.ratios_to_list(ratios)
l = len_dist.tups2counts(l)

rats = [len_dist.dict_mean_std(_d) for _d in d.hash.values()]
means = {(i+1):rats[i][0] for i in range(len(rats))}
stds = {(i+1):rats[i][1] for i in range(len(rats))}


