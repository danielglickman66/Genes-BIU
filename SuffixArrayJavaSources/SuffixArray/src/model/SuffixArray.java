package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import model.SASplitFile.E_FileType;
import utils.CrcTable;
import utils.Utils;
import beans.Beans;

public class SuffixArray {
	public static final int iC_MAX_SUFFIX_LEN = 1024, iC_MAX_SPLIT = 15;
	static final char cC_OOB_CHAR = '-';
	
	//	iC_STD_TABLE_REF is empirical, optimize for using about 1GB memory
	//		Formula for PREFIX.length of Hash.codes is Hash.depth=2*(iC_STD_TABLE_REF^(1.0/ SQRT( m_iAlphabetSize ) ) )
	//		This gives us (each chunk of 65,000 lines):
	//			for standard "actg"=9 characters (max 65,000 tables of 600 entries. in practice - due to recurrences - about 5,000 of 600 entries)
	//			for ASCII 36 letters=3 (max 50,000 tables of 700 entries) 
	static final int iC_STD_TABLE_REF = 22;
	static final byte[] acC_OOBTemplate = String.format( "%0" + iC_MAX_SUFFIX_LEN + "d", 0).replace( '0', cC_OOB_CHAR ).getBytes();
	static final String sC_TOPSCORES_NAME = "output";

	//
	//	For line parsing & storing
	//
	static final String sC_LegalUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static final String sC_LegalLower = "0123456789abcdefghijklmnopqrstuvwxyz";
	static final byte acC_LegalLower [] = sC_LegalLower.getBytes();

	static byte [] m_caBuffer = new byte[iC_MAX_SUFFIX_LEN];
	
	public class Infix {
		// Per infix
		public int m_iCount;
		public byte [] m_caInfix;
		
		// Per length
		int m_iTotalStringsOfLength, m_iDiffStringsOfLength;
		double m_dOccurAvg, m_dSquaresSum, m_dStdScore, m_dInversedStdDev;

		// Helpers
		double m_dTotalStringsOfLength, m_dNormalizedInversedStdDev; 

		// DEBUG
		int m_iTopSplit;
		
		public Infix(){
			this.m_iCount = 0;
			this.m_caInfix = new byte[0];
			// STD_SCORE = (specific_suffix_count - MEAN) / STD_DEV;
			this.m_dStdScore = 0.0;
			
			// number of all strings in a given length
			this.m_iTotalStringsOfLength = 0;
			this.m_dTotalStringsOfLength = 0.0;
			// number of different strings in given length
			this.m_iDiffStringsOfLength = 0;
			// MEAN(AVG) = total_count_per_length/different_strings_count_per_length
			this.m_dOccurAvg = 0.0;
			// STD_DEV = 1/ sqrt(SUM(specific_suffix_count-MEAN)^2/ (total_count_per_length-1 ) )
			this.m_dSquaresSum = 0.0;
			
			// DEBUG
			this.m_iTopSplit = -1;
		}
		
		public Infix( Infix _oInfix ){
			this.m_iCount = _oInfix.m_iCount;
			this.m_caInfix = new byte[_oInfix.m_caInfix.length];
			System.arraycopy( _oInfix.m_caInfix, 0, this.m_caInfix, 0, _oInfix.m_caInfix.length );
			
			this.m_dStdScore = _oInfix.m_dStdScore;
			
			this.m_iTotalStringsOfLength = _oInfix.m_iTotalStringsOfLength;
			this.m_dTotalStringsOfLength = _oInfix.m_dTotalStringsOfLength; 
			this.m_iDiffStringsOfLength = _oInfix.m_iDiffStringsOfLength;
			this.m_dOccurAvg = _oInfix.m_dOccurAvg;
			this.m_dSquaresSum = _oInfix.m_dSquaresSum;
			this.m_dInversedStdDev = _oInfix.m_dInversedStdDev;
			this.m_dNormalizedInversedStdDev = _oInfix.m_dNormalizedInversedStdDev;

			// DEBUG
			this.m_iTopSplit = _oInfix.m_iTopSplit;
		}
		
		public void reset(){
			this.m_iCount = 0;
			this.m_dStdScore = 0.0;
		}
	}
	
	 Comparator<Infix> pf_compareToCount = new Comparator<Infix>() {
	      public int compare(Infix _oInfix1, Infix _oInfix2) {
	    	  if (_oInfix1.m_iCount < _oInfix2.m_iCount)
					return -1;
				else if (_oInfix1.m_iCount > _oInfix2.m_iCount)
					return 1;
				else return 0;
	      }
	 };
	 
	 Comparator<Infix> pf_compareToScore = new Comparator<Infix>() {
		 public int compare(Infix _oInfix1, Infix _oInfix2) {
	    	  if (_oInfix1.m_dStdScore < _oInfix2.m_dStdScore)
					return -1;
				else if (_oInfix1.m_dStdScore > _oInfix2.m_dStdScore)
					return 1;
				else return 0;
	      }
	 };
	
	//
	//	Variables
	//
	static String m_sAlpha, m_sComment, m_sError;
	static byte [] m_caSuffix;
	static byte [] m_caSuffixPrev;
	static int m_iSuffixPrev, m_iFileLines;
	static long m_u64FileCrc;

	HashMap<String, Prefix> m_hmPrefices = new HashMap<String, Prefix>();
	
	ArrayList<Prefix> m_alPrefices;
	Infix[] oRunningInfix = new Infix[iC_MAX_SUFFIX_LEN];
	ArrayList<Infix> m_olTopCount, m_olTopScore;

	ArrayList<String> m_alSplits;
	int m_iSplitLen;
	
	SASplitFile m_oSAFile;
	SAXmlFile m_oSAXml;
	
	boolean m_bInfixSame;

	//
	//	For line parsing, storing, saving & loading
	//
	long m_i64IllegalCount, m_64TimeSave = 0, m_64TimeLoad = 0, m_u64TimeSt = 0;

	int m_iTopCount, m_iInfixMinLen, m_iInfixMaxLen;
	
	// DEBUG
	int m_iCurrentSplit;

	// Temporary node for search
	Suffix m_oSuffix = new Suffix();
	
	double m_dTopScore;
	Beans.E_SortCode m_eSortCode;
	Timestamp m_oTimestamp;
	boolean m_bDebug, m_bIllegal;
	String m_sProgName, m_sProgVers, m_sInputFolder, m_sOutputFolder, m_sWorkFolder;

	public int m_iMaxLen, m_iFileRefCount, m_iSplit, 
		m_iHashDepth, m_iMaxSuffixLen, m_iDebugCount;
	
