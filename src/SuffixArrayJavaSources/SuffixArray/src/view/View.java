package view;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import utils.Utils;
import beans.Beans;

public class View {
	public enum E_Sort {
	    eString, eLength, eCounter;
	}
	
	//
	//	init
	//
	public void init(){
		
	}

	//
	//	printTitle
	//
	public void printTitle(String _sTitle){
		String[] sArrParts = _sTitle.split(";");
		String sTitle;
		int iLen;
		
		for (int iLine=0; iLine < 2; iLine++){
			for (int i=0; i< sArrParts.length; i++) {
				String[] sArrTitleWidth = sArrParts[i].split(":");
				iLen = sArrTitleWidth[0].length();
				
				if (iLine == 0)
					sTitle = sArrTitleWidth[0];
				else
					sTitle = new String(new char[iLen]).replace("\0", "-");
				
				if (sArrTitleWidth.length > 2)
					sTitle = Utils.s_PadRight(sTitle,Integer.parseInt(sArrTitleWidth[1]));
							
				System.out.format("%"+sArrTitleWidth[1]+"s", sTitle);
			}
			System.out.println("");
		}
	}
	
	//
	//	printStats
	//
	public void printStats(List<Beans.C_Entry> _slEntries, E_Sort _eSort){
    	printTitle("String:32:r;Length:10;Counter:10;Avg:10;Dev:10;Score:10");
		Beans.C_Entry oEntry;

		if (_slEntries.isEmpty())
			return;
		
		switch(_eSort){
		case eString:
			Collections.sort(_slEntries, new Comparator<Beans.C_Entry>() {
			      @Override
			      public int compare(final Beans.C_Entry object1, final Beans.C_Entry object2) {
			    	  return object1.m_sStr.compareTo(object2.m_sStr);
			      }
			  });
			break;
		case eCounter:
			Collections.sort(_slEntries, new Comparator<Beans.C_Entry>() {
			      @Override
			      public int compare(final Beans.C_Entry object1, final Beans.C_Entry object2) {
			    	  if (object1.m_iCounter == object2.m_iCounter)
			    		  return 0;
			    	  else if (object1.m_iCounter < object2.m_iCounter)
			    		  return -1;
			    	  else
			    		  return 1;
			      }
			  });
			break;
		case eLength:
			Collections.sort(_slEntries, new Comparator<Beans.C_Entry>() {
			      @Override
			      public int compare(final Beans.C_Entry object1, final Beans.C_Entry object2) {
			    	  if (object1.m_iLen == object2.m_iLen)
			    		  return 0;
			    	  else if (object1.m_iLen < object2.m_iLen)
			    		  return -1;
			    	  else
			    		  return 1;
			      }
			  });
			break;
		}
		
		for (int i = 0; i < _slEntries.size(); i++) {
			oEntry = _slEntries.get(i);
			System.out.format("%32s %7d %8d %13.4f %10.4f %8.4f\n",
					Utils.s_PadRight(oEntry.m_sStr, 32), oEntry.m_iLen,
					oEntry.m_iCounter, oEntry.m_dAvg, oEntry.m_dDev,
					oEntry.m_dScore);
		}
		System.out.println("list size="+_slEntries.size());
	}
	
	//
	//	printAvgPerLength
	//
	public void printAvgPerLength(List<Beans.C_Entry> _slEntries){
		Beans.C_Entry oEntry;
		
		System.out.println("AVG TABLE:");
		for (int i = 0; i < _slEntries.size(); i++) {
			oEntry = _slEntries.get(i);
			System.out.format("%7d %7d %7d %13.4f\n", oEntry.m_iLen, oEntry.m_iCounter, oEntry.m_iTotOfLen, oEntry.m_dAvg);
		}
	}
	
	//
	//	printStdDev
	//
	public void printStdDev(List<Beans.C_Entry> _slEntries){
		Beans.C_Entry oEntry;
		
		System.out.println("DEV+AVG TABLE:");
		for (int i = 0; i < _slEntries.size(); i++) {
			oEntry = _slEntries.get(i);
			System.out.format("%7d %13.4f %13.4f\n", oEntry.m_iLen, oEntry.m_dAvg, oEntry.m_dDev);
		}
	}
}
