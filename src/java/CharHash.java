package model;

public class CharHash {
	
	private TrieNode[] hash = new TrieNode[41];
	
	private int charToIndex(char c){
		if( c >= '0' && c<= '9')
			return c - '0';
		
		return c - 'a' + 10;
		
	}
	
	public boolean containsKey(char c){
		int index = charToIndex(c);
		return hash[index] != null;
	}
	
	public TrieNode get(char c){
		int index = charToIndex(c);
		return hash[index];	
	}
	
	public void put(char c, TrieNode oTrie){
		int index = charToIndex(c);
		hash[index] = oTrie;
		
	}

}