	private int m_iTop;
	private boolean m_bTopCountFull, m_bTopScoreFull;

//	==========================================================================================
	public long m_u64TotInfixCount;
	public long m_u64SplitNodes, m_u64SplitSuffixCount, m_u64SplitInfixCount;
	public long m_u64SplitMaxNodes, m_u64SplitMaxSuffixCount, m_u64SplitMaxPrefixCount, m_u64SplitMaxInfixCount;
	
	public SuffixArray(){
		m_oSAFile = null;
		m_oSAXml = null;
		
		m_sAlpha = "";
		m_alPrefices = new ArrayList<Prefix>();
		m_iInfixMinLen = 0;
		m_iInfixMaxLen = 0;
		m_alSplits = new ArrayList<String>();
		m_caSuffix = new byte[iC_MAX_SUFFIX_LEN];
		m_caSuffixPrev = new byte[iC_MAX_SUFFIX_LEN];
		m_iSuffixPrev = 0;

		m_olTopCount = new ArrayList<Infix>();
		m_iTopCount = 0;
		m_olTopScore = new ArrayList<Infix>();
		m_dTopScore = 0.0;

		m_eSortCode = Beans.E_SortCode.eCOUNT;
		
		m_u64FileCrc = 0;
		m_iFileLines = 0;

		m_oTimestamp = null;
		m_bDebug = false;
		m_bIllegal = false;
		
		m_bTopCountFull = false;
		m_bTopScoreFull = false;
		
		m_i64IllegalCount = 0;
		
		m_iTop = 0;
		m_iMaxLen = -1;
		m_iFileRefCount = 0;
		m_iSplit = 0;
		m_iHashDepth = 0;
		m_iMaxSuffixLen = 0;
		m_bInfixSame = false;

		m_sError = "";

		//	DEBUG SECTION
		m_u64TotInfixCount = 0;
		
		m_u64SplitNodes = 0;
		m_u64SplitSuffixCount = 0;
		Prefix.m_u64SplitPrefixCount = 0;
		m_u64SplitInfixCount = 0;
		
		m_u64SplitMaxNodes =0;
		m_u64SplitMaxSuffixCount = 0;
		m_u64SplitMaxPrefixCount = 0;

		m_iDebugCount = 0;
		
		m_iCurrentSplit = -1;
	
	}
	
	//
	//	init
	//
	static public void init(){
		Suffix.init();
	}
	
	//
	//	setParams
	//
	public void setParams(Beans.Params _oParams){
		m_sProgName = _oParams.m_sProgName;
		m_sProgVers = _oParams.m_sProgVers;
		m_sInputFolder = _oParams.m_sInputFolder;
		m_sOutputFolder = _oParams.m_sOutputFolder;
		m_sWorkFolder = _oParams.m_sWorkFolder;

		m_iTop = _oParams.m_iTop;
		m_iMaxLen = _oParams.m_iMaxLen;
		m_iSplit = _oParams.m_iSplit;
		m_eSortCode = _oParams.m_eSortCode;
		m_bDebug = _oParams.m_bDebug;

		Utils.debugLogLn( "Input=" + m_sInputFolder + ", " + "Output=" + m_sOutputFolder );
		Utils.debugLogLn(
			"  Top=" + m_iTop +
			", MaxLen=" + m_iMaxLen +
			", Split=" + m_iSplit +
			", Sort=" + m_eSortCode
			);
	}
	
	//
	//	clearSplit: clear all tables of previous split (leaving only accumulated stats)
	//
	public void clearSplit() {
		ArrayList<Suffix> aloSuffixes;
		Infix oInfix;
		
		m_u64SplitNodes = 0;
		m_u64SplitSuffixCount = 0;
		Prefix.m_u64SplitPrefixCount = 0;
		m_u64SplitInfixCount = 0;
		
		for (int k=0; k < m_iMaxSuffixLen; k++){
			oInfix = oRunningInfix[k];
			System.arraycopy( acC_OOBTemplate, 0, oInfix.m_caInfix, 0, k+1 ); 
		}

		for (int i=0; i < m_alPrefices.size(); i++ ){
			aloSuffixes = m_alPrefices.get(i).m_aloSuffixes;
			aloSuffixes.clear();
		}
		
		m_alPrefices.clear();
		m_hmPrefices.clear();
	}
	
	//
	//	SuffixToBuffer
	//
	void SuffixToBuffer( Suffix _oSuffix ){
		System.arraycopy( Suffix.m_aclDb.get(_oSuffix.m_iLine), _oSuffix.m_iPos, m_caSuffix, 0, _oSuffix.m_iLen );
	}

	//
	//	s_SuffixAsString
	//
	String s_SuffixAsString( Suffix _oSuffix ){
		return new String( Arrays.copyOfRange( Suffix.m_aclDb.get(_oSuffix.m_iLine), _oSuffix.m_iPos, _oSuffix.m_iPos + _oSuffix.m_iLen ) );
	}

	//
	//	prepFile
	//
	public void prepFile() {
		m_u64FileCrc = -1;
		m_iFileLines = 0;
	}
	
	//
	//	b_StoreLine
	//
	public boolean b_StoreLine( String _sLine, long _u64LinesRead, int _iFileNum, long _u64FilePos ){
		int iLineLen = _sLine.length(), iPos, iBuffLen = 0;
		byte caLine [];
		
		//	Compute max suffix length
		if ( iLineLen > m_iMaxSuffixLen)
			m_iMaxSuffixLen = iLineLen;

		//
		//	Compress Unicode to single bytes
		//
		caLine = _sLine.getBytes( StandardCharsets.UTF_8 );

		//	Check line contents validity
		for ( int iCh = 0; iCh < iLineLen; iCh++ ){
			if ( sC_LegalLower.indexOf( _sLine.charAt( iCh ) ) < 0 ) {
				iPos = sC_LegalUpper.indexOf( _sLine.charAt( iCh ) ); 
				if ( iPos < 0 ) {
					if ( !m_bIllegal ) {
						Utils.debugLogAndPrintLn( "*** ERROR: illegal character '" +
							String.format( "%04x", (int) _sLine.charAt( iCh ) ) + "'" +
							" in line=" + _sLine.substring(0, 30 ) +
							" in file=" + _iFileNum +
							" pos=" + _u64FilePos + " at=" + iCh +
							" after line=" + _u64LinesRead, ' ' );
						m_bIllegal = true;
					}
					m_i64IllegalCount++;
				}
				else
					m_caBuffer[iBuffLen] = acC_LegalLower[iPos];
			}
			else
				m_caBuffer[iBuffLen] = caLine[iCh];
			
			m_u64FileCrc = CrcTable.u64Crc32[ (int) ( m_u64FileCrc ^ (long) m_caBuffer[iBuffLen] ) & 0x000000FF ] ^
					( ( m_u64FileCrc >> 8 ) & 0x00FFFFFF );
			iBuffLen++;

			if ( m_sAlpha.indexOf( _sLine.charAt( iCh ) ) < 0 ) {
				m_sAlpha = m_sAlpha + _sLine.charAt( iCh );
			}
		}
		if ( m_i64IllegalCount > 0 ) {
			m_sError = "*** ERROR: Total illegal characters in file(s)=" + m_i64IllegalCount;
			return false;
		}

		Suffix.m_aclDb.add( Arrays.copyOfRange( m_caBuffer, 0, iBuffLen) );
		m_iFileLines++;
		
		return true;
	}
	
