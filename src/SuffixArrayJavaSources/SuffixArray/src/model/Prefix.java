package model;

import java.util.ArrayList;

public class Prefix extends Suffix {
	static public long m_u64SplitPrefixCount = 0;
	ArrayList<Suffix> m_aloSuffixes;
	String m_sKey;

	public Prefix( Suffix _oSuffix ) {
		super( _oSuffix.m_iLine, _oSuffix.m_iPos, _oSuffix.m_iLen);
		this.m_aloSuffixes = null;

		// Debug statistics
		m_u64SplitPrefixCount++;
	}
	
	public Prefix(int _iLine, int _iPos, int _iLen ) {
		super( _iLine, _iPos, _iLen);
		this.m_aloSuffixes = null;

		// Debug statistics
		m_u64SplitPrefixCount++;
	}
	
	public int compareTo(Prefix _oPrefix) {
		return (this.m_sKey).compareTo(_oPrefix.m_sKey);
	}
}