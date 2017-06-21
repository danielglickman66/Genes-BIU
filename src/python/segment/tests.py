from probas import *
import viterbi
import evaluator


d_count = Counter(d)
z_count = Counter(d_z)


qp = Q_Prob(d_count , q)
neg = Neg_Prob(qp)

p1 = Probability(z_count , prob)

q1 = Probability(d_count , q_exp)

p_my_z = Probability(d_count , my_z_prob)
p_linear = Probability(d_count , linear_prob)

seg_prob = Segment_Probability(qp , p1)

def score(txt , len , p , q=None):
    if q:
        seg_txt = viterbi.segment(txt , len , p  , q)
    else :
        seg_txt = viterbi.segment(txt, len, p)

    (precision , recall , f ) = evaluator.score(seg_txt , gold_sentence)
    return (precision , recall , f )

#def qp.alpha len seg_prob.new_word_prob
def run_test(txt):
    lens = [7, 10 , 15 ]
    alphas = [1 , 20]
    segs = [ p_my_z,p1, p_linear]
    qs = [neg , None]
    new_word_probs = [0.25 , 0.5 , 1. , 4. , 10.]

    for func in segs:
        for q in qs:
            for alpha in alphas:
                for word_prob in new_word_probs:
                    for l in lens:
                        seg_prob.f = func

                        qp.alpha = alpha
                        seg_prob.new_word_prob = word_prob
                        (precision, recall, f) = score(txt , l , seg_prob , q)

                        print precision , recall , f
                        print 'prob function ' , func.f
                        print 'end negaition ' , q
                        print 'q alpha ' , alpha
                        print 'length ' , l
                        print 'new word prob ' , word_prob
                        print '______________________________________\n'