	//
	//	u64_FileCrc
	//
	public long u64_FileCrc() {
		return m_u64FileCrc;
	}
	
	//
	//	i_FileStoredLines
	//
	public int i_FileStoredLines() {
		return m_iFileLines;
	}
	
	//
	//	i_TotalStoredLines: Size of database for split computation
	//
	public int i_TotalStoredLines(){
		return Suffix.m_aclDb.size();
	}
	
	//
	//	b_BuildSplitList
	//
	public boolean b_BuildSplitList( int _iSplitLen ) {
		int iChar1, iChar2, iChar3, iAlphaLen = m_sAlpha.length();
		String sSplit;

		m_iSplitLen = _iSplitLen;

		// Any single letter
		if ( m_iSplitLen == 1 ) {
			for ( iChar1 = 0; iChar1 < iAlphaLen; iChar1 ++ ) {
				sSplit = m_sAlpha.substring( iChar1, iChar1 + 1 );
				if ( m_alSplits.contains( sSplit ) )
					continue;
				m_alSplits.add( sSplit );
			}
		}
		// All couples, no duplicates
		else if ( m_iSplitLen == 2 ) {
			for ( iChar1 = 0; iChar1 < iAlphaLen; iChar1 ++ ) {
				for ( iChar2 = 0; iChar2 < iAlphaLen; iChar2 ++ ) {
					sSplit = m_sAlpha.substring( iChar1, iChar1 + 1 ) +
						m_sAlpha.substring( iChar2, iChar2 + 1 );
					if ( m_alSplits.contains( sSplit ) )
						continue;
					m_alSplits.add( sSplit );
				}
			}
		}
		// All triples, no duplicates
		else if ( m_iSplitLen == 3 ) {
			for ( iChar1 = 0; iChar1 < iAlphaLen; iChar1 ++ ) {
				for ( iChar2 = 0; iChar2 < iAlphaLen; iChar2 ++ ) {
					for ( iChar3 = 0; iChar3 < iAlphaLen; iChar3 ++ ) {
						sSplit = m_sAlpha.substring( iChar1, iChar1 + 1 ) +
								m_sAlpha.substring( iChar2, iChar2 + 1 ) +
								m_sAlpha.substring( iChar3, iChar3 + 1 );
						if ( m_alSplits.contains( sSplit ) )
							continue;
						m_alSplits.add( sSplit );
					}
				}
			}
		}
		// Should never reach so much
		else
			return false;
		
		Collections.sort( m_alSplits );

		return true;
	}

	//
	//	getSplitsSize
	//
	public int getSplitsSize() {
		return m_alSplits.size();
	}
	
	//
	//	getAplhabet
	//
	public String getAplhabet() {
		return m_sAlpha;
	}
	
	//
	//	getError
	//
	public String getError() {
		return m_sError;
	}
	
	//
	//	SetInfixLimits: Set min and max limits for Infix check, depending upon Split length (all splits have equal length) 
	//
	void SetInfixLimits( int _iSplit ){
		m_iCurrentSplit = _iSplit;
		if ( _iSplit < 0 ) {
			m_iInfixMinLen = 0;
			m_iInfixMaxLen = Integer.MAX_VALUE;
		}
		else if ( _iSplit == 0 ) {
			m_iInfixMinLen = 0;
			m_iInfixMaxLen = m_alSplits.get( 0 ).length() - 1;
		}
		else {
			m_iInfixMinLen = m_alSplits.get( 0 ).length() - 1;
			m_iInfixMaxLen = Integer.MAX_VALUE;
		}
	}

	//
	//	scanTotalAndDiff (per length) 
	//
	public void scanTotalAndDiff( int _iSplit ){
		ArrayList<Suffix> aloSuffixes;
		int	iAccuLen, iMinLen;
		Suffix oSuffix;
		Infix oInfix;

		Utils.beginProgress( m_alPrefices.size() );

		SetInfixLimits( _iSplit );

		// loop on list of tables
		for (int i=0; i < m_alPrefices.size(); i++ ){
			Utils.showProgress();

			aloSuffixes = m_alPrefices.get(i).m_aloSuffixes;
			// loop on specific table
			for (int j=0; j < aloSuffixes.size(); j++){
				m_bInfixSame = true;

				// get suffix
				oSuffix = aloSuffixes.get(j);
				SuffixToBuffer( oSuffix );

				iMinLen = Math.max( m_iInfixMinLen, 0 );
//-3-				iAccuLen = Math.min( Math.min( m_iMaxLen, oSuffix.m_iLen ), m_iInfixMaxLen );
				iAccuLen = Math.min( oSuffix.m_iLen, m_iInfixMaxLen );
				for ( int k = iMinLen; k < iAccuLen; k++ ) {
							
					oInfix = oRunningInfix[k];

					// 1. add count of suffix to total count of length
					oInfix.m_iTotalStringsOfLength += oSuffix.m_iCount;
					
					//	2. compare running Infix of current length to proper Infix of suffix until !eq, then , add one global different for length and replace running
					if ( !m_bInfixSame || !ByteBuffer.wrap( m_caSuffix, 0, k+1 ).equals( ByteBuffer.wrap( oInfix.m_caInfix, 0, k+1 ) ) ) {
						m_bInfixSame = false;

						// end of infix computation
						if ( oInfix.m_caInfix[0] != '-' ) {
							oInfix.m_iDiffStringsOfLength++;
						}

						// new infix
						System.arraycopy( m_caSuffix, 0, oInfix.m_caInfix, 0, k+1 );
					}
				}
			}
		}
		
		// take into account last infixes
		iMinLen = Math.max( m_iInfixMinLen, 0 );
		iAccuLen = Math.min( m_iMaxLen, m_iMaxSuffixLen );
		for ( int k = iMinLen; k < iAccuLen; k ++ ) {
			oInfix = oRunningInfix[k];
			
			if ( oInfix.m_caInfix[0] != '-' )
				oInfix.m_iDiffStringsOfLength++;
		}

		Utils.endProgress();
	}
	
