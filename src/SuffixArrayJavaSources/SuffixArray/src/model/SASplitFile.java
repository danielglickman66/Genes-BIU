package model;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import utils.Utils;

//
//	SASplitFile: save/load a compressed binary file of groups of SUFFIX (a Suffix Array Split)
//
public class SASplitFile {
	public static final String sC_SAFILE_EXT = ".saf";

	static final int iC_LONG_SIZE = Long.SIZE / 8, iC_INT_SIZE = Integer.SIZE / 8, iC_SHORT_SIZE = Short.SIZE / 8, iC_BYTE_SIZE = Byte.SIZE / 8;
	static final int iC_MIN_BUFF_SIZE = 1024, iC_MIN_REC_COUNT = 0x4000, iC_PROGNAME_SIZE = 24, iC_DEFLATEINFLATE_BUFF_SIZE = 0x4000, iC_PROGVERS_SIZE = 8;
	static final String sC_SPLIT_NAME = "SPLIT", sC_MARK = "BIO_", sC_ENTRY = "BIO.";

	public enum E_FileType {
		eINPUT, eOUTPUT;
	}

	enum E_ErrorCode {
		eOK, eTYPE, eOPEN, eCLOSE, eFLUSH_HEADER, eFLUSH_BUFFER, eINVALID, eREAD, eINFLATE, eLOAD;
	}
	
	private String m_sSASplitFullFilename;
	private static int m_iSplit, m_iSplitLen;
	
	private E_FileType m_eFileType;

	//	SASplit number of compressed chunks in one file
	private int m_iEntryCount;
	private int m_iCompressedSize;

	//	SASplit Buffers for Split save/load
	static private ArrayList<byte []> m_alSASplitFiles = new ArrayList<byte []>();

	static private byte [] m_acSASplitMemoryFile;
	
	//	SASplit Input file control
	private ByteArrayInputStream m_oSASplitInpStream;
	
	//	SASplit Output file control
	private ByteArrayOutputStream m_oSASplitOutStream;
	private ByteArrayOutputStream m_oArrayOutStream;
	private int m_iSAFileSize;

	private Inflater m_oInflater;
	
	//	Structure of the FILE HEADER:  
	private int m_iPrologueSize, m_iRecSize, m_iFlags, m_iRecords;
	private byte [] m_acCrLf;
	
	//	Various buffers and control variables 
	private byte [] m_acWorkBuffer, m_acHeader, m_acDeflateInflateBuffer, m_acReadBuffer;
	private int m_iBufferSize, m_iBuffPos, m_iDataTodo = 0;

	//	Temporary variable for long/int/short conversion to bytes 
	private int m_iIntBuffer = 0;

	//	Exception handling variables 
	private String m_sErrorMsg, m_sFilenameError;
	private E_ErrorCode m_eErrorCode;
	
	// TEST
	ByteBuffer m_oRecordBuffer;
	
	//
	//	constructor: SASplitFile
	//
	public SASplitFile(){
		m_sSASplitFullFilename = "";
		m_iSplit = 0;

		m_oSASplitInpStream = null;
		m_oSASplitOutStream = null;

		m_acHeader = new byte[ iC_MIN_BUFF_SIZE ];
		m_acDeflateInflateBuffer = new byte[ iC_DEFLATEINFLATE_BUFF_SIZE ];
		
		m_oInflater = new Inflater();

		m_iSAFileSize = 0;
		m_acWorkBuffer = null;
		m_iBuffPos = 0;
		m_iRecSize = 0;
		m_iFlags = 0;
		
		m_iEntryCount = 0;
		
		// Fixed CR-LF pair, only for the aesthetic of HEADER
		m_acCrLf = new byte[2];
		m_acCrLf[0] = '\r';
		m_acCrLf[1] = '\n';

		m_sErrorMsg = "";
		m_eErrorCode = E_ErrorCode.eOK;
	}
	
	//
	//	s_SaveFileName
	//
	public String s_SaveFileName() {
		return m_sSASplitFullFilename;
	}
	
	//
	//	i_Records: Return records count (loaded from SAFile)
	//
	public int i_Records() {
		return m_iRecords;
	}

	//
	//	i_SAFileSize: Return last save compressed file size
	//
	public int i_SAFileSize() {
		return m_iSAFileSize;
	}

	//
	//	i_SAFileCount: Return total compressed file count
	//
	public int i_SAFileCount() {
		if ( m_alSASplitFiles != null )
			return m_alSASplitFiles.size();
		else return 0;
	}

