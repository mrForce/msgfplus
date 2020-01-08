package edu.ucsd.msjava.fdr;

import edu.ucsd.msjava.msdbsearch.CompactSuffixArray;
import edu.ucsd.msjava.msdbsearch.DatabaseMatch;
import edu.ucsd.msjava.msdbsearch.MSGFPlusMatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ComputeFDR {
    public static final float FDR_REPORT_THRESHOLD = 0.1f;

    public static void main(String argv[]) throws Exception {
        // required
        File targetFile = null;
        int scoreCol = -1;
        int specFileCol = -1;

        // optional
        File outputFile = null;
        boolean isGreaterBetter = false;
        boolean hasHeader = true;
        File decoyFile = null;
        String delimiter = "\t";
        int pepCol = -1;
        int specIndexCol = -1;
        boolean isConcatenated = false;
        boolean includeDecoy = false;

        int dbCol = -1;
        String decoyPrefix = null;
        float fdrThreshold = 1;
        float pepFDRThreshold = 1;

        ArrayList<Pair<Integer, ArrayList<String>>> reqStrList = new ArrayList<Pair<Integer, ArrayList<String>>>();

        int i = 0;
        while (i < argv.length) {
            //  -f resultFileName dbCol decoyPrefix  OR
            //  -f targetFileName decoyFileName
            if (argv[i].equalsIgnoreCase("-f")) {
                if (i + 2 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                targetFile = new File(argv[i + 1]);
                if (!targetFile.exists())
                    printUsageAndExit(argv[i + 1] + " doesn't exist.");
                else if (!targetFile.isFile())
                    printUsageAndExit(argv[i + 1] + " is not a file.");
                if (i + 3 < argv.length && !argv[i + 3].startsWith("-"))
                {
                    // concatenated; -f resultFileName dbCol decoyPrefix
                    dbCol = Integer.parseInt(argv[i + 2]);
                    decoyPrefix = argv[i + 3];
                    isConcatenated = true;
                    i += 4;
                } else
                {
                    // separate; -f targetFileName decoyFileName
                    decoyFile = new File(argv[i + 2]);
                    if (!decoyFile.exists())
                        printUsageAndExit(argv[i + 2] + " doesn't exist.");
                    else if (!decoyFile.isFile())
                        printUsageAndExit(argv[i + 2] + " is not a file.");
                    isConcatenated = false;
                    i += 3;
                }
            } else if (argv[i].equalsIgnoreCase("-s")) {
                if (i + 2 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                try {
                    scoreCol = Integer.parseInt(argv[i + 1]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("Invalid scoreCol: " + argv[i + 1]);
                }
                isGreaterBetter = argv[i + 2].equalsIgnoreCase("1");
                i += 3;
            } else if (argv[i].equalsIgnoreCase("-o")) {
                if (i + 1 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                outputFile = new File(argv[i + 1]);
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-h")) {
                if (argv[i + 1].equalsIgnoreCase("0"))
                    hasHeader = false;
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-decoy")) {
                if (argv[i + 1].equalsIgnoreCase("1"))
                    includeDecoy = true;
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-decoyprefix")) {
                if (i + 1 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                decoyPrefix = argv[i + 1];
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-delim")) {
                if (i + 1 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                delimiter = argv[i + 1];
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-p")) {
                if (i + 1 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                try {
                    pepCol = Integer.parseInt(argv[i + 1]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("Invalid pepCol: " + argv[i + 1]);
                }
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-n")) {
                if (i + 1 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                try {
                    specIndexCol = Integer.parseInt(argv[i + 1]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("Invalid pepCol: " + argv[i + 1]);
                }
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-i")) {
                if (i + 1 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                try {
                    specFileCol = Integer.parseInt(argv[i + 1]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("Invalid pepCol: " + argv[i + 1]);
                }
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-m")) {
                int matchCol = -1;
                if (i + 2 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                try {
                    matchCol = Integer.parseInt(argv[i + 1]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("Invalid matchCol: " + argv[i + 1]);
                }
                String[] token = argv[i + 2].split(",");
                ArrayList<String> reqStrOrList = new ArrayList<String>();
                for (String s : token)
                    reqStrOrList.add(s);
                reqStrList.add(new Pair<Integer, ArrayList<String>>(matchCol, reqStrOrList));
                i += 3;
            } else if (argv[i].equalsIgnoreCase("-fdr")) {
                if (i + 1 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                try {
                    fdrThreshold = Float.parseFloat(argv[i + 1]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("Invalid pepCol: " + argv[i + 1]);
                }
                i += 2;
            } else if (argv[i].equalsIgnoreCase("-pepfdr")) {
                if (i + 1 >= argv.length)
                    printUsageAndExit("Invalid parameter: " + argv[i]);
                try {
                    pepFDRThreshold = Float.parseFloat(argv[i + 1]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("Invalid pepCol: " + argv[i + 1]);
                }
                i += 2;
            } else {
                printUsageAndExit("Invalid parameter");
            }
        }

        if (targetFile == null)
            printUsageAndExit("Target is missing!");
//		if(specFileCol < 0)
//			printUsageAndExit("specFileCol is missing or invalid!");
        if (scoreCol < 0)
            printUsageAndExit("scoreCol is missing or invalid!");
        if (pepCol < 0)
            printUsageAndExit("pepCol is missing or invalid!");
        if (specIndexCol < 0)
            printUsageAndExit("specIndexCol is missing or invalid!");

        computeFDR(targetFile, decoyFile,
                scoreCol, isGreaterBetter,
                delimiter, specFileCol, specIndexCol, pepCol, reqStrList,
                isConcatenated, includeDecoy, hasHeader, dbCol, decoyPrefix, fdrThreshold, pepFDRThreshold, outputFile);
    }

    public static void printUsageAndExit(String message) {
        System.err.println(message);
        System.out.print("Usage: java -cp MSGFDB.jar fdr.ComputeFDR\n" +
                "\t -f resultFileName protCol decoyPrefix or -f targetFileName decoyFileName\n" +
                "\t -i specFileCol (SpecFile column number)\n" +
                "\t -n specIndexCol (specIndex column number)\n" +
                "\t -p pepCol (peptide column number)\n" +
                "\t -s scoreCol 0/1 (0: smaller better, 1: greater better)\n" +
                "\t [-o outputFileName (default: stdout)]\n" +
                "\t [-delim delimiter] (default: \\t)\n" +
                "\t [-m colNum keyword (the column 'colNum' must contain 'keyword'. If 'keyword' is delimited by ',' (e.g. A,B,C), then at least one must be matched.)]\n" +
                "\t [-h 0/1] (0: no header, 1: header (default))\n" +
                "\t [-fdr fdrThreshold]\n" +
                "\t [-pepfdr pepFDRThreshold]\n" +
                "\t [-decoy 0/1] (0: don't include decoy (default), 1: include decoy)\n" +
                "\t [-decoyPrefix DecoyProteinPrefix] (default: XXX)\n"
        );
        System.exit(-1);
    }

    public static void computeFDR(File targetFile, File decoyFile, int scoreCol, boolean isGreaterBetter,
                                  String delimiter, int specFileCol, int specIndexCol, int pepCol,
                                  ArrayList<Pair<Integer, ArrayList<String>>> reqStrList,
                                  boolean isConcatenated, boolean includeDecoy,
                                  boolean hasHeader, int dbCol, String decoyPrefix,
                                  float fdrThreshold, float pepFDRThreshold, File outputFile) {
        TargetDecoyAnalysis tda;
        TSVPSMSet target, decoy;
        if (dbCol >= 0)
        {
            // both target and decoy are in the same file
            target = new TSVPSMSet(targetFile, delimiter, hasHeader, scoreCol, isGreaterBetter, specFileCol, specIndexCol, pepCol, reqStrList);
            target.decoy(dbCol, decoyPrefix, true);
            target.read();

            decoy = new TSVPSMSet(targetFile, delimiter, hasHeader, scoreCol, isGreaterBetter, specFileCol, specIndexCol, pepCol, reqStrList);
            decoy.decoy(dbCol, decoyPrefix, false);
            decoy.read();
        } else {
            target = new TSVPSMSet(targetFile, delimiter, hasHeader, scoreCol, isGreaterBetter, specFileCol, specIndexCol, pepCol, reqStrList);
            target.read();
            decoy = new TSVPSMSet(decoyFile, delimiter, hasHeader, scoreCol, isGreaterBetter, specFileCol, specIndexCol, pepCol, reqStrList);
            decoy.read();
        }
        tda = new TargetDecoyAnalysis(target, decoy);

        PrintStream out = null;
        if (outputFile != null)
            try {
                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        else
            out = System.out;

        target.writeResults(tda, out, fdrThreshold, pepFDRThreshold, true);
        if (includeDecoy)
            decoy.writeResults(tda, out, fdrThreshold, pepFDRThreshold, false);

        if (out != System.out)
            out.close();
    }
    
    public static void addQValues(
            List<MSGFPlusMatch> resultList,
            CompactSuffixArray sa,
            boolean considerBestMatchOnly,
            String decoyProteinPrefix) {

        MSGFPlusPSMSet target = new MSGFPlusPSMSet(resultList, false, sa, decoyProteinPrefix);
        target.setConsiderBestMatchOnly(considerBestMatchOnly);
        target.read();
        /*
         * for now, I'll just use a script to extract the p-values and apply BH for estimating Q-value.
        if(considerBestMatchOnly) {
        	
             * Here, I'll use the p-values to compute a Q-value. This is based off the Benjamini-Hochberg procedure.
             * 
             * Since the BH procedure only applies when the p-values are independent, I'll only allow its use when using the top match for each spectra.
             * 
             * 
             
        	List<MSGFPlusMatch> target_matches = target.getPSMList();
        }
    */
        MSGFPlusPSMSet decoy = new MSGFPlusPSMSet(resultList, true, sa, decoyProteinPrefix);
        decoy.setConsiderBestMatchOnly(considerBestMatchOnly);
        decoy.read();

        TargetDecoyAnalysis tda = new TargetDecoyAnalysis(target, decoy);

        for (MSGFPlusMatch match : resultList) {
            List<DatabaseMatch> dbMatchList;
            if (considerBestMatchOnly) {
                dbMatchList = new ArrayList<DatabaseMatch>();
                dbMatchList.add(match.getBestDBMatch());
            } else
                dbMatchList = match.getMatchList();
            
            for (DatabaseMatch m : dbMatchList) {
                float psmQValue = tda.getPSMQValue((float) m.getSpecEValue());
                Float pepQValue = tda.getPepQValue(m.getPepSeq());
                m.setPSMQValue(psmQValue);
                m.setPepQValue(pepQValue);
            }

        }
    }
}