	//
	//	calcAvgPerLength 
	//
	public void calcAvgPerLength(){
		Infix oInfix;
		int iAccuLen;

		iAccuLen = Math.min( m_iMaxLen, m_iMaxSuffixLen );
		for ( int k = 0; k < iAccuLen; k++ ) {
			oInfix = oRunningInfix[k];
			oInfix.m_dTotalStringsOfLength = (double) oInfix.m_iTotalStringsOfLength;
			if ( oInfix.m_iDiffStringsOfLength != 0)
				oInfix.m_dOccurAvg = oInfix.m_dTotalStringsOfLength / (double) oInfix.m_iDiffStringsOfLength;
			else oInfix.m_dOccurAvg = 0.0;
		}
	}
	
	//
	//	scanStdDev (per length) 
	//
	public void scanStdDev( int _iSplit ){
		ArrayList<Suffix> aloSuffixes;
		int	iAccuLen, iMinLen;
		Suffix oSuffix;
		Infix oInfix;
		double dTemp;
		
		if ( _iSplit < 0 )
			Utils.beginProgress( m_alPrefices.size() );
		else
			Utils.beginProgress( m_alPrefices.get(0).m_aloSuffixes.size() );

		SetInfixLimits( _iSplit );

		// loop on list of tables
		for (int i=0; i < m_alPrefices.size(); i++ ){
			if ( _iSplit < 0 )
				Utils.showProgress();

			aloSuffixes = m_alPrefices.get(i).m_aloSuffixes;

			// loop on specific table
			for (int j=0; j < aloSuffixes.size(); j++){
				if ( _iSplit >= 0 )
					Utils.showProgress();

				m_bInfixSame = true;

				// get suffix
				oSuffix = aloSuffixes.get(j);
				SuffixToBuffer( oSuffix );

				// check all infixes of current suffix (up to its length - or max required)
				iMinLen = Math.max( m_iInfixMinLen, 0 );
//-3-				iAccuLen = Math.min( Math.min( m_iMaxLen, oSuffix.m_iLen ), m_iInfixMaxLen );
				iAccuLen = Math.min( oSuffix.m_iLen, m_iInfixMaxLen );
				for ( int k = iMinLen; k < iAccuLen; k++ ) {
					//	1. Retrieve Infix
					oInfix = oRunningInfix[k];

					//	2. compare running Infix of current length to proper Infix of suffix until !eq, then , add one global different for length and replace running
					if ( !m_bInfixSame || !ByteBuffer.wrap( m_caSuffix, 0, k+1 ).equals( ByteBuffer.wrap( oInfix.m_caInfix, 0, k+1 ) ) ) {
						m_bInfixSame = false;

						if ( oInfix.m_caInfix[0] != '-' ) {
							// SUM(specific_suffix_count-MEAN)^2
							
							dTemp = ( (double) oInfix.m_iCount ) - oInfix.m_dOccurAvg;
							oInfix.m_dSquaresSum += dTemp * dTemp;
						}

						// new infix
						System.arraycopy( m_caSuffix, 0, oInfix.m_caInfix, 0, k+1 ); 
						oInfix.m_iCount = oSuffix.m_iCount;
					}
					else{
						oInfix.m_iCount += oSuffix.m_iCount;
					}
				}
			}
		}
		
		// take into account last infixes
		iMinLen = Math.max( m_iInfixMinLen, 0 );
		iAccuLen = Math.min( m_iMaxLen, m_iMaxSuffixLen );
		for ( int k = iMinLen; k < iAccuLen; k++ ) {
			oInfix = oRunningInfix[k];

			if ( oInfix.m_caInfix[0] != '-' ) {
				// Normalized SUM(specific_suffix_count-MEAN)^2
				dTemp = ( (double) oInfix.m_iCount ) - oInfix.m_dOccurAvg;
				oInfix.m_dSquaresSum += dTemp * dTemp;
			}
		}
		Utils.endProgress();
	}

	//
	//	calcStdDev (per length) 
	//
	public void calcStdDev(){
		Infix oInfix;
		int iAccuLen;
		double dTemp, dDbSizeInv;
		
		dDbSizeInv = 1.0d / (double) Suffix.m_aclDb.size();
		iAccuLen = Math.min( m_iMaxLen, m_iMaxSuffixLen );
		for ( int k = 0; k < iAccuLen; k++ ) {
			oInfix = oRunningInfix[k];
			if ( oInfix.m_dSquaresSum != 0.0 ) {
				dTemp = Math.sqrt( ( oInfix.m_dTotalStringsOfLength - 1.0d ) / oInfix.m_dSquaresSum );
				if ( dTemp != 0.0 ) {
					oInfix.m_dInversedStdDev = 1.0d / dTemp;
					oInfix.m_dNormalizedInversedStdDev = oInfix.m_dInversedStdDev*dDbSizeInv;
				}
				else oInfix.m_dInversedStdDev = 0.0;
			}
			else oInfix.m_dInversedStdDev = 0.0;
		}
	}
	
