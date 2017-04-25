package model;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Suffix implements Comparable<Suffix>{
	// The database of lines referenced by Suffix objects 
	static public ArrayList<byte[]> m_aclDb;

	// Reference to Suffix in database (list of byte[])
	int m_iLine;
	int m_iPos, m_iLen;

	// Data accumulation
	int m_iCount;
	
	public Suffix() {
		this.m_iLine = 0;
		this.m_iPos = 0;
		this.m_iLen = 0;

		this.m_iCount = 0;
	}
	
	public Suffix( Prefix _oPrefix ) {
		this.m_iLine = _oPrefix.m_iLine;
		this.m_iPos = _oPrefix.m_iPos;
		this.m_iLen = _oPrefix.m_iLen;
		this.m_iCount = 0;
	}

	public Suffix( Suffix _oSuffix ) {
		this.m_iLine = _oSuffix.m_iLine;
		this.m_iPos = _oSuffix.m_iPos;
		this.m_iLen = _oSuffix.m_iLen;
		this.m_iCount = 0;
	}

	public Suffix(int _iLine, int _iPos, int _iLen ) {
		this.m_iLine = _iLine;
		this.m_iPos = _iPos;
		this.m_iLen = _iLen;
		this.m_iCount = 0;
	}

	public Suffix(int _iLine, int _iPos, int _iLen, int _iCount ) {
		this.m_iLine = _iLine;
		this.m_iPos = _iPos;
		this.m_iLen = _iLen;
		this.m_iCount = _iCount;
	}

	public int compareTo(Suffix _oSuffix) {
		return ByteBuffer.wrap( m_aclDb.get(this.m_iLine), this.m_iPos, this.m_iLen ).
			compareTo( ByteBuffer.wrap( m_aclDb.get(_oSuffix.m_iLine), _oSuffix.m_iPos, _oSuffix.m_iLen ) );
	}
	
	static public void init() {
		m_aclDb = new ArrayList<byte[]>();
	}
}


