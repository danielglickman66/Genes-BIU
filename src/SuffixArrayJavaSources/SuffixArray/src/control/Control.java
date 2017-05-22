package control;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import utils.Utils;
import utils.CmdLine;
import beans.Beans;
import view.View;
import model.Model;
import model.SASplitFile;
import model.SuffixArray;


/*
 * RUNNING on Windows
 * 1. Open CMD, type "G:", then "cd G:\...\FolderOfProjectWithJar"
 * 2. Type "java -jar ProjectNameVx.x.jar input_folder output_folder
 */

public class Control {
	final static String sC_PROG_NAME = "BioStats", sC_PROG_VERS = "6.2";

	//	Empirical value (target: a computer with 1GB memory available for Java JRE)
	//		for 65,000 lines (using an average count of 10):
	//														 600 length
	//				storage	( 65,000x600)					  40 MB
	//		per SPLIT (1/16 of data):
	//				hash	( 5,000 entries of (8+12+8))	 140 KB
	//				tables	( 5,000*1000 entries of pointer)  80 MB
	//				Suffix nodes (5,000,000x[16+pointer])	 160 MB
	//		all SPLITs
	//				Saving Splits (compression about 50%)	 180 MB
	//														====
	//							TOTAL						 500 MB
	final static int iC_SPLIT_MIN_LINES = 0x400, iC_SPLIT_COUNT_FOR_SINGLES = 16, iC_ALPHA_SIZE_FOR_PAIRS = 5;
	final static String sC_WORK = "work";
	
	static CmdLine m_oCmdLine;
	static int m_iTop, m_iSort, m_iSplit, m_iMaxLen;
	static String m_sInputFolder, m_sOutputFolder, m_sWorkFolder, m_sFileSize;
	static boolean m_bLoadSAFiles;
	
	static int m_iFreeParamsCount, m_iMustParamsCount;

	//	Debug helpers
	static boolean m_bDebug, m_bProgress;
	static long m_i64TotSaveTime = 0, m_u64TotBuffersSize = 0;
	
	public Control(){
		m_oCmdLine =  new CmdLine();
	}

	//
	//	init: Set parameters for command-line parsing and parse it
	//
	public boolean init( String[] _CmdLineArgs ){
		//
		//	We require only Input folder to be passed. All other have their default values.
		//
		m_iFreeParamsCount = 2;
		m_iMustParamsCount = 1;
		
		if (!parseCmdLine( _CmdLineArgs )){
			return false;
		}
		return true;
	}
	
	//
	//	parseCmdLine: Define user parameters (with defaults) and check command-line for any selected value
	//
	public static boolean parseCmdLine(String[] _asArgs){
		boolean bDone;
		
		m_oCmdLine.init( m_iFreeParamsCount, m_iMustParamsCount, _asArgs);
		m_sInputFolder = m_oCmdLine.optFree(1);
		m_sOutputFolder = m_oCmdLine.optFree(2);
		
		m_bDebug = m_oCmdLine.optAppears("-debug", "debug mode", -1, false);
		m_sFileSize = m_oCmdLine.optAsString("-file", "choose run files (=all|s|m|l)", -1);
		m_bLoadSAFiles = m_oCmdLine.optAppears("-load", "use previously saved file(s)", -1, false);
		m_iMaxLen = m_oCmdLine.optAsInt("-maxlen", "set max. length of infix accounted", -1, -1 );
		m_bProgress = m_oCmdLine.optAppears("-progress", "progress feedback", -1, false);
		m_iTop = m_oCmdLine.optAsInt("-top", "set number of top entires in results table", -1, 50);
		m_iSort = m_oCmdLine.optAsInt("-sort", "sorting code(=1:std score, 2:count)", -1, 1);
		m_iSplit = m_oCmdLine.optAsInt("-split", "[0=auto] length of split on suffix (for huge number of lines)" + " ", -1, 0);
		
		bDone = m_oCmdLine.done(sC_PROG_NAME, sC_PROG_VERS, "input-folder [output-folder]");
		
		if (m_sFileSize.isEmpty())
			m_sFileSize = "A";
		else
			m_sFileSize = m_sFileSize.substring(0, 1);
		
		if ( m_iSplit > SuffixArray.iC_MAX_SPLIT ) {
			System.out.println("*** ERROR: Split '" + m_iSplit + "' is greater than maximum allowed (" + SuffixArray.iC_MAX_SPLIT + ")");
			return false;
		}
		
		if (m_sInputFolder.isEmpty())
			return false;
		
		if (m_sOutputFolder.isEmpty()){
			m_sOutputFolder = Utils.s_filePathOf(m_sInputFolder)+Utils.getPathSeparator()+"Output";
		}
		if (!Utils.b_MakeFolder(m_sOutputFolder)){
			System.out.println("*** ERROR: cannot create folder '" + m_sOutputFolder + "'");
			return false;
		}
		m_sWorkFolder = m_sOutputFolder + Utils.getPathSeparator() + sC_WORK;
		if (!Utils.b_MakeFolder( m_sWorkFolder )){
			System.out.println("*** ERROR: cannot create folder '" + m_sWorkFolder + "'");
			return false;
		}
		
		return bDone;
	}
	
