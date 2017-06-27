from util.utility import *
import Consts
import sys

"""sorts results file by string column"""
def sort_file(filename ,change_encoding = False, column=3):
    if change_encoding:
        file2utf8(filename)
    new_file = filename[: filename.rfind('/') + 1] + 'sorted_' + filename[filename.rfind('/') + 1:]
    command = 'sort -k' + str(column) + ' ' + filename + ' > ' + new_file
    launch_shell_waiting(command)
    return new_file

#utf16 to utf8
def file2utf8(filename):
    command = 'iconv -f utf-16 -t utf-8 ' + filename + ' > temp && mv temp ' + filename
    launch_shell_waiting(command)


if __name__ == "__main__":
    outputfile = sys.argv[1]

    if Consts.CONVERT_UTF16_TO_8:
        file2utf8(outputfile)

    # sort by name
    output = sort_file(outputfile , change_encoding=Consts.CONVERT_UTF16_TO_8)
    print output
