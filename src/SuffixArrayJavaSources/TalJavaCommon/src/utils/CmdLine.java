package utils;

import java.util.ArrayList;

import utils.Utils;

public class CmdLine {
	private static final int iC_SWITCH_LEN = 10;
	private static final char cC_Switch = '-', cC_Negate = '-'; 
	
	private class ClOption{
		int m_iOrderIx;
		String m_sOrgParam, m_sOptionVal, m_sOrgVal, m_sHelp;
		boolean m_bFree, m_bUser, m_bNegate;
		
		public ClOption(){
			this.m_iOrderIx = 0;
			this.m_sOrgParam = "";
			this.m_sOptionVal = "";
			this.m_sOrgVal = "";
			this.m_sHelp = "";
			this.m_bFree = false;
			this.m_bUser = false;
			this.m_bNegate = false;
		}
	}
	
	private class ClParam{
		String m_sArg;
		ClOption m_oClOpt;
		
		public ClParam(){
			this.m_sArg = "";
			this.m_oClOpt = null;
		}
	}
	
	//HashMap<String, ClOption> m_hmParams = null;
	ArrayList<ClParam> m_aloParams;
	ArrayList<String> m_alsErrors;
	int m_iLastIx;
	boolean m_bHelp;
	
	public CmdLine(){
		m_aloParams = new ArrayList<ClParam>();
		m_alsErrors = new ArrayList<String>();
		m_iLastIx = 0;
		m_bHelp = false;
	}
	
	private int i_findOption(String _sSw, String _sHelp, int _iMinCh){
		ClOption oClOpt = null;
		ClParam oClParam = null;
		int iList, iRes = -1, iRealMinCh;
		String sOpt, sSw = _sSw.toLowerCase();
		
		if (_iMinCh >= 1)
			iRealMinCh = _iMinCh+1;
		else
			iRealMinCh = _iMinCh;
		
		if ((iRealMinCh >= 0) && (iRealMinCh < sSw.length())){
			for (iList=0; iList<m_aloParams.size(); iList++){
				oClParam = m_aloParams.get(iList);
				sOpt = oClParam.m_sArg;
				oClOpt = oClParam.m_oClOpt;
				
				if (oClOpt.m_bFree)
					continue;
				
				if (sOpt.substring(0, iRealMinCh).equals(sSw.substring(0, iRealMinCh))){
					iRes = iList;
					oClParam.m_sArg = sSw;
					m_aloParams.set(iList, oClParam);
					break;
				}
				else{	
					iList++;
				}
			}
		}
		else{ 
			for (iList=0; iList<m_aloParams.size(); iList++){
				oClParam = m_aloParams.get(iList);
				if (oClParam.m_sArg.equals(sSw)){
					iRes = iList;
					break;
				}
			}
		}
		
		if (iRes < 0){
			oClOpt =  new ClOption();
			oClParam =  new ClParam();
			oClParam.m_oClOpt = oClOpt;
			oClParam.m_sArg = sSw;
			m_aloParams.add(oClParam);
			iRes = m_aloParams.size()-1;
		}
		
		oClParam = m_aloParams.get(iRes);
		oClParam.m_oClOpt.m_sHelp = _sHelp;
		oClParam.m_oClOpt.m_iOrderIx = m_iLastIx+1;
		m_iLastIx++;
		
		return iRes;
	}
	
	private void writeHelpLine(String _sOption, String _sUse){
		System.out.println(Utils.s_PadRight(_sOption, iC_SWITCH_LEN)+_sUse);
	}
	