	//
	//	scanStdScore 
	//
	public void scanStdScore( int _iSplit ){
		ArrayList<Suffix> aloSuffixes;
		int	iAccuLen, iMinLen;
		Suffix oSuffix;
		Infix oInfix;
		
		if ( _iSplit < 0 )
			Utils.beginProgress( m_alPrefices.size() );
		else
			Utils.beginProgress( m_alPrefices.get(0).m_aloSuffixes.size() );

		SetInfixLimits( _iSplit );

		// loop on list of tables
		for (int i=0; i < m_alPrefices.size(); i++){
			if ( _iSplit < 0 )
				Utils.showProgress();

			aloSuffixes = m_alPrefices.get(i).m_aloSuffixes;
			// loop on specific table
			for (int j=0; j<aloSuffixes.size() ; j++){
				if ( _iSplit >= 0 )
					Utils.showProgress();

				m_bInfixSame = true;

				// get suffix
				oSuffix = aloSuffixes.get(j);
				SuffixToBuffer( oSuffix );

				// check all infixes of current suffix (up to its length - or max required)
				iMinLen = Math.max( m_iInfixMinLen, 0 );
//-3-				iAccuLen = Math.min( Math.min( m_iMaxLen, oSuffix.m_iLen ), m_iInfixMaxLen );
				iAccuLen = Math.min( oSuffix.m_iLen, m_iInfixMaxLen );
				for ( int k = iMinLen; k < iAccuLen; k++ ) {
					m_u64SplitInfixCount++;

					//	1. Retrieve Infix
					oInfix = oRunningInfix[k];

					//	2. compare running Infix of current length to proper Infix of suffix until !eq, then , add one global different for length and replace running
					if ( !m_bInfixSame || !ByteBuffer.wrap( m_caSuffix, 0, k+1 ).equals( ByteBuffer.wrap( oInfix.m_caInfix, 0, k+1 ) ) ) {
						m_bInfixSame = false;

						if ( oInfix.m_caInfix[0] != '-' ) {
							oInfix.m_dStdScore = ( ( (double) oInfix.m_iCount ) - oInfix.m_dOccurAvg )*oInfix.m_dNormalizedInversedStdDev;
							checkAndStoreTop(oInfix);
						}

						// new infix
						System.arraycopy( m_caSuffix, 0, oInfix.m_caInfix, 0, k+1 );
						oInfix.m_iCount = oSuffix.m_iCount;
					}
					else{
						oInfix.m_iCount += oSuffix.m_iCount;
					}
				}
			}
		}
		
		// take into account last infixes
		iMinLen = Math.max( m_iInfixMinLen, 0 );
		iAccuLen = Math.min( m_iMaxLen, m_iMaxSuffixLen );
		for ( int k = iMinLen; k < iAccuLen; k++ ) {
			oInfix = oRunningInfix[k];
			if ( oInfix.m_caInfix[0] != '-' ) {
				m_u64SplitInfixCount++;
				oInfix.m_dStdScore = ( (double) oInfix.m_iCount - oInfix.m_dOccurAvg )*oInfix.m_dNormalizedInversedStdDev;
				checkAndStoreTop(oInfix);
			}
		}

		//
		//	Accumulate debug Statistics
		//
		if ( m_u64SplitInfixCount > m_u64SplitMaxInfixCount )
			m_u64SplitMaxInfixCount = m_u64SplitInfixCount; 
		m_u64TotInfixCount += m_u64SplitMaxInfixCount;

		Utils.endProgress();
	}
	
	//
	//	prepareStats 
	//
	public void prepareStats(){
		Infix oInfix;
		
		for ( int k=0; k < m_iMaxSuffixLen; k++ ){
			oInfix = new Infix();
			oInfix.m_caInfix = Arrays.copyOfRange( acC_OOBTemplate, 0, k+1 ); 
			oRunningInfix[k] = oInfix;
		}
	}
	
	//
	//	resetStats 
	//
	public void resetStats(){
		Infix oInfix;
		
		for (int k=0; k < m_iMaxSuffixLen; k++){
			oInfix = oRunningInfix[k];
			oInfix.reset();
			oInfix.m_caInfix = Arrays.copyOfRange( acC_OOBTemplate, 0, k+1 ); 
		}
		
	}
	
	//
	//	u64_SaveTime
	//
	public long u64_SaveTime(){
		return m_64TimeSave;
	}
	
	//
	//	u64_LoadTime
	//
	public long u64_LoadTime(){
		return m_64TimeLoad;
	}
	
	public long u64_SAFileSize() {
		if ( m_oSAFile != null )
			return m_oSAFile.i_SAFileSize();
		else return 0;
	}
	public long i_SAFileCount() {
		if ( m_oSAFile != null )
			return m_oSAFile.i_SAFileCount();
		else return 0;
	}
	