	//
	//	b_Init: Common for Input & Output
	//
	public boolean b_Init( String _sFolder, int _iSplit, E_FileType _eFileType, int _iSplitLen, boolean _bFromFile ){
		String sFileSx = "";
		
		m_iSplit = _iSplit;
		m_iSplitLen = _iSplitLen;

		if ( m_iSplit >= 0 )
			sFileSx = String.format( "%03d", m_iSplit );

		m_sSASplitFullFilename = Utils.s_FormFileName( _sFolder, sC_SPLIT_NAME + "_" + sFileSx, sC_SAFILE_EXT );
		m_sFilenameError = "file '" + m_sSASplitFullFilename + "' operation:";
		
		//	64-byte HEADER=MARK (4), PROGNAME (2+2+24), PROGVERS (2+2+8), RECORD.size (4). RECORDS.Count (4), SPARE-1 (4), SPARE-2 (4), Flags (2), CR-LF 
		//-!-		sC_MARK.length() +
		//-!-		iC_SHORT_SIZE + iC_SHORT_SIZE + iC_PROGNAME_SIZE +
		//-!-		iC_SHORT_SIZE + iC_SHORT_SIZE + iC_PROGVERS_SIZE +
		//-!-		iC_INT_SIZE + iC_INT_SIZE +
		//-!-		iC_INT_SIZE + iC_INT_SIZE +
		//-!-		iC_SHORT_SIZE + m_acCrLf.length;
		
		//	Memory for SUFFIX=Count, Line + coded-Len, Pos
		m_iRecSize = iC_INT_SIZE + iC_INT_SIZE + iC_SHORT_SIZE + iC_BYTE_SIZE;
		
		m_iBufferSize = iC_MIN_REC_COUNT*m_iRecSize;
		m_oRecordBuffer = ByteBuffer.allocate(m_iBufferSize);

		
		//	Memory for PROLOGUE=Entry MARK+Count, Size of compressed data
		m_iPrologueSize = sC_ENTRY.length() + String.format( "%04d", 0 ).length() + iC_INT_SIZE;

		m_eFileType = _eFileType;
		
		if ( m_eFileType == E_FileType.eINPUT ) {
			
			if ( _bFromFile ) {
				File oInpFile = new File( m_sSASplitFullFilename );
				m_acSASplitMemoryFile = new byte [ (int) oInpFile.length() ];
				try {
					RandomAccessFile oInpRandom = new RandomAccessFile( m_sSASplitFullFilename, "r" );
					oInpRandom.read( m_acSASplitMemoryFile );
					oInpRandom.close();
					} catch(Exception e) {
				    e.printStackTrace();
				}
				m_oSASplitInpStream = new ByteArrayInputStream( m_acSASplitMemoryFile );
			}
			else {
				if ( _iSplit >= m_alSASplitFiles.size() ) {
					m_eErrorCode = E_ErrorCode.eLOAD;
					m_sErrorMsg = m_sFilenameError + "load";
					return false;
				}
				
				if ( _iSplit < 0 )
					m_oSASplitInpStream = new ByteArrayInputStream( m_alSASplitFiles.get( 0 ) );
				else m_oSASplitInpStream = new ByteArrayInputStream( m_alSASplitFiles.get( _iSplit ) );
			}
		}
		else {
			m_oSASplitOutStream = new ByteArrayOutputStream();
			m_oArrayOutStream = new ByteArrayOutputStream( m_iBufferSize );
		}

		return true;
	}
	
	//
	//	b_PutInt: Put Integer into Save buffer
	//
	public boolean b_PutInt( int _iVal ) {

		if ( m_iBuffPos + iC_INT_SIZE > m_acWorkBuffer.length ) {
			if ( !b_FlushBuffer() )
				return false;
		}

		for ( int i = 0; i < iC_INT_SIZE; ++i ) {
			m_acWorkBuffer[m_iBuffPos++] = (byte)(_iVal & 0xFF);
			_iVal >>= 8;
	    }
		return true;
	}
	
	//
	//	b_PutShort: Put Short into Save buffer
	//
	public boolean b_PutShort( short _wVal ){

		if ( m_iBuffPos + iC_SHORT_SIZE > m_acWorkBuffer.length ) {
			if ( !b_FlushBuffer() )
				return false;
		}

		for ( int i = 0; i < iC_SHORT_SIZE; ++i ) {
			m_acWorkBuffer[m_iBuffPos++] = (byte)(_wVal & 0xFF);
			_wVal >>= 8;
		}
		return true;
	}

