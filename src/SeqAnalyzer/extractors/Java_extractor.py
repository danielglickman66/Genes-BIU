import Consts
import util.utility
from extractors import Extractor
from iterators import iterators
from scripts import sort_file

ADD_TXT_ENDING = True
import os



#uses the java program to extract data and returns a iterator to the generated file.
class Java_Extractor(Extractor):
    def extract(self, file, max_len=None ,sort_by_name = True):
        if max_len == None:
            max_len = self.max_len

        #dir1/dir2/file.txt --> file.txt
        filename = file[file.rfind('/') + 1 :]

        command1 = 'java -jar ' + Consts.builder_jar + file  + Consts.LEN_FLAG + str(max_len)
        util.utility.launch_shell_waiting(command1)

        outf = [Consts.output_dir+f for f in os.listdir('files/Output') if os.path.isdir(Consts.output_dir+f) ]

        outf = max(outf, key=os.path.getmtime)
        command2 = 'java -jar ' + Consts.analyser_jar +outf + \
                  ' -top 200000'  + ' ' + \
                  Consts.OUTPUT_FLAG + filename

        #util.utility.launch_shell_waiting(command)
        util.utility.launch_shell_waiting(command2)

        outputfile =  Consts.output_dir + filename

        if ADD_TXT_ENDING and not outputfile.endswith('.txt'):
            outputfile = outputfile + '.txt'

        if Consts.CONVERT_UTF16_TO_8:
            sort_file.file2utf8(outputfile)

        if sort_by_name:
            outputfile = sort_file.sort_file(outputfile)

        #iterator wrap
        return iterators.output_file_iterator(outputfile)


#extracts from the outputs file of java without running java(assuming it was already called)
class Suffix_File_Extractor(Extractor):
    def extract(self, file, max_len=None):
        output = sort_file.sort_file(file)
        return iterators.output_file_iterator(output)