	//
	//	hashToList: Merge HASH into tables, and create an overall sorted list of all tables
	//
	public boolean hashToList( int _iSplit, boolean _bSaveToFile, boolean _bXml ){
		Iterator<Entry<String,Prefix>> oIter = m_hmPrefices.entrySet().iterator();
		ArrayList<Prefix> alShortSuffixes = new ArrayList<Prefix>();
		Prefix oPrefix, oTablePrefix;
		Suffix oSuffix;
		int iIndex, iSuffixCount;
		
		//	DEBUG
		ArrayList<Suffix> aloSuffixes;
		int iTablesSizeMin = Integer.MAX_VALUE, iTablesSizeMax = 0, iHashSize = 0;
		long i64TablesTotSize = 0;
		double dTableAvgSize = 0.0;
		//	EO.DEBUG

		// 1. pass on long strings and add to table and sort
		while(oIter.hasNext()) {
			iHashSize++;

			Map.Entry<String, Prefix> oPair = (Map.Entry<String, Prefix>) oIter.next();
			oPrefix = (Prefix) oPair.getValue();
			if (oPrefix.m_aloSuffixes != null) {
				m_alPrefices.add(oPrefix);
				m_u64SplitSuffixCount += oPrefix.m_aloSuffixes.size();
			}
		}
		Collections.sort(m_alPrefices);

		// 2. pass on short strings and add to temp table and then sort
		oIter = m_hmPrefices.entrySet().iterator();
		while(oIter.hasNext()) {
			Map.Entry<String, Prefix> oPair = (Map.Entry<String, Prefix>)oIter.next();
			oPrefix = (Prefix) oPair.getValue();
			
			if (oPrefix.m_aloSuffixes != null)
				continue;
			alShortSuffixes.add(oPrefix);
			m_u64SplitSuffixCount += alShortSuffixes.size();
		}
		Collections.sort(alShortSuffixes);
		
		// go through temp table and add all short strings using binary search
		for (int i=alShortSuffixes.size()-1; i>=0; i--){
			oPrefix = alShortSuffixes.get(i);

			oSuffix = new Suffix( oPrefix );
			oSuffix.m_iCount = oPrefix.m_iCount;
			
			iIndex = Collections.binarySearch(m_alPrefices, oPrefix);
			iIndex = -iIndex-1;
			
			if ( iIndex >= m_alPrefices.size() ) {
				oTablePrefix = new Prefix( oSuffix );
				oTablePrefix.m_sKey = s_SuffixAsString( oSuffix );
				oTablePrefix.m_aloSuffixes = new ArrayList<Suffix>();
				oTablePrefix.m_aloSuffixes.add(oSuffix);
				m_alPrefices.add(m_alPrefices.size(), oTablePrefix);
			}
			else {
				oTablePrefix = m_alPrefices.get(iIndex);
				oTablePrefix.m_sKey = s_SuffixAsString( oSuffix );
				oTablePrefix.m_aloSuffixes.add(0, oSuffix);
			}
		}
		
		//
		//	Save Suffix Array (of split) to file
		//
		m_u64TimeSt = System.currentTimeMillis();
			
		//
		//	Open proper SPLIT file
		//
		if ( _bXml ) {
			m_oSAXml = new SAXmlFile();
			if ( !m_oSAXml.b_Init( m_sWorkFolder, _iSplit, SAXmlFile.E_XmlFileType.eOUTPUT, "8" ) ) {
				Utils.debugLogAndPrintLn( m_oSAXml.s_ErrorMsg() ); 
				return false;
			}
		}
		else {
			m_oSAFile = new SASplitFile();
			if ( !m_oSAFile.b_Init( m_sWorkFolder, _iSplit, E_FileType.eOUTPUT, m_iSplitLen, _bSaveToFile ) ) {
				Utils.debugLogAndPrintLn( m_oSAFile.s_ErrorMsg() ); 
				return false;
			}
		}
			
		//
		//	Count (again) number of SUFFIX
		//
		iSuffixCount = 0;
		for (int i=0; i < m_alPrefices.size(); i++ ){
			aloSuffixes = m_alPrefices.get(i).m_aloSuffixes;
			iSuffixCount += aloSuffixes.size();
		}

		if ( _bXml ) {
			if ( !m_oSAXml.b_WriteHeader() )
				return false;
			if ( !m_oSAXml.b_WriteComment( m_sProgName + " " + m_sProgVers + Utils.s_NowTimestamp() ) )
				return false;
			m_oSAXml.b_TagOpen( "PROG" );
				m_oSAXml.b_TagOpen( "NAME" );
					m_oSAXml.b_TagDataAsString( m_sProgName );
				m_oSAXml.b_TagClose();
				m_oSAXml.b_TagOpen( "VERSION" );
					m_oSAXml.b_TagDataAsString( m_sProgVers );
				m_oSAXml.b_TagClose();
				m_oSAXml.b_TagOpen( "RECORDS" );
					m_oSAXml.b_TagDataAsString( String.format( "%d", iSuffixCount ) );
				m_oSAXml.b_TagClose();
				
			m_oSAXml.b_TagClose();
		}
		else {
			if (!m_oSAFile.b_WriteHeader( m_sProgName, m_sProgVers, iSuffixCount ) )
				return false;
		}

		// loop on all tables
		if ( _bXml ) {
			for (int i=0; i < m_alPrefices.size(); i++ ){
				aloSuffixes = m_alPrefices.get(i).m_aloSuffixes;
				// loop on specific table
				for (int j=0; j < aloSuffixes.size(); j++){
					// get suffix
					oSuffix = aloSuffixes.get(j);
					if ( !m_oSAXml.b_TagAndString( "SX", String.format("%d,%d,%d,%d", oSuffix.m_iLine, oSuffix.m_iPos, oSuffix.m_iLen, oSuffix.m_iCount) ) )
						return false;
				}
			}
		}
		else {
			// loop on all tables
			for (int i=0; i < m_alPrefices.size(); i++ ){
				aloSuffixes = m_alPrefices.get(i).m_aloSuffixes;
				// loop on specific table
				for (int j=0; j < aloSuffixes.size(); j++){
					// get suffix
					oSuffix = aloSuffixes.get(j);
					if ( !m_oSAFile.b_PutRecord( oSuffix ) )
						return false;
				}
			}
		}
		
		if ( _bXml ) {
			if ( !m_oSAXml.b_Done() )
				return false;
		}
		else {
			if ( !m_oSAFile.b_Done( false, true ) )
				return false;
		}

		m_64TimeSave = Utils.u64_Elapsed( m_u64TimeSt );

		//	DEBUG
		if ( m_bDebug ) {
			for (int i = 0; i < m_alPrefices.size(); i++){
				aloSuffixes = m_alPrefices.get(i).m_aloSuffixes;
				if ( iTablesSizeMin > aloSuffixes.size() )
					iTablesSizeMin = aloSuffixes.size();
				if ( iTablesSizeMax < aloSuffixes.size() )
					iTablesSizeMax = aloSuffixes.size();
				i64TablesTotSize += aloSuffixes.size();
			}
			
			if ( m_alPrefices.size() > 0 )
				dTableAvgSize = (double) i64TablesTotSize / (double) m_alPrefices.size();
			else dTableAvgSize = 0.0;
			
			if ( iTablesSizeMin > Integer.MAX_VALUE / 2 )
				iTablesSizeMin = 0;
			Utils.debugLogLn( "Tables: Total=" + m_alPrefices.size() +
					", Min.Size=" + iTablesSizeMin +
					", Max.Size=" + iTablesSizeMax +
					", Average.Size=" + String.format( "%8.3f", dTableAvgSize ) +
					", Hash.size=" + String.format( "%,6d", iHashSize ) );
			
			//
			//	Accumulate debug Statistics
			//
			if ( m_u64SplitNodes > m_u64SplitMaxNodes )
				m_u64SplitMaxNodes = m_u64SplitNodes; 

			if ( m_u64SplitSuffixCount > m_u64SplitMaxSuffixCount )
				m_u64SplitMaxSuffixCount = m_u64SplitSuffixCount; 
			
			if ( Prefix.m_u64SplitPrefixCount > m_u64SplitMaxPrefixCount )
				m_u64SplitMaxPrefixCount = Prefix.m_u64SplitPrefixCount; 
		}
		//	EO.DEBUG
		return true;
	}
	
	//
	//	AllDone
	//
	public boolean b_AllDone( boolean _bXml ){
		if ( !_bXml ) {
			m_oSAFile.b_Done( true, false );
		}
		return true;
	}
	
	//
	//	s_SaveFileName
	//
	public String s_SaveFileName( boolean _bXml ) {
		if ( _bXml ) {
			if ( m_oSAXml == null )
				return "";
			return m_oSAXml.s_SaveFileName();
		}
		else {
			if ( m_oSAFile == null )
				return "";
			return m_oSAFile.s_SaveFileName();
		}
	}
	