	//
	//	b_PutShort: Put Short (passed as Integer) into Save buffer
	//
	public boolean b_PutShort( int _iVal ){
		return b_PutShort( (short) _iVal );
	}

	//
	//	b_PutString: Put String.length + String into Save buffer
	//
	public boolean b_PutString( String _sVal, int _iMaxSize ) {
		int iLen = Math.min( _sVal.length(), _iMaxSize );

		if ( m_iBuffPos + iLen + iC_SHORT_SIZE > m_acWorkBuffer.length ) {
			if ( !b_FlushBuffer() )
				return false;
		}

		if ( !b_PutShort( iLen ) )
			return false;

		System.arraycopy( _sVal.getBytes(), 0, m_acWorkBuffer, m_iBuffPos, iLen );
		m_iBuffPos += iLen;
		return true;
	}
	
	//
	//	b_WriteHeader: Put & Write the HEADER structure
	//
	public boolean b_WriteHeader( String _sProgName, String _sProgVers, int _iRecords ){
		int iLen;
		
		m_acWorkBuffer = new byte[ iC_MIN_BUFF_SIZE ];
		m_iRecords = _iRecords;
		m_iDataTodo = m_iRecords*m_iRecSize;

		m_iBuffPos = 0;
		System.arraycopy( sC_MARK.getBytes(), 0, m_acWorkBuffer, m_iBuffPos, sC_MARK.length() );
		m_iBuffPos += sC_MARK.length();

		iLen = Math.min( _sProgName.length(), iC_PROGNAME_SIZE );
		b_PutShort( iLen );
		b_PutString( Utils.s_PadRight( _sProgName, iC_PROGNAME_SIZE ), iC_PROGNAME_SIZE );
		iLen = Math.min( _sProgVers.length(), iC_PROGNAME_SIZE );
		b_PutShort( iLen );
		b_PutString( Utils.s_PadRight( _sProgVers, iC_PROGVERS_SIZE ), iC_PROGVERS_SIZE );

		b_PutInt( m_iRecSize );
		b_PutInt( m_iRecords );

		//	Reserved
		b_PutInt( 0 );
		b_PutInt( 0 );

		b_PutShort( m_iFlags );

		System.arraycopy( m_acCrLf, 0, m_acWorkBuffer, m_iBuffPos, m_acCrLf.length );
		m_iBuffPos += m_acCrLf.length;
		
		if ( !b_FlushHeader() )
			return false;
		
		return true;
	}

	//
	//	b_ReadShort: Read Short from SA file
	//
	boolean b_ReadShort() {
		
		 m_oSASplitInpStream.read( m_acHeader, 0, iC_SHORT_SIZE );
		
		m_iIntBuffer = 0;
		for ( int i = iC_SHORT_SIZE - 1; i >= 0; --i ) {
			m_iIntBuffer = ( m_iIntBuffer << 8 ) + ( m_acHeader[i] & 0xFF );
	    }
		return true;
	}
	
	//
	//	b_ReadInt: Read Integer from SA file
	//
	boolean b_ReadInt() {

		m_oSASplitInpStream.read( m_acHeader, 0, iC_INT_SIZE );
			
		
		m_iIntBuffer = 0;
		for ( int i = iC_INT_SIZE - 1; i >= 0; --i ) {
			m_iIntBuffer = ( m_iIntBuffer << 8 ) + ( m_acHeader[i] & 0xFF );
	    }
		return true;
	}
	
	//
	//	b_ReadString: Read String.length then String from SA file
	//
	boolean b_ReadString() {
		if ( !b_ReadShort() )
			return false;
		m_oSASplitInpStream.read( m_acHeader, 0, m_iIntBuffer );
		return true;
	}
	
