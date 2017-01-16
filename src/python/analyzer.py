from collections import defaultdict
import sys

#number of sub-sequences to display.
top_n = 20

"""
input : filename for score file(output of the java program)
output : dict from string(sub-sequence) to Z-score
"""
def std_dict(filename):

    dic = defaultdict(float)
    with open(filename,'r') as f:
        for line in f:
            line = line.split()
            string = line[0]
            score = float(line[-1])
            dic[string] = score

    return dic

def substract_dicts(d1,d2):
    d3 = defaultdict(float)
    for key in d1:
        d3[key] = d1[key] - d2[key]

    #also add keys that are in d2 but not in d1.
    for key in d2:
        if not d1.__contains__(key):
            d3[key] = - d2[key]

    return d3


f1 = sys.argv[1]
f2 = sys.argv[2]

d1 = std_dict(f1)
d2 = std_dict(f2)
d3 = substract_dicts(d1,d2)
d3 =  sorted(d3.iteritems(), key=lambda (k,v): (v,k))


print 'Top '+str(top_n) +' who are stronger in '+f2 +':'
for key,val in d3[0:top_n]:
    print key +'    ' + str(val)
print '\n\n'

print 'Top '+str(top_n) +' who are stronger in '+f1 +':'
top_end = d3[-top_n:]
top_end.reverse()
for key,val in top_end:
    print key +'    ' + str(val)
