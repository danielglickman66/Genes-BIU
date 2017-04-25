package beans;

public class Beans {
	public enum E_SortCode {
		eSTRING, eCOUNT, eSCORE;
	}
	
	// for printing options
	public static class C_Entry {
		public String m_sStr;
		public int m_iLen, m_iCounter, m_iTotOfLen;
		public double m_dAvg, m_dDev, m_dScore;

		public C_Entry(String _sStr, int _iLen, int _iCounter, double _dAvg,
				double _dDev, double _dScore, int _iTotOfLen) {
			this.m_sStr = _sStr;
			this.m_iLen = _iLen;
			this.m_iCounter = _iCounter;
			this.m_dAvg = _dAvg;
			this.m_dDev = _dDev;
			this.m_dScore = _dScore;
			this.m_iTotOfLen = _iTotOfLen;
		}
	}
	
	public static class FileRef {
		public String m_sLine;
		public long m_u64Pos;
		
		public FileRef(String _sLine, long _u64Pos){
			this.m_sLine = _sLine;
			this.m_u64Pos = _u64Pos;
		}
	}
	
	public static class Params {
		public String m_sProgName, m_sProgVers, m_sInputFolder, m_sOutputFolder, m_sWorkFolder;
		public int m_iTop, m_iMaxLen, m_iSplit;
		public E_SortCode m_eSortCode;
		public boolean m_bDebug;
		
		public Params( String _sProgName, String _sProgVers,
				String _sInputFolder, String _sOutputFolder, String _sWorkFolder,
				int _iTop, int _iMaxLen, int _iSplit, E_SortCode _eSortCode, boolean _bDebug ) {
			this.m_sProgName = _sProgName;
			this.m_sProgVers = _sProgVers;
			this.m_sInputFolder = _sInputFolder;
			this.m_sOutputFolder = _sOutputFolder;
			this.m_sWorkFolder = _sWorkFolder;

			this.m_iTop = _iTop;
			this.m_iMaxLen = _iMaxLen;
			this.m_iSplit = _iSplit;
			this.m_eSortCode = _eSortCode;

			this.m_bDebug = _bDebug;
		}
	}
}