	//
	//	b_ReadHeader: Read HEADER of SA file
	//
	public boolean b_ReadHeader(){
		int iBufferSize;
		String sMark = null;
		byte [] acMark = new byte [ sC_MARK.length() ];
		
		m_oSASplitInpStream.read( acMark, 0, sC_MARK.length() );
			
		sMark = new String( acMark );
		if ( !sMark.equals( sC_MARK ) ) {
			m_eErrorCode = E_ErrorCode.eINVALID;
			m_sErrorMsg = m_sFilenameError + "invalid mark";
			return false;
		}

		//	Skip PROGRAM NAME
		if ( !b_ReadShort() )
			return false;
		if ( !b_ReadString() )
			return false;

		//	Skip PROGRAM VERS
		if ( !b_ReadShort() )
			return false;
		if ( !b_ReadString() )
			return false;

		//	Retrieve Record Size
		if ( !b_ReadInt() )
			return false;
		m_iRecSize = m_iIntBuffer;

		//	Retrieve Records count
		if ( !b_ReadInt() )
			return false;
		m_iRecords = m_iIntBuffer;

		//	Skip FLAGS
		if ( !b_ReadShort() )
			return false;

		//	Skip Reserved x 2
		if ( !b_ReadInt() )
			return false;
		if ( !b_ReadInt() )
			return false;

		//	Skip CR-LF
		m_oSASplitInpStream.read( m_acHeader, 0, 2 );
			
		m_iDataTodo = m_iRecords*m_iRecSize; 

		//	Allocate read buffer for data read and inflating it
		iBufferSize = (int) Math.max( iC_MIN_BUFF_SIZE, iC_MIN_REC_COUNT*m_iRecSize );
		m_acReadBuffer = new byte[ iBufferSize ];
		m_acWorkBuffer = new byte[ iBufferSize ];
		
		m_iBuffPos = 0;
		
		return true;
	}
	
	//
	//	b_PutEntry: Put in buffer a PROLOGUE identifying compressed file
	//
	public boolean b_PutEntry( int _iSize, int _iEntry ){
		String sEntry;
		int iBuffPos = 0;

		sEntry = String.format( sC_ENTRY + "%04d", _iEntry );
		System.arraycopy( sEntry.getBytes(), 0, m_acHeader, iBuffPos, sEntry.length() );
		iBuffPos += sEntry.length();

		for ( int i = 0; i < iC_INT_SIZE; ++i ) {
			m_acHeader[iBuffPos++] = (byte)( _iSize & 0xFF );
			_iSize >>= 8;
	    }

		return true;
	}
	
