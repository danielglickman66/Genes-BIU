package model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beans.Beans;

public class Model {
	public boolean m_bEof = false;
	private final int iC_BUFFER_SIZE = 16*1024;
	private final int iC_BOM_0 = -1, iC_BOM_1 = -2;
	
	private static byte[] m_abBuffer;
	private int m_iBuffPos, m_iBuffLength, m_iCharSize;
	private long m_u64FilePos;
	private String m_sLineStart;
	private boolean m_bNeedBuffer, m_bInit, m_bUnicode;
	
	public Model(){
		m_abBuffer = new byte[iC_BUFFER_SIZE];
		m_iBuffPos = 0;
		m_iBuffLength = 0;
		m_iCharSize = 0;
		m_u64FilePos = 0;
		m_sLineStart = "";
		m_bNeedBuffer = true;
		m_bInit = false;
		m_bUnicode = false;
	}
	
	//
	//	Short files - read all into buffer and split in lines
	//
	public List<String> readFile(String _sFilename) {
		// standard word separators
		String sSeps = "\\t|\\s|\\.|,|\\?|\\!|;|:|/|\\\\|\\$|\\[|\\]|\\(|\\)|=";
		// ", types of ", ... and -
		sSeps = sSeps + "\\x93|\\x94|\\x82|\\x84|\\u201c|\\u201d|\\u2026|\\u0022|\\x2d";
		List<String> slRecords = new ArrayList<String>();
		try {
			FileReader oFileReader = new FileReader(_sFilename);
			BufferedReader oBufferedReader = new BufferedReader(oFileReader);
			String sLine; 
			while ((sLine = oBufferedReader.readLine()) != null) {
				String[] sArrParts = sLine.split(sSeps);
				for (int i=0; i< sArrParts.length; i++) {
					if (sArrParts[i].length() > 0) {
						slRecords.add(sArrParts[i].toLowerCase());
					}
				}
			}
			oBufferedReader.close();
			return slRecords;
		} catch (Exception _e) {
			System.err.format("Exception occurred trying to read '%s'.", _sFilename);
			_e.printStackTrace();
			return null;
		}
	}
	
	private int readBuffer(FileInputStream _oInputStream){
		int iReadBytes = 0;
		
		// Check EOF
		try {
			if (_oInputStream.getChannel().position() >= _oInputStream.getChannel().size()){
				return -1;
			}
			iReadBytes = _oInputStream.read(m_abBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return iReadBytes;
	}
	
	//
	//	Huge files - read by buffers, and split into lines on-the-fly
	//
	public Beans.FileRef readLine( FileInputStream _oInputStream ) {
		Beans.FileRef oLineRef;
		byte[] abSlice;
		int iPos, iEnd = 0;
		boolean bEndOfBuff;
		String sSlice = "";
		oLineRef = new Beans.FileRef("", 0);

		while(true){
			if (m_bNeedBuffer){
				m_bNeedBuffer = false;
				m_u64FilePos += m_iBuffLength;
				m_iBuffLength = readBuffer(_oInputStream);
				if (m_iBuffLength < 0) {
					if ( m_sLineStart.isEmpty() ) {
						m_bEof = true;
					}
					oLineRef.m_sLine = m_sLineStart;
					oLineRef.m_u64Pos = m_u64FilePos;
					return oLineRef;
				}
				m_iBuffPos = 0;
				
				// Detect BOM
				if (!m_bInit){
					m_bInit = true;
					if (m_iBuffLength < 2){
						m_bUnicode = false;
						m_iCharSize = 1;
					}
					else{
						m_bUnicode = (m_abBuffer[0] == iC_BOM_0) && (m_abBuffer[1] == iC_BOM_1);
						if (m_bUnicode){
							m_iCharSize = 2;
							m_iBuffPos = 2;
						}
						else
							m_iCharSize = 1;
					}
				}
			}
			
			iPos = m_iBuffPos;
			bEndOfBuff = false;
			while (iPos < m_iBuffLength){
				if (m_abBuffer[iPos] == '\n'){
					iEnd = iPos;
					iPos += m_iCharSize;
					if ( iPos < m_iBuffLength ) {
						if (m_abBuffer[iPos] == '\r'){
							iPos += m_iCharSize;
						}
					}
					bEndOfBuff = true;
				}
				if (m_abBuffer[iPos] == '\r'){
					iEnd = iPos;
					iPos += m_iCharSize;
					if ( iPos < m_iBuffLength ) {
						if (m_abBuffer[iPos] == '\n'){
							iPos += m_iCharSize;
						}
					}
					bEndOfBuff = true;
				}
				if ( bEndOfBuff ) {
					if ( iEnd >= m_iBuffPos ) { 
						abSlice = Arrays.copyOfRange(m_abBuffer, m_iBuffPos, iEnd );
						try {
							if (!m_bUnicode)
								sSlice = new String(abSlice, "UTF-8");
							else
								sSlice = new String(abSlice, "UTF-16LE");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						oLineRef.m_sLine = m_sLineStart + sSlice;
						oLineRef.m_u64Pos = m_u64FilePos + m_iBuffPos;

						m_sLineStart = "";
						m_iBuffPos = iPos;
						return oLineRef;
					}
					bEndOfBuff = false;
					continue;
				}
				iPos += m_iCharSize;
			}
			if (m_iBuffPos < m_iBuffLength){
				abSlice = Arrays.copyOfRange(m_abBuffer, m_iBuffPos, m_iBuffLength);
				try {
					if (!m_bUnicode)
						sSlice = new String(abSlice, "UTF-8");
					else
						sSlice = new String(abSlice, "UTF-16LE");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				m_sLineStart = sSlice;
			}
			else{
				m_sLineStart = "";
			}
			m_bNeedBuffer = true;
		}
	}
}
