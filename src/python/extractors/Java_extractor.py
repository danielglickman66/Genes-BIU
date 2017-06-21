from extractors import Extractor
import Consts
import utility.utility
from scripts import sort_file
from extractors import iterators

ADD_TXT_ENDING = True
CONVERT = True

#uses the java program to extract data and returns a iterator to the generated file.
class Java_Extractor(Extractor):
    def extract(self, file, max_len=None):
        if max_len == None:
            max_len = self.max_len

        #dir1/dir2/file.txt --> file.txt
        filename = file[file.rfind('/') + 1 :]

        command = 'java -jar ' + Consts.jar_file + file +\
                  Consts.LEN_FLAG + str(max_len) + ' ' \
                  + Consts.OUTPUT_FLAG + filename

        utility.utility.launch_shell_waiting(command)


        outputfile =  Consts.output_dir + filename

        if ADD_TXT_ENDING and not outputfile.endswith('.txt'):
            outputfile = outputfile + '.txt'

        if CONVERT:
            sort_file.file2utf8(outputfile)

        #sort by name
        output = sort_file.sort_file(outputfile)
        #iterator wrap
        iter = iterators.file_decorator(output)
        return iterators.IteritemsWraper(iter)