package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import utils.Utils;

public class SAXmlFile {
	static final String sC_SPLIT_NAME = "SPLIT";

	public enum E_XmlFileType {
		eINPUT, eOUTPUT;
	}

	enum E_ErrorCode {
		eOK, eOPEN, eCLOSE, eREAD, eINVALIDTAG;
	}
	
	private PrintWriter m_oWriter;
	private BufferedReader m_oReader;
	
	private String m_sXmlName;
	private int m_iSplit;
	private String m_sLine, m_sData, m_sType, m_sTag, m_sLinePx;
	private String m_sErrorMsg;
	private E_ErrorCode m_eErrorCode;
	ArrayList<String> m_alsTags;

	public SAXmlFile(){
		m_oWriter = null;
		m_sXmlName = "";
		m_iSplit = 0;

		m_sType = "";
		
		m_alsTags = new ArrayList<String>();

		m_sLine = "";
		m_sErrorMsg = "";
		m_eErrorCode = E_ErrorCode.eOK;
	}
	
	//
	//	s_SaveFileName
	//
	public String s_SaveFileName() {
		return m_sXmlName;
	}
	
	//
	//	b_Init
	//
	public boolean b_Init( String _sFolder, int _iSplit, E_XmlFileType _eFileType, String _sType ){
		String sFileSx = "";
		
		m_iSplit = _iSplit;

		if ( m_iSplit >= 0 )
			sFileSx = String.format( "%03d", m_iSplit );

		m_sXmlName = Utils.s_FormFileName( _sFolder, sC_SPLIT_NAME + "_" + sFileSx, ".bin" );
		m_sType = "UTF-" + _sType;

		if ( _eFileType == E_XmlFileType.eOUTPUT ) {
			try{
				m_oWriter = new PrintWriter( m_sXmlName, m_sType );
			} catch (IOException e) {
				m_eErrorCode = E_ErrorCode.eOPEN;
				m_sErrorMsg = "unable to open file '" + m_sXmlName + "'";
				return false;
			}
		}
		else {
			try{
				m_oReader = new BufferedReader( new FileReader( m_sXmlName ) );
			} catch (IOException e) {
				m_eErrorCode = E_ErrorCode.eOPEN;
				m_sErrorMsg = "unable to open file '" + m_sXmlName + "'";
				return false;
			}
		}
		return true;
	}
	
	//
	//	b_WriteLine
	//
	boolean b_WriteLine( String _sLine ){
		m_oWriter.write( _sLine );
		return true;
	}
	
	//
	//	b_WriteBuffered
	//
	boolean b_WriteBuffered(){
		boolean bRc = b_WriteLine( m_sLine );
		m_sLine = "";
		return bRc;
	}
	
	//
	//	b_WriteHeader
	//
	public boolean b_WriteHeader(){
		return b_WriteLine( "<?xml  version=\"1.0\" encoding=\""+ m_sType + "\"?>\r\n" );
	}
	
	//
	//	b_ReadLine
	//
	public boolean b_ReadLine(){
		try {
			m_sLine = m_oReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			m_eErrorCode = E_ErrorCode.eREAD;
			m_sErrorMsg = "unable to read from file '" + m_sXmlName + "'";
			return false;
		}
		return true;
	}

	//
	//	b_ReadHeader
	//
	public boolean b_ReadHeader(){
		return b_ReadLine();
	}

	//
	//	b_WriteComment
	//
	public boolean b_WriteComment( String _sComment ) {
		return b_WriteLine( "<!-- " + _sComment + " " + m_sXmlName + "-->\r\n" );
	}
	
	//
	//	s_ReadComment
	//
	public String s_ReadComment() {
		if ( b_ReadLine() )
			return m_sLine;
		else return null;
	}
	
	//
	//	b_TagOpen
	//
	public boolean b_TagOpen( String _sTag ) {
		String sTag = _sTag.toUpperCase();
		
		if ( m_alsTags.size() > 0 )
			m_sLinePx = String.format( "%0" + m_alsTags.size() + "d", 0).replace( '0', '\t' );
		else m_sLinePx = "";
		m_sLine += m_sLinePx + "<" + sTag + ">";
		m_alsTags.add( 0, sTag );
		return true;
	}
	
	//
	//	b_TagDataAsString
	//
	public boolean b_TagDataAsString( String _sData ) {
		m_sLine += _sData;
		return true;
	}
	
	//
	//	b_TagAndString
	//
	public boolean b_TagAndString( String _sTag, String _sData ) {
		String sTag = _sTag.toUpperCase();

		if ( m_alsTags.size() > 0 )
			m_sLinePx = String.format( "%0" + m_alsTags.size() + "d", 0).replace( '0', '\t' );
		else m_sLinePx = "";
		m_sLine += "<" + sTag + ">" + _sData + "</" + sTag + ">\r\n";
		return b_WriteBuffered();
	}
	
	//
	//	b_TagClose
	//
	public boolean b_TagClose() {
		boolean bRc;
		if ( m_alsTags.size() == 0 ) {
			m_eErrorCode = E_ErrorCode.eINVALIDTAG;
			m_sErrorMsg = "no TAG open for '" + m_sXmlName + "'";
			return false;
		}
		m_sTag = m_alsTags.get( 0 );
		m_sLine += "</" + m_sTag + ">\r\n";
		bRc = b_WriteBuffered();
		m_alsTags.remove(0);
		return bRc;
	}
	
	//
	//	b_ParseUntil
	//
	public boolean b_ParseUntil( String _sPath ) {
		int iTag = 0, iPosSt, iPosSp;
		boolean bFound = false;
		String [] asPathParts = _sPath.split("/");
		String sTagUser, sPart, sTagLine;

		if ( asPathParts.length == 0 )
			return false;
		sTagUser = asPathParts[iTag].toUpperCase();
		while ( !bFound ) {
			if ( !b_ReadLine() )
				return false;
			iPosSt = m_sLine.indexOf( '<' );
			if ( iPosSt < 0 )
				continue;
			iPosSt = m_sLine.indexOf( '<' );
			sPart = m_sLine.substring( iPosSt );
			if ( sPart.length() == 0 )
				continue;
			iPosSp = sPart.indexOf( '>' );
			if ( iPosSp < 0 )
				continue;
			sTagLine = sPart.substring( 0, iPosSp );
			
			if ( !sTagLine.equals( sTagUser ) ) {
				iTag++;
				if ( iTag >= asPathParts.length ) {
					bFound = true;
					iPosSt = sPart.indexOf( '<' );
					if ( iPosSp < 0 ) {
						m_sData = "";
					}
					else {
						m_sData = sPart.substring( iPosSp + 1, iPosSt );
					}
					break;
				}
				sTagUser = asPathParts[iTag].toUpperCase();
			}
		}
		return bFound;
	}
	
	//
	//	s_Data
	//
	public String s_Data()	{
		return m_sData;
	}
	
	//
	//	i_Data
	//
	public int i_Data()	{
		return Utils.i_safeStringToInt( m_sData );
	}
	
	//
	//	s_ErrorMsg
	//
	public String s_ErrorMsg(){
		return "***ERROR: " + m_sErrorMsg;
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
	public boolean b_Done(){
		if ( m_oWriter != null )
			m_oWriter.close();
		
		if ( m_oReader != null ) {
			try {
				m_oReader.close();
			} catch (IOException e) {
				e.printStackTrace();
				m_eErrorCode = E_ErrorCode.eCLOSE;
				m_sErrorMsg = "unable to close file '" + m_sXmlName + "'";
				return false;
			}
		}
		return true;
	}
}