	//
	//	b_LoadSuffixArray
	//
	public boolean b_LoadSuffixArray( int _iSplit, boolean _bLoadFromFile, boolean _bXml ) {
		String [] sData = null;
		Prefix oPrefix;
		Suffix oSuffix;
		int iSuffixCount = 0;
		
		//
		//	Load Suffix Array (of split) from file
		//
		m_u64TimeSt = System.currentTimeMillis();
			
		//
		//	Open proper SPLIT file
		//
		if ( _bXml ) {
			m_oSAXml = new SAXmlFile();
			if ( !m_oSAXml.b_Init( m_sWorkFolder, _iSplit, SAXmlFile.E_XmlFileType.eINPUT, "8" ) ) {
				Utils.debugLogAndPrintLn( m_oSAXml.s_ErrorMsg() ); 
				return false;
			}
		}
		else {
			m_oSAFile = new SASplitFile();
			if ( !m_oSAFile.b_Init( m_sWorkFolder, _iSplit, E_FileType.eINPUT, m_iSplitLen, _bLoadFromFile ) ) {
				Utils.debugLogAndPrintLn( m_oSAFile.s_ErrorMsg() ); 
				return false;
			}
		}
			
		
		if ( _bXml ) {
			if ( !m_oSAXml.b_ReadHeader() )
				return false;
			m_sComment = m_oSAXml.s_ReadComment();
			if ( !m_oSAXml.b_ParseUntil( "PROG/RECORDS" )  )
				return false;
			iSuffixCount = m_oSAXml.i_Data();
		}
		else {
			if ( !m_oSAFile.b_ReadHeader() ) {
				Utils.debugLogAndPrintLn( m_oSAXml.s_ErrorMsg() ); 
				return false;
			}
			 iSuffixCount = m_oSAFile.i_Records();
		}
		
		// generate a table of SUFFIX for this SPLIT
		oPrefix = new Prefix( 0, 0, 0 );
		oPrefix.m_aloSuffixes = new ArrayList<Suffix>();
		if ( _bXml ) {
			for (int j=0; j < iSuffixCount; j++){
				oSuffix = new Suffix();

				if ( !m_oSAXml.b_ParseUntil( "SX" )  )
					return false;
				sData = m_oSAXml.s_Data().split( "," );
				if ( sData.length < 4 ) {
					return false;
				}
				oSuffix.m_iLine = Utils.i_safeStringToInt( sData[0] );
				oSuffix.m_iPos = Utils.i_safeStringToInt( sData[1] );
				oSuffix.m_iLen = Utils.i_safeStringToInt( sData[2] );
				oSuffix.m_iCount = Utils.i_safeStringToInt( sData[3] );
				oPrefix.m_aloSuffixes.add( oSuffix );
			}
		}
		else {
			if ( !m_oSAFile.b_ReadBuffer() )
				return false;

			for (int j=0; j < iSuffixCount; j++){
				oSuffix = m_oSAFile.o_ReadRecord();
				if ( oSuffix == null )
					return false;
				if ( oSuffix.m_iLen == 0 )
					oSuffix.m_iLen = Math.min( Suffix.m_aclDb.get( oSuffix.m_iLine ).length - oSuffix.m_iPos, m_iMaxLen );
				oPrefix.m_aloSuffixes.add( oSuffix );
			}
		}
		m_alPrefices.add( oPrefix );

		if ( _bXml ) {
			if ( !m_oSAXml.b_Done() )
				return false;
		}
		else {
			if ( !m_oSAFile.b_Done( false, false ) )
				return false;
		}

		m_64TimeLoad = Utils.u64_Elapsed( m_u64TimeSt );
		Utils.debugDebugFlush();
		
		return true;
	}
	
	//
	//	insertToTopCount 
	//
	void insertToTopCount(Infix _oInfix){
		Infix oInfix;

		int iPos = Collections.binarySearch(m_olTopCount, _oInfix, pf_compareToCount);
		
		if ( iPos < 0 )
			iPos = -iPos-1;
		
		oInfix = new Infix(_oInfix);
		oInfix.m_iTopSplit = m_iCurrentSplit;
		m_olTopCount.add( iPos, oInfix );
		
		if ( m_olTopCount.size() >= m_iTop ){
			m_bTopCountFull = true;
			m_olTopCount.remove(0);
		}
	}
	
	//
	//	insertToTopScore 
	//
	void insertToTopScore(Infix _oInfix){
		Infix oInfix;

		int iPos = Collections.binarySearch(m_olTopScore, _oInfix, pf_compareToScore);
		
		if (iPos < 0)
			iPos = -iPos-1;
		
		oInfix = new Infix(_oInfix);
		oInfix.m_iTopSplit = m_iCurrentSplit;
		m_olTopScore.add( iPos, oInfix );
		
		if ( m_olTopScore.size() >= m_iTop ){
			m_bTopScoreFull = true;
			m_olTopScore.remove(0);
		}
	}
	
	//
	//	checkAndStoreTop 
	//
	public void checkAndStoreTop(Infix _oInfix){
		//
		//	Check for all criteria
		//
		if ( !m_bTopCountFull ||( _oInfix.m_iCount > m_iTopCount ) ){
			insertToTopCount(_oInfix);
			m_iTopCount = m_olTopCount.get(0).m_iCount;
		}

		if ( !m_bTopScoreFull || ( _oInfix.m_dStdScore > m_dTopScore ) ){
			insertToTopScore(_oInfix);
			m_dTopScore = m_olTopScore.get(0).m_dStdScore;
		}
	
	}
	
	//
	//	printTop 
	//
	public void printTop(){
		PrintWriter oWriter;
		ArrayList<Infix> alTopToPrint = null;
		String sFileName = Utils.s_FormFileName( m_sOutputFolder, sC_TOPSCORES_NAME, ".txt" ); 
		String sFormat = "%,12d   %,10.3f   %30s\n", sTopInfix;
		Infix oInfix;
		byte [] caInfix = new byte [0];
		
		try{
			oWriter = new PrintWriter(sFileName, "UTF-16LE");
		} catch (IOException e) {
			System.out.println("unable to open file '"+sFileName+"'");
			return;
		}
		
		switch(m_eSortCode){
		case eCOUNT:
			alTopToPrint =  m_olTopCount;
			break;
		case eSCORE:
			alTopToPrint = m_olTopScore;
			break;
		default:
			break;
		}
		
		if ( alTopToPrint != null ) {
			//
			//	Print largest numbers at top
			//
			for ( int k = alTopToPrint.size()-1; k >= 0; k-- ) {
				oInfix = alTopToPrint.get(k);
				caInfix = oInfix.m_caInfix;
				sTopInfix = new String(caInfix);

				oWriter.write( String.format( sFormat,
					oInfix.m_iCount, oInfix.m_dStdScore, Utils.s_PadRight( sTopInfix, 64) ) );
			}
		}
		
		oWriter.close();
	}
	
