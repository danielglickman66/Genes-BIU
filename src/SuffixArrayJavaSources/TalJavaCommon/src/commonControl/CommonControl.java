package commonControl;

import utils.Utils;
import utils.CmdLine;

public class CommonControl {

	final static String sC_PROG_NAME = "TalJavaCommon", sC_PROG_VERS = "2.3";
	final static String sC_OUTPUT = "Output", sC_WORK = "Work";

	// Command line and basic parameters (folders, ...)
	static CmdLine m_oCmdLine;
	static String m_sInputFolder, m_sOutputFolder, m_sWorkFolder;

	// Debug helpers
	static boolean m_bDebug, m_bProgress;

	public CommonControl() {
		m_oCmdLine = new CmdLine();
	}

	//
	// init: Set parameters for command-line parsing and parse it
	//
	public boolean init(String[] _CmdLineArgs) {
		if (!parseCmdLine(_CmdLineArgs)) {
			return false;
		}
		return true;
	}

	//
	// parseCmdLine: Define user parameters (with defaults) and check
	// command-line for any selected value
	//
	public static boolean parseCmdLine(String[] _asArgs) {
		boolean bDone;

		m_oCmdLine.init(2, 0, _asArgs);
		m_sInputFolder = m_oCmdLine.optFree(1);
		m_sOutputFolder = m_oCmdLine.optFree(2);

		m_bDebug = m_oCmdLine.optAppears("-debug", "debug mode", -1, false);
		m_bProgress = m_oCmdLine.optAppears("-progress", "progress feedback",
				-1, false);

		bDone = m_oCmdLine.done(sC_PROG_NAME, sC_PROG_VERS,
				"input-folder [output-folder]");

		if (m_sInputFolder.isEmpty())
			m_sInputFolder = Utils.s_RunFolder();

		if (m_sOutputFolder.isEmpty()) {
			m_sOutputFolder = Utils.s_RunFolder() + Utils.getPathSeparator()
					+ sC_OUTPUT;
		}

		if (!Utils.b_MakeFolder(m_sOutputFolder)) {
			System.out.println("*** ERROR: cannot create folder '"
					+ m_sOutputFolder + "'");
			return false;
		}

		m_sWorkFolder = m_sOutputFolder + Utils.getPathSeparator() + sC_WORK;
		if (!Utils.b_MakeFolder(m_sWorkFolder)) {
			System.out.println("*** ERROR: cannot create folder '"
					+ m_sWorkFolder + "'");
			return false;
		}

		return bDone;
	}

	//
	// EchoParameters: show selections (and defaults) on screen (and LOG)
	//
	static void EchoParameters() {
		String sPrompt;
		Utils.debugLogAndPrintLn("Input folder '" + m_sInputFolder + "'");
		sPrompt = "Parameters: ";
		if (m_bDebug)
			sPrompt = sPrompt + " mode=debug";
		Utils.debugLogAndPrintLn(sPrompt);
		Utils.debugLogAndPrintLn("Results folder '" + m_sOutputFolder);
		Utils.debugLogAndPrintLn(String.format("%064d", 0).replace("0", "-"));
	}

	//
	// main
	//
	public static void main(String[] args) {
		// MVC
		CommonControl oControl = new CommonControl();

		// Initialize Utils and MVC
		Utils.init();
		if (!oControl.init(args))
			return;

		Utils.setParams(m_bDebug, m_bProgress);

		//
		// Initialize LOG and log some basic references
		//
		Utils.debugLogAndPrintLn(">>> Running=" + sC_PROG_NAME + " "
				+ sC_PROG_VERS + " (available memory="
				+ Utils.s_FormatSize(Utils.u64_Memory(Utils.E_TecMEM.eMax)));
		Utils.debugLogInit(Utils.s_filePathOf(m_sInputFolder), sC_PROG_NAME,
				true);
		Utils.debugLogLn("");

		EchoParameters();
		Utils.debugLogFlush();

		//
		// THE END!
		//
		Utils.debugLogAndPrintLn(String.format("!!! %s",
				Utils.s_StampTime("run time", true)));

		Utils.debugLogdone();
		return;
	}
}