	//
	//	EchoParameters: show selections (and defaults) on screen (and LOG)
	//
	static void EchoParameters() {
		String sPrompt;
		Utils.debugLogAndPrintLn( "Running on input folder '" + m_sInputFolder + "'" );
		sPrompt = "for parameters: top=" + m_iTop +
				" maxlen=" + m_iMaxLen +
				" sort=" + m_iSort + " (" + s_sortCode(m_iSort) + ")" +
				" split=" + m_iSplit +
				" file=" + m_sFileSize;
		if ( m_bDebug )
			sPrompt = sPrompt + " mode=debug";
		Utils.debugLogAndPrintLn( sPrompt );
		Utils.debugLogAndPrintLn( "top results at '"+m_sOutputFolder+Utils.getPathSeparator()+"output.txt'" );
		Utils.debugLogAndPrintLn( String.format("%064d", 0).replace( "0", "-" ) );
	}

	//
	//	LogStats: Log some summaries stats for analysis
	//
	static void LogStats( SuffixArray _oArray ) {
		if (!m_bDebug)
			return;

		Utils.debugLogLn( String.format( "Split max. nodes  =%,15d", _oArray.m_u64SplitMaxNodes ) );
		Utils.debugLogLn( String.format( "Split max.prefixes=%,15d", _oArray.m_u64SplitMaxPrefixCount ) );
		Utils.debugLogLn( String.format( "Split max.suffixes=%,15d", _oArray.m_u64SplitMaxSuffixCount ) );
		Utils.debugLogLn( String.format( "Split max.infixes =%,15d", _oArray.m_u64SplitMaxInfixCount ) );
		Utils.debugLogLn( String.format( "total infixes     =%,15d", _oArray.m_u64TotInfixCount ) );
	}
	
	//
	//	e_sortCode: translate int parameter for Sort code into enumeration
	//
	public static Beans.E_SortCode e_sortCode(int _iCode){
		switch(_iCode){
		case 0:
			return Beans.E_SortCode.eSTRING;
		case 1:
			return Beans.E_SortCode.eSCORE;
		default: return Beans.E_SortCode.eCOUNT;
		}
	}
	
	//
	//	s_sortCode: translate int parameter for Sort code into name
	//
	public static String s_sortCode(int _iCode){
		switch(_iCode){
		case 0:
			return Beans.E_SortCode.eSTRING.toString().substring(1);
		case 1:
			return Beans.E_SortCode.eSCORE.toString().substring(1);
		default: return Beans.E_SortCode.eCOUNT.toString().substring(1);
		}
	}
	
