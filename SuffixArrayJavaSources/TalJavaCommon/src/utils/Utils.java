package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

public class Utils {
	static Runtime m_oRuntime = null;

	static Writer m_oLogWriter = null;
	static long m_i64LogLines = 0;
	static String m_sLogFileName = null;
	static Writer m_oDebugWriter = null;
	static long m_i64DebugLines = 0;
	static String m_sDebugFileName = null;
	
	static long i64DebugProgressMax = 0, i64DebugProgressStep = 0, i64DebugProgressCount = 0, i64DebugProgressSlice = 0;
	static boolean m_bDebug = false, m_bProgress = false, m_bProgressE = false;
	
	//
	//	Date-Time constants
	//
	public static final int
			iC_MONTHES_IN_YEAR	= 12,
			iC_MS_IN_SEC		= 1000,
			iC_SECS_IN_MIN		= 60,
			iC_MINS_IN_HOUR		= 60,
			iC_HOURS_IN_DAY		= 24,
			iC_MS_IN_MIN		= 60000,
			iC_MS_IN_HOUR		= 3600000,
			iC_MS_IN_DAY		= 86400000,
			iC_SECS_IN_HOUR		= 3600,
			iC_SECS_IN_DAY		= 86400;
	
	public static final int iC_YEAR_POS = 0, iC_MONTH_POS = 1, iC_DAY_POS = 2,
			iC_HOUR_POS = 3, iC_MIN_POS = 4, iC_SEC_POS = 5, iC_MS_POS = 6;
	
	//
	//	Memory size constants
	//
	public enum E_TecMEM {
		eMax, eTotal, eAvail, eFree;
	}

	public static final long
		iC_SIZE_1K = 1024, iC_SIZE_1M = 1024*iC_SIZE_1K, iC_SIZE_1G = 1024*iC_SIZE_1M;

	public static boolean m_bError = false;
	public static long m_u64StartTime, m_u64PrevTime;
	
	public static class PairLongInt implements Comparable<PairLongInt> {
		public long m_u64Val;
		public int m_iVal;
		
		public PairLongInt(long _u64Val, int _iVal){
			this.m_u64Val = _u64Val;
			this.m_iVal = _iVal;
		}
		
		public int compareTo(PairLongInt _oPairLongInt) {
			if (this.m_u64Val < _oPairLongInt.m_u64Val)
				return -1;
			else if (this.m_u64Val > _oPairLongInt.m_u64Val)
				return 1;
			else return 0;
		}
	}
	
	//
	//	init	: Setup time
	//
	public static void init(){
		m_u64StartTime = System.currentTimeMillis();
		m_u64PrevTime = m_u64StartTime;
		
		m_oRuntime = Runtime.getRuntime();
	}
	
	//
	//	setParams: Save general parameters
	//
	public static void setParams( boolean _bDebug, boolean _bProgress ){
		m_bDebug = _bDebug;
		m_bProgress = _bProgress;
	}
	
	//
	//	beginProgress: Save progress feedback parameters & initiate
	//
	public static void beginProgress( long _iMax ) {
		i64DebugProgressMax = _iMax;
		i64DebugProgressStep = 0;
		i64DebugProgressCount = 0;
		i64DebugProgressSlice = _iMax / 100;
		m_bProgressE = false;
	}
	
	//
	//	showProgress: show progress feedback
	//
	public static void showProgress() {
		if (!m_bProgress)
			return;
		i64DebugProgressStep++;
		if ( m_bDebug & ( i64DebugProgressStep > i64DebugProgressSlice ) ) {
			if ( i64DebugProgressCount % 20 == 0 ) {
				System.out.print( String.format( "%3d",  i64DebugProgressCount ) + "%" );
			}

			i64DebugProgressCount++;
			i64DebugProgressStep = 0;

			m_bProgressE = !m_bProgressE;
			if ( m_bProgressE )
				System.out.print( "." );
		}
	}
	
	//
	//	endProgress: end progress feedback
	//
	public static void endProgress() {
		if (!m_bProgress)
			return;
		System.out.println( String.format( "%3d",  100 ) + "%" );
	}
	
