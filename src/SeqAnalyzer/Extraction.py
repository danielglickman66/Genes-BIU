from core import extractors

import argparse


parser = argparse.ArgumentParser(description='Extract using the jar files and output to top scoring ones.')

parser.add_argument('file',
                    help='path of input dir')

parser.add_argument('--max_len', type=int, default=10,
                    help='max length of subsequences to extract')
parser.add_argument('--n_top', type=int, default=100,
                    help='prints only the n_top top scoring')
args = parser.parse_args()



extractor = extractors.get_extractor()
it = extractor.extract(args.file, max_len=args.max_len  , sort_by_name = False)

for i , (feat , score) in enumerate(it) :
    print  str(i+1 ) + '    '  , "{:10.4f}".format(score), '  ' , feat
    if i+1 >= args.n_top:
        break