	//
	//	processData: run splits of Build-Suffix-Array, compute Average, compute STD.DEV, compute Score & save Top
	//
	public static void processData( SuffixArray _oArray, boolean _bXml ){
		int iSplitCount = 0, iSplitSt;
		boolean bMatchAnyOfSplit = false;
		String sSplitFormatted, sProgressHeader;
		long u64TimeSt, u64RefTime, u64SaveTime, u64BuildTime;
		char cSep = ( m_bProgress ) ? '-' : ' ';
		
		//
		//	Evaluate number of splits necessary (but if user require some value)
		//
		bMatchAnyOfSplit = ( _oArray.getAplhabet().length() > iC_ALPHA_SIZE_FOR_PAIRS );
		if ( _oArray.getAplhabet().length() <= iC_ALPHA_SIZE_FOR_PAIRS ) {
			_oArray.b_BuildSplitList( 2 );
			iSplitCount = _oArray.getSplitsSize(); 
		}
		else {
			_oArray.b_BuildSplitList( 1 );
			iSplitCount = iC_SPLIT_COUNT_FOR_SINGLES; 
		}
		
		if ( ( m_iSplit == 0 ) && ( _oArray.i_TotalStoredLines() / iC_SPLIT_MIN_LINES <= 0 ) ){
			iSplitCount = 0;
		}
		else {
			iSplitCount = _oArray.getSplitsSize();
		}
		Utils.debugLogAndPrintLn( ">>> Split count=" + iSplitCount, '-' );

		//
		//	Build Suffix-Array, and store SPLITs
		//
		if ( m_bProgress ) 
			Utils.debugLogAndPrintLn( "... Building" );
		u64TimeSt = System.currentTimeMillis();
		_oArray.prepareStats();
		Utils.i_CleanExtension( m_sWorkFolder, SASplitFile.sC_SAFILE_EXT );

		_oArray.resetStats();
		
		iSplitSt = ( bMatchAnyOfSplit ) ? 1 : 0;
		if ( iSplitCount > 0 ) {
			for ( int iSplit = iSplitSt; iSplit <= iSplitCount; iSplit++ ) {
				sSplitFormatted = String.format( "%2d", iSplit ) + " of " + String.format( "%2d", iSplitCount );
				sProgressHeader = " Build: Suffix Array[" + sSplitFormatted + "]= ";

				u64RefTime = System.currentTimeMillis();

				//
				//	Clean previous Split data
				//
				_oArray.clearSplit();

				if ( m_bProgress ) 
					Utils.debugLogAndPrintLn( "Build, Split: " + sSplitFormatted );
				
				//
				//	Build a new Suffix Array - and save it
				//
				if ( _oArray.b_BuildSuffixArray( iSplit, bMatchAnyOfSplit ) ){

					// hash to sorted list (create virtual large sorted table)
					_oArray.hashToList( iSplit, true, false );

					u64SaveTime = _oArray.u64_SaveTime();
					m_i64TotSaveTime += u64SaveTime;
					u64BuildTime = Utils.u64_Elapsed( u64RefTime ) - u64SaveTime; 
					
					Utils.debugLogAndPrintLn( String.format( ">>>" + sProgressHeader + "%s",
						Utils.s_StampTime( u64BuildTime, "built in" ) ) );
					m_u64TotBuffersSize += _oArray.u64_SAFileSize();
					Utils.debugLogAndPrintLn( "... Saved [" +
						Utils.s_fileFNameOf( _oArray.s_SaveFileName( false ) ) +
						", size=" + Utils.s_FormatSize( _oArray.u64_SAFileSize() ) +
						"], total saved=" + Utils.s_FormatSize( m_u64TotBuffersSize ) );
				}
				else {
					Utils.debugLogAndPrintLn( _oArray.getError() );
					return;
				}
				Utils.debugLogAndPrintLn( String.format( "... SA Build [" + String.format( "%2d", iSplit ) + "]: " + "%s, %s",
					Utils.s_StampTime( "computed in", false ), Utils.s_StampElapsed( u64TimeSt, "Elapsed for SA build" ) ), cSep );
			}
		}
		else {
			if ( _oArray.b_BuildSuffixArray( -1, false ) ){

				// hash to sorted list (create virtual large sorted table)
				_oArray.hashToList( -1, true, false );
				m_i64TotSaveTime += _oArray.u64_SaveTime(); 
				Utils.debugLogAndPrintLn( String.format( ">>> Suffix Array: %s %s",
					Utils.s_StampTime( "computed in", false ), Utils.s_StampTime( "total elapsed", true ) ) );
			}
			else {
				Utils.debugLogAndPrintLn( _oArray.getError() );
				return;
			}
		}
		Utils.debugLogAndPrintLn( ">>> " + Utils.s_StampTime( m_i64TotSaveTime, "Total saving time" ) );
		
		Utils.debugLogAndPrintLn( String.format( ">>> BUILD     : %s %s",
			Utils.s_StampElapsed( u64TimeSt, "done in" ), Utils.s_StampTime( "total elapsed", true ) ), '-' );
		
		Utils.debugDebugFlush();
		
		//
		//	Compute AVERAGE
		//
		if ( m_bProgress ) 
			Utils.debugLogAndPrintLn( "... AVERAGE: computing" );
		u64TimeSt = System.currentTimeMillis();
		_oArray.resetStats();

		if ( iSplitCount > 0 ) {
			for ( int iSplit = iSplitSt; iSplit <= iSplitCount; iSplit++ ) {
				sSplitFormatted = String.format( "%2d", iSplit ) + " of " + String.format( "%2d", iSplitCount );
				sProgressHeader = " AVERAGE: Suffix Array[" + sSplitFormatted + "]= ";

				//
				//	Clean previous Split data
				//
				_oArray.clearSplit();

				if ( m_bProgress )  
					Utils.debugLogAndPrintLn( "AVERAGE, Split: " + sSplitFormatted );
				
				//
				//	Load saved Suffix Array
				//
				if ( _oArray.b_LoadSuffixArray( iSplit, true, false ) ){
					Utils.debugLogAndPrintLn( String.format( ">>>" + sProgressHeader + "%s",
						Utils.s_StampTime( "loaded in", false ) ) );
				}
				else {
					Utils.debugLogAndPrintLn( _oArray.getError() );
					return;
				}

				_oArray.scanTotalAndDiff( iSplit );

				Utils.debugLogAndPrintLn( String.format( "... AVERAGE [" + String.format( "%2d", iSplit ) + "]: " + "%s, %s",
						Utils.s_StampTime( "computed in", false ), Utils.s_StampElapsed( u64TimeSt, "Elapsed for AVERAGE" ) ), cSep );
			}
		}
		else {
			_oArray.scanTotalAndDiff( -1 );
		}
		_oArray.calcAvgPerLength();
		Utils.debugLogAndPrintLn( String.format( ">>> AVERAGE     : %s %s",
			Utils.s_StampElapsed( u64TimeSt, "computed in" ), Utils.s_StampTime( "total elapsed", true ) ), '-' );

		//
		//	Compute STD.DEV
		//
		if ( m_bProgress ) 
			Utils.debugLogAndPrintLn( "... STD.DEV: computing" );
		u64TimeSt = System.currentTimeMillis();
		_oArray.resetStats();

		if ( iSplitCount > 0 ) {
			for ( int iSplit = iSplitSt; iSplit <= iSplitCount; iSplit++ ) {
				sSplitFormatted = String.format( "%2d", iSplit ) + " of " + String.format( "%2d", iSplitCount );
				sProgressHeader = " STD.DEV: Suffix Array[" + sSplitFormatted + "]= ";

				//
				//	Clean previous Split data
				//
				_oArray.clearSplit();

				if ( m_bProgress )  
					Utils.debugLogAndPrintLn( "STD.DEV, Split: " + sSplitFormatted );
				
				//
				//	Load saved Suffix Array
				//
				if ( _oArray.b_LoadSuffixArray( iSplit, true, false ) ){
					Utils.debugLogAndPrintLn( String.format( ">>>" + sProgressHeader + "%s",
						Utils.s_StampTime( "loaded in", false ) ) );
				}
				else {
					Utils.debugLogAndPrintLn( _oArray.getError() );
					return;
				}

				_oArray.scanStdDev( iSplit );

				Utils.debugLogAndPrintLn( String.format( "... STD.DEV [" + String.format( "%2d", iSplit ) + "]: " + "%s, %s",
						Utils.s_StampTime( "computed in", false ), Utils.s_StampElapsed( u64TimeSt, "Elapsed for STD.DEV" ) ), cSep );
			}
			_oArray.calcStdDev();
		}
		else {
			_oArray.scanStdDev( -1 );
			_oArray.calcStdDev();
		}
		Utils.debugLogAndPrintLn( String.format( ">>> STD.DEV     : %s %s",
			Utils.s_StampElapsed( u64TimeSt, "computed in" ), Utils.s_StampTime( "total elapsed", true ) ), '-' );

		//
		//	SCORE
		//
		if ( m_bProgress ) 
			Utils.debugLogAndPrintLn( "... Score: computing" );
		_oArray.resetStats();
		u64TimeSt = System.currentTimeMillis();
		if ( iSplitCount > 0 ) {
			for ( int iSplit = iSplitSt; iSplit <= iSplitCount; iSplit++ ) {
				sSplitFormatted = String.format( "%2d", iSplit ) + " of " + String.format( "%2d", iSplitCount );
				sProgressHeader = " Score: Suffix Array[" + sSplitFormatted + "]= ";

				//
				//	Clean previous Split data
				//
				_oArray.clearSplit();

				if ( m_bProgress ) 
					Utils.debugLogAndPrintLn( "... Score, split: " + sSplitFormatted );
				
				//
				//	Load saved Suffix Array
				//
				if ( _oArray.b_LoadSuffixArray( iSplit, true, false ) ){
					Utils.debugLogAndPrintLn( String.format( ">>>" + sProgressHeader + "%s",
						Utils.s_StampTime( "loaded in", false ) ) );
				}
				else {
					Utils.debugLogAndPrintLn( _oArray.getError() );
					return;
				}

				_oArray.scanStdScore( iSplit );

				Utils.debugLogAndPrintLn( String.format( "... Score [" + String.format( "%2d", iSplit ) + "]: " + "%s, %s",
					Utils.s_StampTime( "computed in", false ), Utils.s_StampElapsed( u64TimeSt, "Elapsed for SCORE" ) ), cSep );
			}
		}
		else {
			_oArray.scanStdScore( -1 );
		}
		Utils.debugLogAndPrintLn( String.format( ">>> Score       : %s %s",
			Utils.s_StampElapsed( u64TimeSt, "computed in" ), Utils.s_StampTime( "total elapsed", true ) ), '-' );
		
		_oArray.printTop();
		if ( m_bProgress ) 
			Utils.debugLogAndPrintLn( "... Top list saved!", '=' );
		
		_oArray.b_AllDone( _bXml );
	}