	//
	//	s_FormFileName	: build a filename from path, name and extension
	//
	public static String s_FormFileName( String _sPath, String _sFileName, String _sExt ){
		String	sPath;
		if ( _sPath != null ) {
			
			if ( _sPath.isEmpty() ) sPath = "";
			else sPath = _sPath + getPathSeparator();

			return sPath + s_fileNameOf( _sFileName ) + _sExt;
		}
		else return null;
	}
	
	//
	//	debugLogInit	: open LOG file
	//
	public static void debugLogInit( String _sPath, String _sFileName, boolean _bDebugLog ){
		m_sLogFileName = s_FormFileName( _sPath, _sFileName, ".log" );
		if ( m_sLogFileName != null ) {
			try{
				m_oLogWriter = new PrintWriter( m_sLogFileName, "UTF-16LE");
			} catch (IOException e) {
				System.out.println("***ERROR: Unable to open LOG '"+ m_sLogFileName + "'" );
			}
		}
		m_sDebugFileName = s_FormFileName( _sPath, _sFileName, "_debug.log" );
		if ( _bDebugLog && ( m_sDebugFileName != null ) ) {
			try{
				m_oDebugWriter = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(m_sDebugFileName), "UTF-8") );
			} catch (IOException e) {
				System.out.println("***ERROR: Unable to open DEBUG.LOG '"+ m_sDebugFileName + "'" );
			}
		}
	}
	
	//
	//	debugLogLn
	//
	public static void debugLogLn( String _sText ) {
		if ( m_oLogWriter != null ) {
			try {
				m_oLogWriter.write( _sText );
				m_oLogWriter.write( "\n" );
				m_i64LogLines++;
			} catch( IOException e ) {
				System.out.println("***ERROR: Unable to write to LOG '"+ m_sLogFileName + "'" );
			}
		}
	}
	
	//
	//	debugLogAndPrintLn
	//
	public static void debugLogAndPrintLn( String _sText, char _bSep ){
		System.out.println( String.format( "%s", _sText ) );
		if ( _bSep != ' ' ) {
			System.out.println( String.format("%064d", 0).replace( '0', _bSep ) );
		}
		if ( m_oLogWriter != null ) {
			try {
				m_oLogWriter.write( _sText );
				m_oLogWriter.write( "\n" );
				m_i64LogLines++;
				if ( _bSep != ' ' ) {
					m_oLogWriter.write( String.format("%064d", 0).replace( '0', _bSep ) );
					m_oLogWriter.write( "\n" );
					m_i64LogLines++;
				}
			} catch( IOException e ) {
				System.out.println("***ERROR: Unable to write to LOG '"+ m_sLogFileName + "'" );
			}
		}
	}
	
	
	//
	//	debugLogAndPrintLn - wrapper
	//
	public static void debugLogAndPrintLn( String _sText ){
		debugLogAndPrintLn( _sText, ' ' );
	}
	
	//
	//	debugDebugLn
	//
	public static void debugDebugLn( String _sText ){
		if ( m_oDebugWriter != null ) {
			try {
				m_oDebugWriter.write( _sText );
				m_oDebugWriter.write( "\r\n" );
				m_i64DebugLines++;
			} catch( IOException e ) {
				System.out.println("***ERROR: Unable to write to LOG '"+ m_sDebugFileName + "'" );
			}
		}
	}
	
	//
	//	debugLogFlush
	//
	public static void debugLogFlush() {
		if ( m_oLogWriter != null ) {
			try {
				m_oLogWriter.flush();;
			} catch( IOException e ) {
				System.out.println("***ERROR: Unable to write to LOG '"+ m_sLogFileName + "'" );
			}
		}
	}
	
	//
	//	debugDebugFlush
	//
	public static void debugDebugFlush() {
		if ( m_oDebugWriter != null ) {
			try {
				m_oDebugWriter.flush();;
			} catch( IOException e ) {
				System.out.println("***ERROR: Unable to write to DEBUG '"+ m_sDebugFileName + "'" );
			}
		}
	}
	
	//
	//	done	: finalize LOG
	//
	public static void debugLogdone(){
		if ( m_oLogWriter != null ) {
			try {
				m_oLogWriter.close();
			} catch( IOException e ) {
				System.out.println("***ERROR: Unable to close LOG '"+ m_sLogFileName + "'" );
			}
			if ( m_i64LogLines == 0 ) {
				File oFile = new File( m_sLogFileName );
			    if ( oFile.exists() )
			    	oFile.delete();     
			}

		}
		if ( m_oDebugWriter != null ) {
			try {
				m_oDebugWriter.close();
			} catch( IOException e ) {
				System.out.println("***ERROR: Unable to close LOG '"+ m_sDebugFileName + "'" );
			}
			if ( m_i64DebugLines == 0 ) {
				File oFile = new File( m_sDebugFileName );
			    if ( oFile.exists() )
			    	oFile.delete();     
			}
		}
	}
	
	//
	//	s_FormatCrc
	//
	public static String s_FormatCrc( long _u64Crc ) {
		String sCrcAsHex = "00000000" + Long.toHexString( _u64Crc );
		return sCrcAsHex.substring( sCrcAsHex.length() - 8 );
	}

	//
	//	s_PadRight
	//
	public static String s_PadRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}

	//
	//	s_PadLeft
	//
	public static String s_PadLeft(String s, int n) {
		return String.format("%1$" + n + "s", s);
	}
	
	//
	//	i_safeStringToInt
	//
	public static int i_safeStringToInt(String _sStr){
		m_bError = false;
		try{
			return Integer.parseInt(_sStr);
		} catch (NumberFormatException oE){
			m_bError = true;
			return 0;
		}
	}
	
	//
	//	ProgramPause
	//
	public static void ProgramPause( int _iMs ) {
		int	iSec, iMs = 0;
		try {
			if ( _iMs >= 1000 ) {
				iSec = _iMs / 1000;
				iMs = _iMs - iSec*1000;
				TimeUnit.SECONDS.sleep( iSec );
			}
			else iMs = _iMs;
			if ( iMs > 0 ) 
				Thread.sleep(iMs);
			
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
	}
	
	//
	//	s_FormatSize	= returns formatted size in bytes, KB, MB or GB
	//
	public static String s_FormatSize( long _u64Size ) {
		String sResult;

		if ( _u64Size > iC_SIZE_1G ) {
			sResult = String.format( "%6.2f", (double) _u64Size / (double) iC_SIZE_1G ) + " GB"; 
		}
		else if ( _u64Size > iC_SIZE_1M ) {
			sResult = String.format( "%6.2f", (double) _u64Size / (double) iC_SIZE_1M ) + " MB"; 
		}
		else if ( _u64Size > iC_SIZE_1K ) {
			sResult = String.format( "%6.2f", (double) _u64Size / (double) iC_SIZE_1K ) + " KB"; 
		}
		else if ( _u64Size >= 0 ) sResult = String.format( "%3d", _u64Size ) + " B";
		else sResult = "??? NEGATIVE size";
		return sResult;
	}

	
	//
	//	u64_Elapsed: elapsed time from previous or start
	//
	public static long u64_Elapsed(boolean _bFromStart ){
		long u64ThisTime = System.currentTimeMillis(), u64Elapsed;
		
		if (_bFromStart){
			u64Elapsed = u64ThisTime - m_u64StartTime;
		}
		else{
			u64Elapsed = u64ThisTime - m_u64PrevTime;
		}
		m_u64PrevTime = u64ThisTime;
		return u64Elapsed;
	}
	
	//
	//	u64_Elapsed: elapsed time from given time
	//
	public static long u64_Elapsed(long _u64Time){
		return System.currentTimeMillis() - _u64Time;
	}
	
	//
	//	s_StampTime: format (elapsed) time (from previous or start)
	//
	public static String s_StampTime(long _u64RefTime,  String _sStamp ){
		int iHour, iMin, iSec, iMs;
		
		if ( _u64RefTime < 0 ) {
			return String.format( "%s=??? ms", _sStamp );
		}

		if ( _u64RefTime < iC_MS_IN_SEC ) {
			return String.format( "%s=%3d ms", _sStamp, _u64RefTime );
		}
		if ( _u64RefTime < iC_MS_IN_MIN ) {
			iSec = (int) ( _u64RefTime / iC_MS_IN_SEC );
			iMs = (int) ( _u64RefTime - iSec * iC_MS_IN_SEC );
			return String.format( "%s=%2d.%03d s", _sStamp, iSec, iMs );
		}

		if ( _u64RefTime < iC_MS_IN_HOUR ) {
			iMin = (int) ( _u64RefTime / iC_MS_IN_MIN );
			iSec =  (int) ( _u64RefTime - iMin * iC_MS_IN_MIN ) / iC_MS_IN_SEC;
			return String.format( "%s=00:%02d:%02d", _sStamp, iMin, iSec );
		}
		
		iHour = (int) ( _u64RefTime / iC_MS_IN_HOUR );
		iMin = (int) ( ( _u64RefTime - iHour * iC_MS_IN_HOUR ) / iC_MS_IN_MIN );
		iSec =  (int) ( _u64RefTime - iHour * iC_MS_IN_HOUR - iMin * iC_MS_IN_MIN ) / iC_MS_IN_SEC;
		return String.format( "%s=%3d:%02d:%02d", _sStamp, iHour, iMin, iSec );
	}
	
	//
	//	s_StampElapsed: format elapsed time from given
	//
	public static String s_StampElapsed(long _u64Time, String _sStamp ){
		return s_StampTime( u64_Elapsed( _u64Time ), _sStamp );
	}
	
	//
	//	s_StampTime: format elapsed time (from previous or start)
	//
	public static String s_StampTime( String _sStamp, boolean _bFromStart ){
		return s_StampTime( u64_Elapsed( _bFromStart ), _sStamp );
	}
	
	//
	//	getTimestamp: format a time-stamp
	//
	public static String getTimestamp(Timestamp _oTimestamp, String _sSepDate, String _sSepParts, boolean _bAddMs){
		String sTimestamp = _oTimestamp.toString(), sRes;
		String[] saTimestamp = sTimestamp.split("-|\\.|:| ");
	
		sRes = saTimestamp[iC_YEAR_POS]+_sSepDate+saTimestamp[iC_MONTH_POS]+_sSepDate+saTimestamp[iC_DAY_POS]+
				_sSepParts+saTimestamp[iC_HOUR_POS]+saTimestamp[iC_MIN_POS]+saTimestamp[iC_SEC_POS];
		if (_bAddMs)
			sRes += "."+saTimestamp[iC_MS_POS];
		return sRes;
	}
	
	//
	//	getTimestamp
	//
	public static String getTimestamp(Timestamp _oTimestamp){
		return getTimestamp(_oTimestamp, "-", "_", false);
	}
	
	//
	//	s_NowTimestamp
	//
	public static String s_NowTimestamp(){
		return getTimestamp(new Timestamp(System.currentTimeMillis()), "-", "_", false);
	}
	
	//
	//	s_RunFolder
	//
	public static String s_RunFolder(){
		return Paths.get(".").toAbsolutePath().normalize().toString();
	}
	
	//
	//	s_filePathOf
	//
	public static String s_filePathOf(String _sFileName){
		Path oPath = Paths.get(_sFileName);
		if ( ( oPath == null ) || ( oPath.getParent() == null ) ) return _sFileName;
		return oPath.getParent().toString();
	}
	
	//
	//	s_fileNameOf
	//
	public static String s_fileNameOf(String _sFileName){
		String sFilename = s_fileFileNameOf( _sFileName );
		int iPos = sFilename.lastIndexOf('.');
		if ( iPos < 0 ) return sFilename;
		return sFilename.substring( 0, iPos ); 
	}
	
	//
	//	s_fileFNameOf
	//
	public static String s_fileFNameOf(String _sPath){
		Path oPath = Paths.get(_sPath);
		return oPath.getFileName().toString();
	}
	
	//
	//	s_fileFileNameOf
	//
	public static String s_fileFileNameOf(String _sFileName){
		Path oPath = Paths.get(_sFileName);
		if ( ( oPath == null ) || ( oPath.getParent() == null ) ) return _sFileName;
		
		return _sFileName.substring( oPath.getParent().toString().length() + getPathSeparator().length() );
	}
	
	//
	//	s_fileExtensionOf
	//
	public static String s_fileExtensionOf(String _sFileName){
		int iPos = _sFileName.lastIndexOf('.');
		if ( iPos < 0 ) return "";
		return _sFileName.substring( iPos ); 
	}
	
	//
	//	b_MakeFolder
	//
	public static boolean b_MakeFolder(String _sFolder){
		if (Files.exists(Paths.get(_sFolder)))
			return true;
		else{
			try{
				File oFile = new File(_sFolder);
				oFile.mkdir();
			} catch (SecurityException oE) {
				return false;
			}
			return true;
		}
			
	}
	
	//
	//	cleanFolder
	//
	public static int cleanFolder(String _sFolder){
		File oFolder = new File(_sFolder), oFile;
		File[] sArrFiles = oFolder.listFiles();
		int iCount = 0;
		
		for(int i=0; i<sArrFiles.length ;i++){
			oFile = sArrFiles[i];
			if (!oFile.isDirectory()){
				oFile.delete();
				iCount++;
			}
		}
		return iCount;
	}

	//
	//	i_CleanExtension
	//
	public static int i_CleanExtension( String _sFolder, String _sExt ){
		File oFolder = new File(_sFolder), oFile;
		File[] sArrFiles = oFolder.listFiles();
		int iCount = 0;
		
		for( int iFile = 0; iFile < sArrFiles.length ; iFile++ ){
			oFile = sArrFiles[iFile];
			if ( !oFile.isDirectory() ) {
				if ( s_fileExtensionOf ( oFile.getName() ).equals( _sExt ) ) {
					oFile.delete();
					iCount++;
				}
			}
		}
		return iCount;
	}

	//
	//	P L A T F O R M   D E P E N D A N T   H A N D L E R S
	//

	//
	//	isUnix
	//
	public static boolean isUnix(){
		return (System.getProperty("os.name").toLowerCase().indexOf("uni") >= 0);
	}
	
	public static boolean isWindows(){
		return (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0);
	}
	
	public static boolean isLinux(){
		return (System.getProperty("os.name").toLowerCase().indexOf("lin") >= 0);
	}
	
	public static boolean isMac(){
		return (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0);
	}
	
	public static String getPathSeparator(){
		if (isWindows())
			return "\\";
		else
			return "/";
	}
	
	//
	//	Returns approximate memory status
	//
	public static long u64_Memory( E_TecMEM _eType ) {
		long uResult = 0;
		
		if ( m_oRuntime == null ) return -1;

		switch ( _eType ) {
			case eMax	: uResult = m_oRuntime.maxMemory();
				break;
			case eTotal	: uResult = m_oRuntime.totalMemory();
				break;
			case eAvail	: uResult = m_oRuntime.totalMemory() - m_oRuntime.freeMemory();
				break;
			case eFree	: uResult = m_oRuntime.freeMemory();
				break;
		default:
			break;
		}
		return uResult;
	}
	
	//
	//	printMemory	= format & print approximate free memory
	//
	public static void printMemory( String _sMsg, long _u64Mem, String _sOther, long _u64Other ) {
		String sAlloc;

		if ( _u64Other >= 0 ) sAlloc = ", " + _sOther + "=" + s_FormatSize( _u64Other );
		else sAlloc = "";
		System.out.println( _sMsg + " memory=" + s_FormatSize( _u64Mem ) + sAlloc );
	}
	
	public static void printMemory( String _sMsg, long _u64Mem ) {
		printMemory( _sMsg, _u64Mem, "", -1 );
	}

}