	//
	//	b_FlushBuffer: Write SAFile PROLOGUE and SUFFIX compressed data block to file
	//
	public boolean b_FlushBuffer() {
		int iByteCount, iCompressedSize;
		
		if ( m_oRecordBuffer.position() == 0 )
			return true;
		
		//
		// Prepare buffer
		//
		m_oArrayOutStream.reset();
		Deflater m_oDeflater = new Deflater();  
		m_oDeflater.setInput( m_oRecordBuffer.array(), 0, m_oRecordBuffer.position() );
		
		m_oDeflater.finish();  

		while ( !m_oDeflater.finished() ) {  
			iByteCount = m_oDeflater.deflate( m_acDeflateInflateBuffer );  
			m_oArrayOutStream.write( m_acDeflateInflateBuffer, 0, iByteCount );   
		}  
		try {
			//
			//	Write PROLOGUE
			//
			iCompressedSize = (int) m_oArrayOutStream.size(); 
			b_PutEntry( iCompressedSize, ++m_iEntryCount );

			m_oSASplitOutStream.write( m_acHeader, 0, m_iPrologueSize );
			
			//
			// Write buffer
			//
			m_oSASplitOutStream.write( m_oArrayOutStream.toByteArray() );

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		//
		// Back to start for next buffer
		//
		m_oRecordBuffer.clear();
		m_iBuffPos = 0;
		
		return true;
	}

	
	//
	//	b_FlushHeader: Write SAFile HEADER to file
	//
	public boolean b_FlushHeader() {
		m_oSASplitOutStream.write( m_acWorkBuffer, 0, m_iBuffPos );
		m_iBuffPos = 0;
		return true;
	}
	
	//
	//	s_ErrorMsg
	//
	public String s_ErrorMsg(){
		return "*** ERROR : " + m_sErrorMsg;
	}

	//
	//	i_ErrorCode
	//
	public int i_ErrorCode(){
		return m_eErrorCode.ordinal();
	}
	
	//
	//	b_Done
	//
	public boolean b_Done( boolean _bFinalize, boolean _bSaveToFile ){
		
		if ( m_eFileType == E_FileType.eOUTPUT ) {
			if ( !_bFinalize ) {
				// Output last bytes in buffer
				if ( !b_FlushBuffer() )
					return false;

				m_iSAFileSize = m_oSASplitOutStream.size();

				//
				//	Save compressed SASplitFile to be read by next steps (in file/memory)
				//
				if ( _bSaveToFile ) {
					try {
						m_oSASplitOutStream.writeTo( new FileOutputStream( m_sSASplitFullFilename ) );
						m_oSASplitOutStream.close();
						} catch ( IOException _oIOException ) {
						_oIOException.printStackTrace();
					}
				}
				else {
					m_acSASplitMemoryFile = new byte [ m_iSAFileSize ];
					System.arraycopy( m_oSASplitOutStream.toByteArray(), 0, m_acSASplitMemoryFile, 0, m_iSAFileSize );
					m_alSASplitFiles.add( m_acSASplitMemoryFile );
				}
			}
		}
		if ( m_eFileType == E_FileType.eINPUT ) {
			if ( !_bFinalize ) {
				try {
					m_oSASplitInpStream.close();
				} catch (IOException e) {
					e.printStackTrace();
					m_eErrorCode = E_ErrorCode.eCLOSE;
					m_sErrorMsg = m_sFilenameError + "close";
					return false;
				}
			}
			else {
				while ( m_alSASplitFiles.size() > 0 ){
					m_alSASplitFiles.remove(0);
				}
			}
		}
		
		return true;
	}

	//
	//	b_GetEntry: Get PROLOGUE identifying compressed file
	//
	public boolean b_GetEntry(){
		int iBuffPos;

		iBuffPos = m_iPrologueSize - iC_INT_SIZE;

		m_iCompressedSize = 0;
		for ( int i = iC_INT_SIZE - 1; i >= 0; --i ) {
			m_iCompressedSize = ( m_iCompressedSize << 8 ) + (int) ( m_acReadBuffer[iBuffPos+i] & 0xFF );
		}
		
		return true;
	}
	
	//
	//	b_ReadBuffer: Read a PROLOGUE, then a buffer into work area 
	//
	boolean b_ReadBuffer(){
		int iBytesRead, iDataRead, iDataTodo;
		
		//
		//	Read PROLOGUE
		//
		m_oSASplitInpStream.read( m_acReadBuffer, 0, m_iPrologueSize );
		b_GetEntry();

		
		//
		//	Inflate SUFFIX block in buffer
		//
		iDataRead = m_oSASplitInpStream.read( m_acReadBuffer, 0, m_iCompressedSize );

		if ( iDataRead != m_iCompressedSize ) {
			m_eErrorCode = E_ErrorCode.eINFLATE;
			m_sErrorMsg = m_sFilenameError + "inflate";
			return false;
		}
		
		m_oInflater.setInput( m_acReadBuffer, 0, iDataRead );
		m_oRecordBuffer.position(0);
		try {
			while ( !m_oInflater.finished() ) {
				iBytesRead = m_oInflater.inflate( m_acDeflateInflateBuffer );
				if ( iBytesRead <= 0 )
					break;
				iDataTodo = Math.min( m_iDataTodo, m_acDeflateInflateBuffer.length );
				m_iDataTodo -= iDataTodo;
				m_oRecordBuffer.put( m_acDeflateInflateBuffer, 0, iDataTodo );
				if ( m_iDataTodo <= 0 )
					break;
			}
			
			if ( m_oInflater.finished() ) {
				m_oInflater.reset();
			}
			
		} catch ( DataFormatException _oDataFormatException ) {
			_oDataFormatException.printStackTrace();
			m_eErrorCode = E_ErrorCode.eINFLATE;
			m_sErrorMsg = m_sFilenameError + "inflate";
			return false;
		}  
		
		m_oRecordBuffer.position(0);
		return true;
	}

	//
	//	o_ReadRecord
	//
	public Suffix o_ReadRecord(){
		int iLine, iPos, iLen, iCount;
	
		if ( m_oRecordBuffer.position() + m_iRecSize > m_iBufferSize ) {
			if ( !b_ReadBuffer() )
				return null;
		}
		
		iCount = m_oRecordBuffer.getInt();
		iLine = m_oRecordBuffer.getInt();
		iPos = m_oRecordBuffer.getShort();
		iLen = m_oRecordBuffer.get();
		
		return ( new Suffix( iLine, iPos, iLen, iCount ) );
	}
	
	//
	//	b_PutRecord: Put a SUFFIX into Save buffer
	//
	public boolean b_PutRecord( Suffix _oSuffix ){
		
		if ( m_oRecordBuffer.position() + m_iRecSize > m_iBufferSize ) {
			if ( !b_FlushBuffer() )
				return false;
		}
		
		m_oRecordBuffer.putInt(_oSuffix.m_iCount);
		m_oRecordBuffer.putInt(_oSuffix.m_iLine);
		m_oRecordBuffer.putShort( (short) _oSuffix.m_iPos );
		if ( _oSuffix.m_iLen < m_iSplitLen )
			m_oRecordBuffer.put( (byte) ( _oSuffix.m_iLen & 0xFF ) );
		else m_oRecordBuffer.put( (byte) 0 );
		
		return true;
	}
}
