import subprocess
import sys

if __name__ == "__main__":
    from data_generation import main
    f = sys.argv[1]
    main(sys.argv)
    filename = f[f.rfind('/') + 1 :]
    folder = f[:f.rfind('/') + 1 ]
    format_dir = folder + filename+'_formated'
    noisy_dir = folder + filename + '_noisy'
    command = "mkdir -p "+format_dir +" && mv 'format_'* " +format_dir+" && mkdir -p " + noisy_dir+" && mv 'noisy_'* " + noisy_dir 
    
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
    process.wait()
