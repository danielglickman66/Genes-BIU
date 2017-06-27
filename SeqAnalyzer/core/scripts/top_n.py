from util.utility import *

"""
extract the top lines in a file, according to the attribute at the given column.
the extracted lines are saved to a new file.

input : filename - file to extract from.
        colum    - int, colum number. starts at 1.
        reverse_order - if True get the buttom n.
        default_thres - used if params do not specify the threshold.

output : the name of the new file with the top lines.
"""
def top_n_2file(filename , n_lines ,column ,  reverse_order = False):
    command = 'sort -k'+str(column) + 'g ' + filename + ' > tempfile' 
    launch_shell_waiting(command)

    #get top n
    if reverse_order:
        command = 'tail -'+str(n_lines) + ' tempfile > top'
    else: command = 'head -'+str(n_lines) + ' tempfile > top'

    launch_shell_waiting(command)

    #sort again by name
    new_file = 'top_' + filename[filename.rfind('/')+1:]
    command = 'sort -k 1 top > '+ new_file 
    launch_shell_waiting(command)


    #delete temp files
    command = 'rm tempfile; rm top'
    launch_shell_no_wait(command)

    return new_file


"""
same as top_n_from_file but returns the lines as a python dictionary.
"""
def top_n_2dict(filename , params ,column ,  reverse_order = False,default_thres = 10**6):
    new_file = top_n_2file(filename , params ,column ,  reverse_order,default_thres)
    return file2dict(new_file)



#returns the number of lines to extract according to command line params.
def get_thres(filename,params , deafult_n_lines=10 ** 6):
    # get threshold(n) value
    if flag_in_string(params, '-percent'):
        threshold = int(float(get_flag_value(params, '-percent')) * num_lines(filename))
    else:
        threshold = (get_flag_value(params, '-K', default=deafult_n_lines))
    return threshold



""""
running from command line:
top_n.py top_v1.stats -percent 0.12   <- returns top 12%
top_n.py top_v1.stats -K 77           <- returns top 77
output is saved to file top_filename
"""
if __name__ == "__main__":
    n_lines = get_thres(sys.argv[1] , sys.argv )
    top_n_2file(sys.argv[1] , n_lines ,  3  )