	//
	//	b_ReadAndStore : Read the file(s) and store them as lines of bytes (UTF-8 characters) in memory 
	//
	static boolean b_ReadAndStore( Model _oModel, boolean _bAllFiles, SuffixArray _oArray ) {
		File oFolder = new File( m_sInputFolder ), oInputFile;
		FileInputStream oInputStream = null;
		File[] sArrFiles = oFolder.listFiles();
		
		String sFileName, sExt;
		Beans.FileRef oFileRef;
		int iFile = 0, iIxSizeS = 0, iIxSizeM = 0, iIxSizeL = 0;
		long u64FileSize, u64LinesRead = 0;


		// File size measurements 
		List<Utils.PairLongInt> slFileSizeIx = new ArrayList<Utils.PairLongInt>();

		
		//
		//	Select run file(s)
		//
		if (!_bAllFiles){
			for ( iFile = 0; iFile < sArrFiles.length; iFile++ ) {
				oInputFile = sArrFiles[iFile];
				sExt = Utils.s_fileExtensionOf( oInputFile.getName() ).toLowerCase(); 
				if ( !sExt.equals( ".txt" ) )
					continue;
				u64FileSize = oInputFile.length();
				slFileSizeIx.add(new Utils.PairLongInt(u64FileSize, iFile));
			}
			if ( slFileSizeIx.size() == 0 ) {
				Utils.debugLogAndPrintLn( "*** ERROR: No input files!" );
				return false;
			}
			Collections.sort(slFileSizeIx);
			
			iIxSizeS = slFileSizeIx.get(0).m_iVal;
			iIxSizeM = slFileSizeIx.get( slFileSizeIx.size()/2 ).m_iVal;
			iIxSizeL = slFileSizeIx.get( slFileSizeIx.size() - 1 ).m_iVal;
		}
		
		//
		//	Read and store data
		//
		for ( iFile = 0; iFile < sArrFiles.length; iFile++ ) {
			if (!_bAllFiles){
				switch(m_sFileSize){
				case "S":
					if (iIxSizeS != iFile)
						continue;
					break;
				case "M":
					if (iIxSizeM != iFile)
						continue;
					break;
				default:
					if (iIxSizeL != iFile)
						continue;
				}
			}

			oInputFile = sArrFiles[iFile];
			if (oInputFile.isDirectory())
				continue;
			sFileName = oInputFile.toString();
			
			Utils.debugLogAndPrintLn( "Running file '"+sFileName+"'" );
			
			try {
				oInputStream = new FileInputStream(oInputFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			_oArray.prepFile();
			while(true) {
				oFileRef = _oModel.readLine( oInputStream );

				if ( _oModel.m_bEof )
					break;
				if ( oFileRef.m_sLine.isEmpty() )
					continue;
				else{
					u64LinesRead++;
					if ( oFileRef.m_sLine.length() > SuffixArray.iC_MAX_SUFFIX_LEN ) {
						Utils.debugLogAndPrintLn( "*** ERROR: line[" + u64LinesRead + "] length (" + oFileRef.m_sLine.length() + ")" +
							"in '" + sFileName + "' exceeds program limit (" + SuffixArray.iC_MAX_SUFFIX_LEN + ")"  );
						return false;
					}
					if ( !_oArray.b_StoreLine( oFileRef.m_sLine, u64LinesRead, iFile, oFileRef.m_u64Pos ) ){
						Utils.debugLogAndPrintLn( "*** ERROR: invalid file '"+sFileName+"'" );
						return false;
					}
				}
			}
			
			Utils.debugLogAndPrintLn( ">>> " + Utils.s_fileFileNameOf(sFileName) +
				": stored lines=" + _oArray.i_FileStoredLines() + " (CRC=" + Utils.s_FormatCrc(_oArray.u64_FileCrc() ) + ")", '-' );
		}
		return true;
	}

	//
	//	LoadAndStore : TODO 
	//
	static void LoadAndStore( Model _oModel ) {
		Utils.debugLogAndPrintLn( "TODO: Load saved files as SA splits" );
	}


	//
	//	main 
	//
	public static void main( String[] args ) {
		// MVC
		Control oControl = new Control();
		Model oModel = new Model();
		View oView = new View();
		
		// Suffix Array (SA) object 
		SuffixArray oArray = new SuffixArray();
		SuffixArray.init();
		
		// Initialize Utils and MVC 
		Utils.init();
		oView.init();
		if ( !oControl.init( args ) )
			return;
		
		Utils.setParams( m_bDebug, m_bProgress );
		//
		//	Initialize LOG and log some basic references
		//
		Utils.debugLogInit( Utils.s_filePathOf( m_sInputFolder ), sC_PROG_NAME, true );
		Utils.debugLogAndPrintLn( ">>> Running=" + sC_PROG_NAME + " " + sC_PROG_VERS +
			" (available memory=" + Utils.s_FormatSize( Utils.u64_Memory( Utils.E_TecMEM.eMax ) ) + ")" );
		Utils.debugLogLn( "" );

		EchoParameters();
		Utils.debugLogFlush();

		if ( m_iMaxLen <= 0 )
			m_iMaxLen = SuffixArray.iC_MAX_SUFFIX_LEN - 1;

		oArray.setParams( new Beans.Params( sC_PROG_NAME, sC_PROG_VERS,
			m_sInputFolder, m_sOutputFolder, m_sWorkFolder, 
			m_iTop, m_iMaxLen, m_iSplit, e_sortCode(m_iSort), m_bDebug ) );
		
		//
		//	Load previous database or read it from file(s) and store in in memory
		//
		if( m_bLoadSAFiles )
			LoadAndStore(  oModel  );
		else {
			if ( !b_ReadAndStore( oModel, m_sFileSize.equals("A"), oArray ) )
				return;
		}
		
		Utils.debugLogAndPrintLn( ">>> Total stored lines=" + oArray.i_TotalStoredLines() +
			" (" + String.format( "%s", Utils.s_StampTime( "storage time", false ) ) + ")", '-' );

		//
		//	Do the real work: Average, STD.DEV, Score & Top
		//
		processData( oArray, false );

		//
		//	Save some debug stats
		//
		LogStats( oArray );
		
		//
		//	THE END!
		//
		Utils.debugLogAndPrintLn( String.format( "!!! Total %s", Utils.s_StampTime( "run time", true ) ) );

		System.out.println("FIN");
		Utils.debugLogdone();
		return;
	}
}
