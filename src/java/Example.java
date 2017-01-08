
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DataGenerator gen = new DataGenerator();
		//use only 1 noise for now.
		String[] noises = new String[]{"noise" };
		gen.generateFromFile("1984.txt", 0.05, true , noises  );
		
		
		gen.generateRandomFiles("random1.txt", 0.05, 1000);
		
	}

}