	public boolean init(int _iNumOfFree, int _iNumOfMust, String[] _asCmdLine){
		int iNumOfFree = 0, iImport = 0, iLen;
		String sThisParam, sVal, sNextParam;
		boolean bSw, bNoValue;
		ClOption oClOpt;
		ClParam oClParam;
		
		while(iImport < _asCmdLine.length){
			oClOpt = new ClOption();
			oClOpt.m_bUser = true;
			oClOpt.m_sOrgParam = _asCmdLine[iImport];
			sVal = oClOpt.m_sOrgParam.toLowerCase();
			bSw = sVal.charAt(0) == cC_Switch;
			if ( bSw ) {
				iLen = sVal.length();
				if ( iLen > 2 ) {
					if ( sVal.charAt( iLen - 1 ) == cC_Negate ) {
						sVal = sVal.substring( 0, iLen - 1 );
						oClOpt.m_bNegate = true; 
					}
				}
				sThisParam = "-" + sVal.substring(1);
			}
			else {
				sThisParam = oClOpt.m_sOrgParam;
			}
			
			if (bSw){
				if (sThisParam.length() < 2){
					m_alsErrors.add("switch without option");
					iImport++;
					continue;
				}
				if (iImport < _asCmdLine.length-1){
					sNextParam = _asCmdLine[iImport+1];
				}
				else{
					sNextParam = "--";
				}
				if ( sNextParam.charAt(0) != cC_Switch ){
					bNoValue = false;
					oClOpt.m_sOrgVal = sNextParam;
					oClOpt.m_sOptionVal = sNextParam.toUpperCase();
				}
				else
					bNoValue = true;
				if (sThisParam.equals("-?")){
					m_bHelp = true;
				}
				else{
					oClParam = new ClParam();
					oClParam.m_oClOpt = oClOpt;
					oClParam.m_sArg = sThisParam;
					m_aloParams.add(oClParam);
					if (!bNoValue)
						iImport++;
				}
			}
			else{
				oClOpt.m_bFree = true;
				oClOpt.m_iOrderIx = m_iLastIx+1;
				m_iLastIx++;
				oClParam = new ClParam();
				oClParam.m_oClOpt = oClOpt;
				oClParam.m_sArg = sThisParam;
				m_aloParams.add(oClParam);
				iNumOfFree++;
				if (iNumOfFree > _iNumOfFree){
					m_alsErrors.add("extra parameter '"+sThisParam+"'");
					iImport++;
					continue;
				}
				
			}
			iImport++;
		}
		if (iNumOfFree < _iNumOfMust){
			if (_iNumOfMust - iNumOfFree == 1){
				m_alsErrors.add("missing free parameter");
			}
			else{
				m_alsErrors.add("missing free parameters");
			}
		}
		return (m_alsErrors.size() == 0);
	}
	
	public String getOptFree(int _iIx, boolean _bCase){
		ClOption oClOpt = null;
		ClParam oClParam = null;
		int iCounter = 0;
		String sOpt;
		
		for (int iList=0; iList<m_aloParams.size(); iList++){
			oClParam = m_aloParams.get(iList);
			sOpt = oClParam.m_sArg;
			oClOpt = oClParam.m_oClOpt;
			
			if (oClOpt.m_bFree){
				iCounter++;
				if (iCounter == _iIx){
					if (!_bCase){
						return sOpt;
					}
					else{
						return oClOpt.m_sOrgParam;
					}
				}
			}
		}
		
		return "";
	}
	
	public String optFree(int _iIx){
		return getOptFree(_iIx, false);
	}
	
	public String optFreeAsCaseString(int _iIx){
		return getOptFree(_iIx, true);
	}
	
	public boolean optAppears(String _sSw, String _sHelp, int _iMinCh, boolean _bDefault){
		int iIx = i_findOption( _sSw, _sHelp, _iMinCh );
		ClParam oClParam;
		
		if ( iIx >= 0 ){
			oClParam = m_aloParams.get(iIx);
			if ( oClParam.m_oClOpt.m_bNegate )
				return !oClParam.m_oClOpt.m_bUser;
			return oClParam.m_oClOpt.m_bUser;
		}
		else
			return false;
	}
	
	public String optAsString(String _sSw, String _sHelp, int _iMinCh){
		int iIx = i_findOption(_sSw, _sHelp, _iMinCh);
		ClParam oClParam;
		
		if (iIx >= 0){
			oClParam = m_aloParams.get(iIx);
			if (oClParam.m_oClOpt.m_bUser)
				return oClParam.m_oClOpt.m_sOptionVal;
		}
		return "";
	}
	
	public String optAsCaseString(String _sSw, String _sHelp, int _iMinCh){
		int iIx = i_findOption(_sSw, _sHelp, _iMinCh);
		ClParam oClParam;
		
		if (iIx >= 0){
			oClParam = m_aloParams.get(iIx);
			if (oClParam.m_oClOpt.m_bUser)
				return oClParam.m_oClOpt.m_sOrgVal;
		}
		return "";
	}
	