	//
	//	insertSuffix: Add a new suffix to: HASH (is shorter than Hash.depth) or Table of same PREFIX (long SUFFIX only) 
	//
	void insertSuffix( String _sPrefix, int _iLine, int _iPos, int _iLen ){
		Prefix oPrefix;
		Suffix oSuffix;
		int iIndex, iSuffixMax;
		
		// Look for suffix in hash
		oPrefix = m_hmPrefices.get(_sPrefix);
		
		iSuffixMax = Math.min( _iLen, m_iMaxLen );
		
		if (oPrefix == null){
			// Create new hash entry
			Prefix.m_u64SplitPrefixCount++;
			
			oPrefix = new Prefix( _iLine, _iPos, iSuffixMax );
			oPrefix.m_sKey = _sPrefix;

			if ( iSuffixMax < m_iHashDepth ) {
				oPrefix.m_iCount = 1;
			}
			else {
				oPrefix.m_iCount = 0;

				// Create a restricted suffix array for this prefix
				oSuffix = new Suffix( oPrefix );
				m_u64SplitNodes++;
				oSuffix.m_iCount = 1;
					
				oPrefix.m_aloSuffixes = new ArrayList<Suffix>();
				oPrefix.m_aloSuffixes.add( oSuffix );
			}
			
			m_hmPrefices.put(_sPrefix, oPrefix);
		}
		else{
			if ( iSuffixMax >= m_iHashDepth ){
				m_oSuffix.m_iLine = _iLine;
				m_oSuffix.m_iPos = _iPos;	
				m_oSuffix.m_iLen = iSuffixMax;	
				
				iIndex = Collections.binarySearch(oPrefix.m_aloSuffixes, m_oSuffix);
				if (iIndex >= 0){
					oSuffix = oPrefix.m_aloSuffixes.get(iIndex);
					oSuffix.m_iCount++;
				}
				else{
					iIndex = -iIndex-1;
					
					oSuffix = new Suffix( m_oSuffix );
					m_u64SplitNodes++;
					oSuffix.m_iCount = 1;
					
					oPrefix.m_aloSuffixes.add(iIndex, oSuffix);
				}
			}
			else{
				oPrefix.m_iCount++;
			}
		}
	}
	
	//
	//	buildSuffixArray 
	//
	public boolean b_BuildSuffixArray( int _iSplit, boolean _bMatchAny ){
		//	NOTE: sSplit is a string of chars, representing a selection of suffixes (see below) 
		//
		// _iSplit: 	-1		= insert all suffixes for each line (short files)
		//				0		= insert all suffixes which length are less than the selected Split size
		//				1-N		= insert suffixes of selected Split, where 
		// _bMatchAny:	true	= insert any char of the Split
		//				false	= insert only suffix starting with the selected Split

		int iDbSize = Suffix.m_aclDb.size(), iLineLen, iSuffixLen, iSuffixStop, iSplitLen, iShortStop = m_iSplitLen - 1;
		String sPrefix;
		byte [] acLine, acSplit = null;
		boolean bAsSplit;
		
		m_oTimestamp = new Timestamp(System.currentTimeMillis());
		Utils.beginProgress( iDbSize );
		
		m_iHashDepth = iC_STD_TABLE_REF / (int) Math.round( Math.sqrt( (double) m_sAlpha.length()) );

		if ( _iSplit > 0 ) {
			acSplit = m_alSplits.get( _iSplit - 1 ).getBytes( StandardCharsets.UTF_8 );
			if ( acSplit.length < 2 )
				iSplitLen = 2;
			else
				iSplitLen = m_iSplitLen;
		}
		else iSplitLen = m_iSplitLen;

		for ( int iLine = 0; iLine < iDbSize; iLine ++ ){
			acLine = Suffix.m_aclDb.get(iLine);
			iLineLen = acLine.length;
				
			Utils.showProgress();
				
			if ( _iSplit > 0 ) {
				iSuffixStop = iLineLen - iSplitLen + 1;
				for ( int iCh = 0; iCh < iSuffixStop; iCh++ ){
					//
					// Match full SPLIT: insert only suffixes == selected Split
					//
					if ( !_bMatchAny ) {
						bAsSplit = true;
						for ( int iPref = 0; iPref < m_iSplitLen; iPref++ ){
							if ( acSplit[iPref] != acLine[iCh+iPref] ) {
								bAsSplit = false;
								break;
							}
						}
						if ( bAsSplit ) {
							iSuffixLen = iLineLen - iCh;
							if ( iSuffixLen >= m_iHashDepth ) {
								sPrefix = new String( Arrays.copyOfRange( acLine, iCh, iCh + m_iHashDepth ) );
							}
							else {
								sPrefix = new String( Arrays.copyOfRange( acLine, iCh, iCh + iSuffixLen ) );
							}
							insertSuffix( sPrefix, iLine, iCh, iSuffixLen );
						}
					}
					//
					// _bMatchAny = insert suffixes whose 1st char appears in selected Split
					//
					else {
						for ( int iShort = 0; iShort < iSplitLen - 1; iShort++ ){
							if ( acLine[iCh] == acSplit[iShort] ) {
								iSuffixLen = iLineLen - iCh;
								if ( iSuffixLen >= m_iHashDepth )
									sPrefix = new String( Arrays.copyOfRange( acLine, iCh, iCh + m_iHashDepth ) ); 
								else {
									sPrefix = new String( Arrays.copyOfRange( acLine, iCh, iCh + iSuffixLen ) );
								}
								insertSuffix( sPrefix, iLine, iCh, iSuffixLen );
								break;
							}
						}
						continue;
					}
				}
			}
			//
			// insert suffixes with length shorter than Split
			//
			else if ( _iSplit == 0 ) {
				iSuffixStop = iLineLen - iShortStop + 1;
				for ( int iCh = 0; iCh < iSuffixStop; iCh++ ){
					for ( int iShort = 0; iShort < iShortStop; iShort++ ){
						sPrefix = new String( Arrays.copyOfRange( acLine, iCh, iCh + iShort + 1 ) );
						insertSuffix( sPrefix, iLine, iCh, iShort + 1 );
					}
				}
			}
			//
			// _iSplit: Negative = insert any suffixes of line
			//
			else {
				iSuffixStop = iLineLen;
				for ( int iCh = 0; iCh < iSuffixStop; iCh++ ){
					iSuffixLen = iLineLen - iCh;

					if ( iSuffixLen >= m_iHashDepth )
						sPrefix = new String( Arrays.copyOfRange( acLine, iCh, iCh + m_iHashDepth ) ); 
					else {
						sPrefix = new String( Arrays.copyOfRange( acLine, iCh, iCh + iSuffixLen ) );
					}
					insertSuffix( sPrefix, iLine, iCh, iSuffixLen );
				}
			}
		}

		Utils.endProgress();
		
		return true;
	}
	
}
