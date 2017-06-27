import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/***
 * Functions to create new files with noisy data to be used as input for the trie.
 * Mostly a wraper for python functions.
 * @author danielos60
 *
 */



public class DataGenerator {
	//should change this if the file isnt in the java dir.
	String python_file = "./data_generation.py ";
	String CUTSPACE = "-no_space ";
	String RANDOM_SEQUENCE = "-RANDOM-SEQ ";
	
	/**
	 * creates a new file called noisy_filename with is a copy of filename, but with noise from noises
	 * inserted into it at random with probability prob.
	 */
	public void generateFromFile(String filename, double probability,boolean delete_spaces, String[] noises){
		//the probability we send python is the prob to insert noise between charactars not words,
		// so need to normalize probability
		probability = probability /4;
		
		String params = "python " + python_file; 
		params += filename + " ";
		params+= Double.toString(probability) + " ";
		
		if (delete_spaces){
			params += CUTSPACE;
		}
		
		for(String noise : noises)
			params += noise +" ";
		
		
		try {
			
			Process p = Runtime.getRuntime().exec(params);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("coudlnt run python script");
			e.printStackTrace();
		}
	}
	
	
	
	/***
	 * creates 2 files:
	 * file 1: called filename, which contains num_sequences lines.
	 * each line is a random sequence of 300-600 charactars.
	 * 
	 * file 2: called noisy_filename, each line in this file is a sequence from the same line
	 * in file 1, but with random noise inserted to it with probability given.
	 */
	public void generateRandomFiles(String filename,double probability,int num_sequences){
		
		String params = "python " + python_file; 
		params += filename + " ";
		params+= Double.toString(probability) + " ";
		params += RANDOM_SEQUENCE;
		params += num_sequences;

		
		try {
			
			Process p = Runtime.getRuntime().exec(params);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("coudlnt run python script");
			e.printStackTrace();
		}
	}
}