	public String optExistsAsString(String _sSw, String _sHelp, int _iMinCh, String _sDefault){
		int iIx = i_findOption(_sSw, _sHelp, _iMinCh);
		ClParam oClParam;
		
		if (iIx >= 0){
			oClParam = m_aloParams.get(iIx);
			if (oClParam.m_oClOpt.m_bUser)
				return oClParam.m_oClOpt.m_sOptionVal;
		}
		return _sDefault;
	}
	
	public String optAsFileName(String _sSw, String _sHelp, String _sExt, int _iMinCh){
		String sRes = optAsCaseString(_sSw, _sHelp, _iMinCh);
		int iPos;
		
		if (sRes.length() > 0){
			iPos = sRes.lastIndexOf('.');
			if (iPos < 0)
				return sRes + "." + _sExt;
		}
		return sRes;
	}
	
	public int optAsInt(String _sSw, String _sHelp, int _iMinCh, int _iDefault){
		int iIx = i_findOption(_sSw, _sHelp, _iMinCh), iRes;
		ClParam oClParam;
		
		if (iIx >= 0){
			oClParam = m_aloParams.get(iIx);
			if (oClParam.m_oClOpt.m_bUser){
				iRes = Utils.i_safeStringToInt(oClParam.m_oClOpt.m_sOptionVal);
				if (Utils.m_bError)
					return _iDefault;
				return iRes;
			}
		}
		return _iDefault;
	}
	
	public int optAsBounded(String _sSw, String _sHelp, int _iMinCh, int _iMin, int _iDefault, int _iMax){
		int iIx = i_findOption(_sSw, _sHelp, _iMinCh), iRes;
		ClParam oClParam;
		
		if (iIx >= 0){
			oClParam = m_aloParams.get(iIx);
			if (oClParam.m_oClOpt.m_bUser){
				iRes = Utils.i_safeStringToInt(oClParam.m_oClOpt.m_sOptionVal);
				if (Utils.m_bError)
					return _iDefault;
				if ((iRes < _iMin) || (iRes > _iMax)){
					m_alsErrors.add("value out of range '"+oClParam.m_oClOpt.m_sOptionVal+"'");
					return _iDefault;
				}
				return iRes;
			}
		}
		return _iDefault;
	}
	
	public void showHelp(String _sName, String _sVersion, String _sUsage){
		ClOption oClOpt; 
		String sMsg = "", sOpt;
		
		if (m_aloParams.size() > 0)
			sMsg =  " [parameters]";
		System.out.println(_sName+" "+_sVersion+" usage: java -jar "+_sName+_sVersion+".jar "+_sUsage+sMsg);
		
		sMsg = "";
		writeHelpLine("parameter", "use");
		writeHelpLine("---------", "---");
		for (int i=0; i<m_aloParams.size() ; i++){
			oClOpt = m_aloParams.get(i).m_oClOpt;
			sOpt = m_aloParams.get(i).m_sArg.toLowerCase();
			if (oClOpt.m_bFree)
				continue;
			writeHelpLine(sOpt, m_aloParams.get(i).m_oClOpt.m_sHelp);
		}
	}
	
	public boolean done(String _sName, String _sVersion, String _sUsage){
		ClOption oClOpt;
		ClParam oClParam;
		int iList;
		String sOpt;
		boolean bStop = false;
		
		for (iList=0; iList<m_aloParams.size(); iList++){
			oClParam = m_aloParams.get(iList);
			sOpt = oClParam.m_sArg;
			oClOpt = oClParam.m_oClOpt;
			if ((oClOpt.m_iOrderIx == 0) && (!oClOpt.m_bFree) && !(sOpt.substring(1, 2).equals("_"))){
				m_alsErrors.add("extra parameter '"+sOpt+"'");
			}
		}
		bStop = (m_alsErrors.size() > 0) || (m_bHelp);
		if (bStop){
			showHelp(_sName, _sVersion, _sUsage);
		}
		return !bStop;
	}
}
