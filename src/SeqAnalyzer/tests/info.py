from extractors.Z_extractor import Count_Extractor

count_ex = Count_Extractor(11)
filename = 'files/input1/format_1984.txt'
s = open(filename).read()
s = s.split()

d= count_ex.extract(s)

def per_line(line , length):
    return len(line) - length + 1

#total_strings(1) == len(txt
def total_strings(l): return sum([per_line(i,l) for i in s])

def square_sum(l): return sum([i*i for i in d.hash[l].values()])

def different_strings(l): return len(d.hash[l])


func = [total_strings , different_strings , square_sum]


for l in d.hash:
    print l , '     ' , total_strings(l) , '     ' , different_strings(l) ,'        ' , square_sum(l)