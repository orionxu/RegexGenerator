/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http://machinelearning.inginf.units.it/)  
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.inginf.male.console;


import it.units.inginf.male.Main;
import it.units.inginf.male.configuration.Configuration;
import it.units.inginf.male.dto.SimpleConfig;
import it.units.inginf.male.inputs.DataSet;
import it.units.inginf.male.inputs.DataSet.Example;
import it.units.inginf.male.outputs.FinalSolution;
import it.units.inginf.male.outputs.Results;
import it.units.inginf.male.postprocessing.BasicPostprocessor;
import it.units.inginf.male.postprocessing.JsonPostProcessor;
import it.units.inginf.male.strategy.ExecutionStrategy;
import it.units.inginf.male.strategy.impl.CoolTextualExecutionListener;
import it.units.inginf.male.tree.Anchor;
import it.units.inginf.male.tree.Constant;
import it.units.inginf.male.tree.Node;
import it.units.inginf.male.tree.RegexRange;
import it.units.inginf.male.tree.operator.Concatenator;
import it.units.inginf.male.tree.operator.ListMatch;
import it.units.inginf.male.tree.operator.MatchMinMax;
import it.units.inginf.male.tree.operator.MatchOneOrMore;
import it.units.inginf.male.tree.operator.MatchOneOrMoreGreedy;
import it.units.inginf.male.tree.operator.MatchZeroOrMore;
import it.units.inginf.male.tree.operator.MatchZeroOrMoreGreedy;
import it.units.inginf.male.tree.operator.MatchZeroOrOne;
import it.units.inginf.male.tree.operator.Or;
import it.units.inginf.male.utils.Utils;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a commandline tool for the GP Engine, RandomRegexTurtle.
 *
 * @author MaleLabTs
 */
public class ConsoleRegexTurtle {

    private static String WARNING_MESSAGE = "\nWARNING\n"
            + "The quality of the solution depends on a number of factors, including size and syntactical properties of the learning information.\n"
            + "The algorithms embedded in this experimental prototype have always been tested with at least 25 matches over at least 2 examples.\n"
            + "It is very unlikely that a smaller number of matches allows obtaining a useful solution.\n";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SimpleConfig simpleConfiguration = new SimpleConfig();

        //Set defaults for commandline parameters
        simpleConfiguration.datasetName = "./dataset.json"; // -d
        simpleConfiguration.outputFolder = "."; // -o
        //load simpleconfig defaults
        simpleConfiguration.numberOfJobs = 32; // -j
        simpleConfiguration.generations = 1000; // -g
        simpleConfiguration.numberThreads = 4; // -t
        simpleConfiguration.populationSize = 500; //-p
        simpleConfiguration.termination = 20; //-e
        simpleConfiguration.populateOptionalFields = false;
        simpleConfiguration.isStriped = false;

        parseArgs(args, simpleConfiguration);

        simpleConfiguration.dataset = createDataset1();
        //Output warning about learning size
        String message = null;
        int numberPositiveExamples = 0;
        for (Example example : simpleConfiguration.dataset.getExamples()) {
            if (example.getNumberMatches() > 0) {
                numberPositiveExamples++;
            }
        }
        if (simpleConfiguration.dataset.getNumberMatches() < 25 || numberPositiveExamples < 2) {
            message = WARNING_MESSAGE;
        }
        Configuration config = simpleConfiguration.buildConfiguration();
        
        //change defaults for console usage
        config.setPostProcessor(new JsonPostProcessor());
        config.getPostprocessorParameters().put(BasicPostprocessor.PARAMETER_NAME_POPULATE_OPTIONAL_FIELDS, Boolean.toString(simpleConfiguration.populateOptionalFields));
        config.setOutputFolderName(simpleConfiguration.outputFolder);

        Results results = new Results(config);
        results.setComment(simpleConfiguration.comment);
        try {
            //This is an optional information
            results.setMachineHardwareSpecifications(Utils.cpuInfo());
        } catch (IOException ex) {
            Logger.getLogger(ConsoleRegexTurtle.class.getName()).log(Level.SEVERE, null, ex);
        }
        CoolTextualExecutionListener consolelistener = new CoolTextualExecutionListener(message, config, results);

        long startTime = System.currentTimeMillis();
        ExecutionStrategy strategy = config.getStrategy();
        try {
            strategy.execute(config, consolelistener);
        } catch (Exception ex) {
            Logger.getLogger(ConsoleRegexTurtle.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (config.getPostProcessor() != null) {
            startTime = System.currentTimeMillis() - startTime;
            config.getPostProcessor().elaborate(config, results, startTime);
        }
        writeBestPerformances(results.getBestSolution(), config.isIsFlagging());
    }
    
    private static DataSet createDataset1() {
        DataSet data = new DataSet("Sample DataSet 1", "", "");
        String rawPositive = "10101000\n" +
                            "01010000\n" +
                            "01110110\n" +
                            "10111001\n" +
                            "01011001\n" +
                            "11001100\n" +
                            "11111111\n" +
                            "00000000";
        String rawNegative = "011011001\n" +
                            "100000001 \n" +
                            "01000200\n" +
                            "00021000\n" +
                            "e10000000\n" +
                            "46548642\n" +
                            "2154648";
        Example ex = new Example();
        String dataStr = "";
        Scanner scnrP = new Scanner(rawPositive);
        Scanner scnrN = new Scanner(rawNegative);
        int lBound = 0;
        int rBound = 0;
        while (scnrP.hasNextLine()) {
            String temp = scnrP.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addMatchBounds(lBound, rBound);
            lBound = rBound;
        }
        while (scnrN.hasNextLine()) {
            String temp = scnrN.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addUnmatchBounds(lBound, rBound);
            lBound = rBound;
        }
        ex.string = new String(dataStr);
        data.examples.add(ex);
        
        Node n1 = new Constant("\\d");
        Node n2 = new MatchOneOrMoreGreedy();
        n2.getChildrens().add(n1);
        data.initReg = n2;
        return data;
    }
    
    private static DataSet createDataset0() {
        DataSet data = new DataSet("Sample DataSet 0", "", "");
        String rawPositive = "";
        String rawNegative = "";
        Example ex = new Example();
        String dataStr = "";
        Scanner scnrP = new Scanner(rawPositive);
        Scanner scnrN = new Scanner(rawNegative);
        int lBound = 0;
        int rBound = 0;
        while (scnrP.hasNextLine()) {
            String temp = scnrP.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addMatchBounds(lBound, rBound);
            lBound = rBound;
        }
        while (scnrN.hasNextLine()) {
            String temp = scnrN.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addUnmatchBounds(lBound, rBound);
            lBound = rBound;
        }
        ex.string = new String(dataStr);
        data.examples.add(ex);
        
        //
        
        data.initReg = null;
        return data;
    }
    
    private static DataSet createDataset2() {
        DataSet data = new DataSet("Sample DataSet 2", "", "");
        String rawPositive = "123\n" +
                            "4\n" +
                            "1,234\n" +
                            "1,234,567\n" +
                            "23,456\n" +
                            "789,456\n" +
                            "456,789,123\n" +
                            "21,654,879,213\n" +
                            "3,546,799\n" +
                            "333,333";
        String rawNegative = "1234\n" +
                            "1234.0\n" +
                            "-1234\n" +
                            "$1234\n" +
                            "3333\n" +
                            "1,2,3,45\n" +
                            "12345,6789,0\n" +
                            "12345,6789\n" +
                            "19,24,358\n" +
                            "1,87,865\n" +
                            "35,8786,897";
        Example ex = new Example();
        String dataStr = "";
        Scanner scnrP = new Scanner(rawPositive);
        Scanner scnrN = new Scanner(rawNegative);
        int lBound = 0;
        int rBound = 0;
        while (scnrP.hasNextLine()) {
            String temp = scnrP.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addMatchBounds(lBound, rBound);
            lBound = rBound;
        }
        while (scnrN.hasNextLine()) {
            String temp = scnrN.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addUnmatchBounds(lBound, rBound);
            lBound = rBound;
        }
        ex.string = new String(dataStr);
        data.examples.add(ex);
        //(\d|,)*\d+
        Node nodeN3 = new Anchor("\\d");
        Node nodeN4 = new Constant(",");
        Node nodeN2 = new Or();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN4);
        Node nodeN1 = new MatchZeroOrMore();
        nodeN1.getChildrens().add(nodeN2);
        Node nodeN6 = new Anchor("\\d");
        Node nodeN5 = new MatchOneOrMore();
        nodeN5.getChildrens().add(nodeN6);
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN5);

        
        data.initReg = nodeN0;
        return data;
    }
    
    private static DataSet createDataset3() {
        DataSet data = new DataSet("Sample DataSet 3", "", "");
        String rawPositive = "12/30/2011\n" +
"01/01/2011\n" +
"3/12/2012\n" +
"4/1/2013\n" +
"12/12/2012\n" +
"5/5/2014\n" +
"01/31/2013\n" +
"1/9/2011\n" +
"2/28/2014\n" +
"1/01/2012";
        String rawNegative = "10.03.1979\n" +
"12/30/2004\n" +
"01/01/2004\n" +
"09--02--2004\n" +
"15-15-2004\n" +
"13/12/2004\n" +
"02/31/2000\n" +
"4/1/2001\n" +
"12/12/2001\n" +
"55/5/3434\n" +
"1/1/01\n" +
"12 Jan 01\n" +
"1-1-2001\n" +
"12/55/2003\n" +
"01/31/1905\n" +
"1/9/1900\n" +
"2/29/1904\n" +
"31/01/2005\n" +
"02/29/2005\n" +
"2/29/2005";
        Example ex = new Example();
        String dataStr = "";
        Scanner scnrP = new Scanner(rawPositive);
        Scanner scnrN = new Scanner(rawNegative);
        int lBound = 0;
        int rBound = 0;
        while (scnrP.hasNextLine()) {
            String temp = scnrP.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addMatchBounds(lBound, rBound);
            lBound = rBound;
        }
        while (scnrN.hasNextLine()) {
            String temp = scnrN.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addUnmatchBounds(lBound, rBound);
            lBound = rBound;
        }
        ex.string = new String(dataStr);
        data.examples.add(ex);
        
        //(((0?[1-9]|1[012])/(0?[1-9]|1\d|2[0-8])|(0?[13456789]|1[012])/(29|30)|(0?[13578]|1[02])/31)/(19|[2-9]\d)\d{2}|0?2/29/((19|[2-9]\d)(0[48]|[2468][048]|[13579][26])|(([2468][048]|[3579][26])00)))
        Node nodeN8 = new Constant("0");
        Node nodeN7 = new MatchZeroOrOne();
        nodeN7.getChildrens().add(nodeN8);
        Node nodeN9 = new ListMatch();
        nodeN9.getChildrens().add(new RegexRange("1-9"));
        Node nodeN6 = new Concatenator();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(nodeN9);
        Node nodeN11 = new Constant("1");
        Node nodeN12 = new ListMatch();
        nodeN12.getChildrens().add(new RegexRange("120"));
        Node nodeN10 = new Concatenator();
        nodeN10.getChildrens().add(nodeN11);
        nodeN10.getChildrens().add(nodeN12);
        Node nodeN5 = new Or();
        nodeN5.getChildrens().add(nodeN6);
        nodeN5.getChildrens().add(nodeN10);
        Node nodeN13 = new Constant("/");
        Node nodeN4 = new Concatenator();
        nodeN4.getChildrens().add(nodeN5);
        nodeN4.getChildrens().add(nodeN13);
        Node nodeN14 = new Concatenator();
        Node nodeN19 = new Constant("0");
        Node nodeN18 = new MatchZeroOrOne();
        nodeN18.getChildrens().add(nodeN19);
        Node nodeN20 = new ListMatch();
        nodeN20.getChildrens().add(new RegexRange("1-9"));
        Node nodeN17 = new Concatenator();
        nodeN17.getChildrens().add(nodeN18);
        nodeN17.getChildrens().add(nodeN20);
        Node nodeN22 = new Constant("1");
        Node nodeN23 = new Anchor("\\d");
        Node nodeN21 = new Concatenator();
        nodeN21.getChildrens().add(nodeN22);
        nodeN21.getChildrens().add(nodeN23);
        Node nodeN16 = new Or();
        nodeN16.getChildrens().add(nodeN17);
        nodeN16.getChildrens().add(nodeN21);
        Node nodeN25 = new Constant("2");
        Node nodeN26 = new ListMatch();
        nodeN26.getChildrens().add(new RegexRange("0-8"));
        Node nodeN24 = new Concatenator();
        nodeN24.getChildrens().add(nodeN25);
        nodeN24.getChildrens().add(nodeN26);
        Node nodeN15 = new Or();
        nodeN15.getChildrens().add(nodeN16);
        nodeN15.getChildrens().add(nodeN24);
        nodeN14.getChildrens().add(nodeN4);
        nodeN14.getChildrens().add(nodeN15);
        Node nodeN31 = new Constant("0");
        Node nodeN30 = new MatchZeroOrOne();
        nodeN30.getChildrens().add(nodeN31);
        Node nodeN32 = new ListMatch();
        nodeN32.getChildrens().add(new RegexRange("13456789"));
        Node nodeN29 = new Concatenator();
        nodeN29.getChildrens().add(nodeN30);
        nodeN29.getChildrens().add(nodeN32);
        Node nodeN34 = new Constant("1");
        Node nodeN35 = new ListMatch();
        nodeN35.getChildrens().add(new RegexRange("120"));
        Node nodeN33 = new Concatenator();
        nodeN33.getChildrens().add(nodeN34);
        nodeN33.getChildrens().add(nodeN35);
        Node nodeN28 = new Or();
        nodeN28.getChildrens().add(nodeN29);
        nodeN28.getChildrens().add(nodeN33);
        Node nodeN36 = new Constant("/");
        Node nodeN27 = new Concatenator();
        nodeN27.getChildrens().add(nodeN28);
        nodeN27.getChildrens().add(nodeN36);
        Node nodeN37 = new Concatenator();
        Node nodeN40 = new Constant("2");
        Node nodeN41 = new Constant("9");
        Node nodeN39 = new Concatenator();
        nodeN39.getChildrens().add(nodeN40);
        nodeN39.getChildrens().add(nodeN41);
        Node nodeN43 = new Constant("3");
        Node nodeN44 = new Constant("0");
        Node nodeN42 = new Concatenator();
        nodeN42.getChildrens().add(nodeN43);
        nodeN42.getChildrens().add(nodeN44);
        Node nodeN38 = new Or();
        nodeN38.getChildrens().add(nodeN39);
        nodeN38.getChildrens().add(nodeN42);
        nodeN37.getChildrens().add(nodeN27);
        nodeN37.getChildrens().add(nodeN38);
        Node nodeN3 = new Or();
        nodeN3.getChildrens().add(nodeN14);
        nodeN3.getChildrens().add(nodeN37);
        Node nodeN49 = new Constant("0");
        Node nodeN48 = new MatchZeroOrOne();
        nodeN48.getChildrens().add(nodeN49);
        Node nodeN50 = new ListMatch();
        nodeN50.getChildrens().add(new RegexRange("13578"));
        Node nodeN47 = new Concatenator();
        nodeN47.getChildrens().add(nodeN48);
        nodeN47.getChildrens().add(nodeN50);
        Node nodeN52 = new Constant("1");
        Node nodeN53 = new ListMatch();
        nodeN53.getChildrens().add(new RegexRange("20"));
        Node nodeN51 = new Concatenator();
        nodeN51.getChildrens().add(nodeN52);
        nodeN51.getChildrens().add(nodeN53);
        Node nodeN46 = new Or();
        nodeN46.getChildrens().add(nodeN47);
        nodeN46.getChildrens().add(nodeN51);
        Node nodeN54 = new Constant("/");
        Node nodeN45 = new Concatenator();
        nodeN45.getChildrens().add(nodeN46);
        nodeN45.getChildrens().add(nodeN54);
        Node nodeN55 = new Concatenator();
        Node nodeN56 = new Constant("3");
        nodeN55.getChildrens().add(nodeN45);
        nodeN55.getChildrens().add(nodeN56);
        Node nodeN57 = new Concatenator();
        Node nodeN58 = new Constant("1");
        nodeN57.getChildrens().add(nodeN55);
        nodeN57.getChildrens().add(nodeN58);
        Node nodeN2 = new Or();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN57);
        Node nodeN59 = new Constant("/");
        Node nodeN1 = new Concatenator();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN59);
        Node nodeN60 = new Concatenator();
        Node nodeN63 = new Constant("1");
        Node nodeN64 = new Constant("9");
        Node nodeN62 = new Concatenator();
        nodeN62.getChildrens().add(nodeN63);
        nodeN62.getChildrens().add(nodeN64);
        Node nodeN66 = new ListMatch();
        nodeN66.getChildrens().add(new RegexRange("2-9"));
        Node nodeN67 = new Anchor("\\d");
        Node nodeN65 = new Concatenator();
        nodeN65.getChildrens().add(nodeN66);
        nodeN65.getChildrens().add(nodeN67);
        Node nodeN61 = new Or();
        nodeN61.getChildrens().add(nodeN62);
        nodeN61.getChildrens().add(nodeN65);
        nodeN60.getChildrens().add(nodeN1);
        nodeN60.getChildrens().add(nodeN61);
        Node nodeN68 = new Concatenator();
        Node nodeN70 = new Anchor("\\d");
        Node nodeN69 = new MatchMinMax();
        nodeN69.getChildrens().add(nodeN70);
        nodeN69.getChildrens().add(new Constant("2"));
        nodeN69.getChildrens().add(new Constant("2"));
        nodeN68.getChildrens().add(nodeN60);
        nodeN68.getChildrens().add(nodeN69);
        Node nodeN73 = new Constant("0");
        Node nodeN72 = new MatchZeroOrOne();
        nodeN72.getChildrens().add(nodeN73);
        Node nodeN74 = new Constant("2");
        Node nodeN71 = new Concatenator();
        nodeN71.getChildrens().add(nodeN72);
        nodeN71.getChildrens().add(nodeN74);
        Node nodeN75 = new Concatenator();
        Node nodeN76 = new Constant("/");
        nodeN75.getChildrens().add(nodeN71);
        nodeN75.getChildrens().add(nodeN76);
        Node nodeN77 = new Concatenator();
        Node nodeN78 = new Constant("2");
        nodeN77.getChildrens().add(nodeN75);
        nodeN77.getChildrens().add(nodeN78);
        Node nodeN79 = new Concatenator();
        Node nodeN80 = new Constant("9");
        nodeN79.getChildrens().add(nodeN77);
        nodeN79.getChildrens().add(nodeN80);
        Node nodeN81 = new Concatenator();
        Node nodeN82 = new Constant("/");
        nodeN81.getChildrens().add(nodeN79);
        nodeN81.getChildrens().add(nodeN82);
        Node nodeN83 = new Concatenator();
        Node nodeN88 = new Constant("1");
        Node nodeN89 = new Constant("9");
        Node nodeN87 = new Concatenator();
        nodeN87.getChildrens().add(nodeN88);
        nodeN87.getChildrens().add(nodeN89);
        Node nodeN91 = new ListMatch();
        nodeN91.getChildrens().add(new RegexRange("2-9"));
        Node nodeN92 = new Anchor("\\d");
        Node nodeN90 = new Concatenator();
        nodeN90.getChildrens().add(nodeN91);
        nodeN90.getChildrens().add(nodeN92);
        Node nodeN86 = new Or();
        nodeN86.getChildrens().add(nodeN87);
        nodeN86.getChildrens().add(nodeN90);
        Node nodeN96 = new Constant("0");
        Node nodeN97 = new ListMatch();
        nodeN97.getChildrens().add(new RegexRange("48"));
        Node nodeN95 = new Concatenator();
        nodeN95.getChildrens().add(nodeN96);
        nodeN95.getChildrens().add(nodeN97);
        Node nodeN99 = new ListMatch();
        nodeN99.getChildrens().add(new RegexRange("2468"));
        Node nodeN100 = new ListMatch();
        nodeN100.getChildrens().add(new RegexRange("480"));
        Node nodeN98 = new Concatenator();
        nodeN98.getChildrens().add(nodeN99);
        nodeN98.getChildrens().add(nodeN100);
        Node nodeN94 = new Or();
        nodeN94.getChildrens().add(nodeN95);
        nodeN94.getChildrens().add(nodeN98);
        Node nodeN102 = new ListMatch();
        nodeN102.getChildrens().add(new RegexRange("13579"));
        Node nodeN103 = new ListMatch();
        nodeN103.getChildrens().add(new RegexRange("26"));
        Node nodeN101 = new Concatenator();
        nodeN101.getChildrens().add(nodeN102);
        nodeN101.getChildrens().add(nodeN103);
        Node nodeN93 = new Or();
        nodeN93.getChildrens().add(nodeN94);
        nodeN93.getChildrens().add(nodeN101);
        Node nodeN85 = new Concatenator();
        nodeN85.getChildrens().add(nodeN86);
        nodeN85.getChildrens().add(nodeN93);
        Node nodeN107 = new ListMatch();
        nodeN107.getChildrens().add(new RegexRange("2468"));
        Node nodeN108 = new ListMatch();
        nodeN108.getChildrens().add(new RegexRange("480"));
        Node nodeN106 = new Concatenator();
        nodeN106.getChildrens().add(nodeN107);
        nodeN106.getChildrens().add(nodeN108);
        Node nodeN110 = new ListMatch();
        nodeN110.getChildrens().add(new RegexRange("3579"));
        Node nodeN111 = new ListMatch();
        nodeN111.getChildrens().add(new RegexRange("26"));
        Node nodeN109 = new Concatenator();
        nodeN109.getChildrens().add(nodeN110);
        nodeN109.getChildrens().add(nodeN111);
        Node nodeN105 = new Or();
        nodeN105.getChildrens().add(nodeN106);
        nodeN105.getChildrens().add(nodeN109);
        Node nodeN112 = new Constant("0");
        Node nodeN104 = new Concatenator();
        nodeN104.getChildrens().add(nodeN105);
        nodeN104.getChildrens().add(nodeN112);
        Node nodeN113 = new Concatenator();
        Node nodeN114 = new Constant("0");
        nodeN113.getChildrens().add(nodeN104);
        nodeN113.getChildrens().add(nodeN114);
        Node nodeN84 = new Or();
        nodeN84.getChildrens().add(nodeN85);
        nodeN84.getChildrens().add(nodeN113);
        nodeN83.getChildrens().add(nodeN81);
        nodeN83.getChildrens().add(nodeN84);
        Node nodeN0 = new Or();
        nodeN0.getChildrens().add(nodeN68);
        nodeN0.getChildrens().add(nodeN83);

        
        data.initReg = nodeN0;
        return data;
    }
    
    private static DataSet createDataset4() {
        DataSet data = new DataSet("Sample DataSet 4", "", "");
        String rawPositive = "20000101\n" +
"20051231\n" +
"20040229\n" +
"19990101\n" +
"19991231\n" +
"19990229\n" +
"19990101\n" +
"19980101\n" +
"19970203\n" +
"19960830\n" +
"19950722";
        String rawNegative = "20053112\n" +
"20050229\n" +
"12345\n" +
"-98.7\n" +
"3.141\n" +
".6180\n" +
"9,000\n" +
"+42\n" +
"555.123.4567\n" +
"+1-(800)-555-2468";
        Example ex = new Example();
        String dataStr = "";
        Scanner scnrP = new Scanner(rawPositive);
        Scanner scnrN = new Scanner(rawNegative);
        int lBound = 0;
        int rBound = 0;
        while (scnrP.hasNextLine()) {
            String temp = scnrP.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addMatchBounds(lBound, rBound);
            lBound = rBound;
        }
        while (scnrN.hasNextLine()) {
            String temp = scnrN.nextLine();
            dataStr += temp;
            rBound += temp.length();
            ex.addUnmatchBounds(lBound, rBound);
            lBound = rBound;
        }
        ex.string = new String(dataStr);
        data.examples.add(ex);
        
        //([2-9]\d{3}((0[1-9]|1[012])(0[1-9]|1\d|2[0-8])|(0[13456789]|1[012])(29|30)|(0[13578]|1[02])31)|(([2-9]\d)(0[48]|[2468][048]|[13579][26])|(([2468][048]|[3579][26])00))0229)
        Node nodeN2 = new ListMatch();
        nodeN2.getChildrens().add(new RegexRange("2-9"));
        Node nodeN4 = new Anchor("\\d");
        Node nodeN3 = new MatchMinMax();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(new Constant("3"));
        nodeN3.getChildrens().add(new Constant("3"));
        Node nodeN1 = new Concatenator();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN3);
        Node nodeN5 = new Concatenator();
        Node nodeN11 = new Constant("0");
        Node nodeN12 = new ListMatch();
        nodeN12.getChildrens().add(new RegexRange("1-9"));
        Node nodeN10 = new Concatenator();
        nodeN10.getChildrens().add(nodeN11);
        nodeN10.getChildrens().add(nodeN12);
        Node nodeN14 = new Constant("1");
        Node nodeN15 = new ListMatch();
        nodeN15.getChildrens().add(new RegexRange("120"));
        Node nodeN13 = new Concatenator();
        nodeN13.getChildrens().add(nodeN14);
        nodeN13.getChildrens().add(nodeN15);
        Node nodeN9 = new Or();
        nodeN9.getChildrens().add(nodeN10);
        nodeN9.getChildrens().add(nodeN13);
        Node nodeN19 = new Constant("0");
        Node nodeN20 = new ListMatch();
        nodeN20.getChildrens().add(new RegexRange("1-9"));
        Node nodeN18 = new Concatenator();
        nodeN18.getChildrens().add(nodeN19);
        nodeN18.getChildrens().add(nodeN20);
        Node nodeN22 = new Constant("1");
        Node nodeN23 = new Anchor("\\d");
        Node nodeN21 = new Concatenator();
        nodeN21.getChildrens().add(nodeN22);
        nodeN21.getChildrens().add(nodeN23);
        Node nodeN17 = new Or();
        nodeN17.getChildrens().add(nodeN18);
        nodeN17.getChildrens().add(nodeN21);
        Node nodeN25 = new Constant("2");
        Node nodeN26 = new ListMatch();
        nodeN26.getChildrens().add(new RegexRange("0-8"));
        Node nodeN24 = new Concatenator();
        nodeN24.getChildrens().add(nodeN25);
        nodeN24.getChildrens().add(nodeN26);
        Node nodeN16 = new Or();
        nodeN16.getChildrens().add(nodeN17);
        nodeN16.getChildrens().add(nodeN24);
        Node nodeN8 = new Concatenator();
        nodeN8.getChildrens().add(nodeN9);
        nodeN8.getChildrens().add(nodeN16);
        Node nodeN30 = new Constant("0");
        Node nodeN31 = new ListMatch();
        nodeN31.getChildrens().add(new RegexRange("13456789"));
        Node nodeN29 = new Concatenator();
        nodeN29.getChildrens().add(nodeN30);
        nodeN29.getChildrens().add(nodeN31);
        Node nodeN33 = new Constant("1");
        Node nodeN34 = new ListMatch();
        nodeN34.getChildrens().add(new RegexRange("120"));
        Node nodeN32 = new Concatenator();
        nodeN32.getChildrens().add(nodeN33);
        nodeN32.getChildrens().add(nodeN34);
        Node nodeN28 = new Or();
        nodeN28.getChildrens().add(nodeN29);
        nodeN28.getChildrens().add(nodeN32);
        Node nodeN37 = new Constant("2");
        Node nodeN38 = new Constant("9");
        Node nodeN36 = new Concatenator();
        nodeN36.getChildrens().add(nodeN37);
        nodeN36.getChildrens().add(nodeN38);
        Node nodeN40 = new Constant("3");
        Node nodeN41 = new Constant("0");
        Node nodeN39 = new Concatenator();
        nodeN39.getChildrens().add(nodeN40);
        nodeN39.getChildrens().add(nodeN41);
        Node nodeN35 = new Or();
        nodeN35.getChildrens().add(nodeN36);
        nodeN35.getChildrens().add(nodeN39);
        Node nodeN27 = new Concatenator();
        nodeN27.getChildrens().add(nodeN28);
        nodeN27.getChildrens().add(nodeN35);
        Node nodeN7 = new Or();
        nodeN7.getChildrens().add(nodeN8);
        nodeN7.getChildrens().add(nodeN27);
        Node nodeN45 = new Constant("0");
        Node nodeN46 = new ListMatch();
        nodeN46.getChildrens().add(new RegexRange("13578"));
        Node nodeN44 = new Concatenator();
        nodeN44.getChildrens().add(nodeN45);
        nodeN44.getChildrens().add(nodeN46);
        Node nodeN48 = new Constant("1");
        Node nodeN49 = new ListMatch();
        nodeN49.getChildrens().add(new RegexRange("20"));
        Node nodeN47 = new Concatenator();
        nodeN47.getChildrens().add(nodeN48);
        nodeN47.getChildrens().add(nodeN49);
        Node nodeN43 = new Or();
        nodeN43.getChildrens().add(nodeN44);
        nodeN43.getChildrens().add(nodeN47);
        Node nodeN50 = new Constant("3");
        Node nodeN42 = new Concatenator();
        nodeN42.getChildrens().add(nodeN43);
        nodeN42.getChildrens().add(nodeN50);
        Node nodeN51 = new Concatenator();
        Node nodeN52 = new Constant("1");
        nodeN51.getChildrens().add(nodeN42);
        nodeN51.getChildrens().add(nodeN52);
        Node nodeN6 = new Or();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(nodeN51);
        nodeN5.getChildrens().add(nodeN1);
        nodeN5.getChildrens().add(nodeN6);
        Node nodeN57 = new ListMatch();
        nodeN57.getChildrens().add(new RegexRange("2-9"));
        Node nodeN58 = new Anchor("\\d");
        Node nodeN56 = new Concatenator();
        nodeN56.getChildrens().add(nodeN57);
        nodeN56.getChildrens().add(nodeN58);
        Node nodeN62 = new Constant("0");
        Node nodeN63 = new ListMatch();
        nodeN63.getChildrens().add(new RegexRange("48"));
        Node nodeN61 = new Concatenator();
        nodeN61.getChildrens().add(nodeN62);
        nodeN61.getChildrens().add(nodeN63);
        Node nodeN65 = new ListMatch();
        nodeN65.getChildrens().add(new RegexRange("2468"));
        Node nodeN66 = new ListMatch();
        nodeN66.getChildrens().add(new RegexRange("480"));
        Node nodeN64 = new Concatenator();
        nodeN64.getChildrens().add(nodeN65);
        nodeN64.getChildrens().add(nodeN66);
        Node nodeN60 = new Or();
        nodeN60.getChildrens().add(nodeN61);
        nodeN60.getChildrens().add(nodeN64);
        Node nodeN68 = new ListMatch();
        nodeN68.getChildrens().add(new RegexRange("13579"));
        Node nodeN69 = new ListMatch();
        nodeN69.getChildrens().add(new RegexRange("26"));
        Node nodeN67 = new Concatenator();
        nodeN67.getChildrens().add(nodeN68);
        nodeN67.getChildrens().add(nodeN69);
        Node nodeN59 = new Or();
        nodeN59.getChildrens().add(nodeN60);
        nodeN59.getChildrens().add(nodeN67);
        Node nodeN55 = new Concatenator();
        nodeN55.getChildrens().add(nodeN56);
        nodeN55.getChildrens().add(nodeN59);
        Node nodeN73 = new ListMatch();
        nodeN73.getChildrens().add(new RegexRange("2468"));
        Node nodeN74 = new ListMatch();
        nodeN74.getChildrens().add(new RegexRange("480"));
        Node nodeN72 = new Concatenator();
        nodeN72.getChildrens().add(nodeN73);
        nodeN72.getChildrens().add(nodeN74);
        Node nodeN76 = new ListMatch();
        nodeN76.getChildrens().add(new RegexRange("3579"));
        Node nodeN77 = new ListMatch();
        nodeN77.getChildrens().add(new RegexRange("26"));
        Node nodeN75 = new Concatenator();
        nodeN75.getChildrens().add(nodeN76);
        nodeN75.getChildrens().add(nodeN77);
        Node nodeN71 = new Or();
        nodeN71.getChildrens().add(nodeN72);
        nodeN71.getChildrens().add(nodeN75);
        Node nodeN78 = new Constant("0");
        Node nodeN70 = new Concatenator();
        nodeN70.getChildrens().add(nodeN71);
        nodeN70.getChildrens().add(nodeN78);
        Node nodeN79 = new Concatenator();
        Node nodeN80 = new Constant("0");
        nodeN79.getChildrens().add(nodeN70);
        nodeN79.getChildrens().add(nodeN80);
        Node nodeN54 = new Or();
        nodeN54.getChildrens().add(nodeN55);
        nodeN54.getChildrens().add(nodeN79);
        Node nodeN81 = new Constant("0");
        Node nodeN53 = new Concatenator();
        nodeN53.getChildrens().add(nodeN54);
        nodeN53.getChildrens().add(nodeN81);
        Node nodeN82 = new Concatenator();
        Node nodeN83 = new Constant("2");
        nodeN82.getChildrens().add(nodeN53);
        nodeN82.getChildrens().add(nodeN83);
        Node nodeN84 = new Concatenator();
        Node nodeN85 = new Constant("2");
        nodeN84.getChildrens().add(nodeN82);
        nodeN84.getChildrens().add(nodeN85);
        Node nodeN86 = new Concatenator();
        Node nodeN87 = new Constant("9");
        nodeN86.getChildrens().add(nodeN84);
        nodeN86.getChildrens().add(nodeN87);
        Node nodeN0 = new Or();
        nodeN0.getChildrens().add(nodeN5);
        nodeN0.getChildrens().add(nodeN86);
        
        data.initReg = nodeN0;
        return data;
    }

    private static void writeBestPerformances(FinalSolution solution, boolean isFlagging) {
        if (solution != null) {
            System.out.println("Best on learning (JAVA): " + solution.getSolution());
            System.out.println("Best on learning (JS): " + solution.getSolutionJS());
            if (!isFlagging) {
                System.out.println("******Stats for Extraction task******");
                System.out.println("******Stats on training******");
                System.out.println("F-measure: " + solution.getTrainingPerformances().get("match f-measure"));
                System.out.println("Precision: " + solution.getTrainingPerformances().get("match precision"));
                System.out.println("Recall: " + solution.getTrainingPerformances().get("match recall"));
                System.out.println("Char precision: " + solution.getTrainingPerformances().get("character precision"));
                System.out.println("Char recall: " + solution.getTrainingPerformances().get("character recall"));
                System.out.println("******Stats on validation******");
                System.out.println("F-measure " + solution.getValidationPerformances().get("match f-measure"));
                System.out.println("Precision: " + solution.getValidationPerformances().get("match precision"));
                System.out.println("Recall: " + solution.getValidationPerformances().get("match recall"));
                System.out.println("Char precision: " + solution.getValidationPerformances().get("character precision"));
                System.out.println("Char recall: " + solution.getValidationPerformances().get("character recall"));
                System.out.println("******Stats on learning******");
                System.out.println("F-measure: " + solution.getLearningPerformances().get("match f-measure"));
                System.out.println("Precision: " + solution.getLearningPerformances().get("match precision"));
                System.out.println("Recall: " + solution.getLearningPerformances().get("match recall"));
                System.out.println("Char precision: " + solution.getLearningPerformances().get("character precision"));
                System.out.println("Char recall: " + solution.getLearningPerformances().get("character recall"));
            } else {
                System.out.println("******Stats for Flagging task******");
                System.out.println("******Stats on training******");
                System.out.println("Accuracy: " + solution.getTrainingPerformances().get("flag accuracy"));
                System.out.println("Fpr: " + solution.getTrainingPerformances().get("flag fpr"));
                System.out.println("Fnr: " + solution.getTrainingPerformances().get("flag fnr"));
                System.out.println("F-measure: " + solution.getTrainingPerformances().get("flag f-measure"));
                System.out.println("Precision: " + solution.getTrainingPerformances().get("flag precision"));
                System.out.println("Recall: " + solution.getTrainingPerformances().get("flag recall"));
                System.out.println("******Stats on validation******");
                System.out.println("Accuracy: " + solution.getValidationPerformances().get("flag accuracy"));
                System.out.println("Fpr: " + solution.getValidationPerformances().get("flag fpr"));
                System.out.println("Fnr: " + solution.getValidationPerformances().get("flag fnr"));
                System.out.println("F-measure " + solution.getValidationPerformances().get("flag f-measure"));
                System.out.println("Precision: " + solution.getValidationPerformances().get("flag precision"));
                System.out.println("Recall: " + solution.getValidationPerformances().get("flag recall"));
                System.out.println("******Stats on learning******");
                System.out.println("Accuracy: " + solution.getLearningPerformances().get("flag accuracy"));
                System.out.println("Fpr: " + solution.getLearningPerformances().get("flag fpr"));
                System.out.println("Fnr: " + solution.getLearningPerformances().get("flag fnr"));
                System.out.println("F-measure: " + solution.getLearningPerformances().get("flag f-measure"));
                System.out.println("Precision: " + solution.getLearningPerformances().get("flag precision"));
                System.out.println("Recall: " + solution.getLearningPerformances().get("flag recall"));
            }
        }
    }

    static private final String HELP_MESSAGE
            = "Usage:\n"
            + "java -jar ConsoleRegexTurtle -t 4 -p 500 -g 1000 -e 20.0 -c \"interesting evolution\" -x true -d dataset.json -o ./outputfolder/\n"
            + "\nOn linux you can invoke this tool using the alternative script:\n"
            + "regexturtle.sh -t 4 -p 500 -g 1000 -e 20.0 -c \"interesting evolution\" -d dataset.json -o ./outputfolder/\n"
            + "\nParameters:\n"
            + "-t number of threads, default is 2\n"
            + "-p population size, default is 500\n"
            + "-g maximum number of generations, per Job, default si 1000\n"
            + "-j number of Jobs, default si 32\n"
            + "-e percentange of number generations, defines a threshold for the separate and conquer split criteria, when best doesn't change for the provided % of generation the Job evolution separates the dataset.\n"
            + "   Default is 20%, 200 geberations with default 1000 generations.\n"
            + "-d path of the dataset json file containing the examples, this parameter is mandatory.\n"
            + "-o name of the output folder, results.json is saved into this folder; default is '.'\n"
            + "-x boolean, populates an extra field in results file, when 'true' adds all dataset examples in the results file 'examples' field, default is 'false'\n"
            + "-s boolean, when 'true' enables dataset striping, striping is an experimental feature, default is disabled: 'false'\n"
            + "-c adds an optional comment string\n"
            + "-f enables the flagging mode: solves a flagging problem with a separate-and-conquer strategy\n"
            + "-h visualizes this help message\n";

    static private void parseArgs(String[] args, SimpleConfig simpleConfig) {
        try {
            boolean mandatoryDatasetCheck = true;
            if (args.length == 0) {
                System.out.println(HELP_MESSAGE);
            }
            for (int i = 0; i < args.length; i++) {
                String string = args[i];
                i = i + 1;
                String parameter = args[i];
                switch (string) {
                    case "-t":
                        simpleConfig.numberThreads = Integer.valueOf(parameter);
                        break;
                    case "-p":
                        simpleConfig.populationSize = Integer.valueOf(parameter);
                        break;
                    case "-d":
                        simpleConfig.datasetName = parameter;
                        mandatoryDatasetCheck = false;
                        break;
                    case "-o":
                        simpleConfig.outputFolder = parameter;
                        break;
                    case "-g":
                        simpleConfig.generations = Integer.valueOf(parameter);
                        break;
                    case "-j":
                        simpleConfig.numberOfJobs = Integer.valueOf(parameter);
                        break;
                    case "-e":
                        simpleConfig.termination = Double.valueOf(parameter);
                        break;
                    case "-x":
                        simpleConfig.populateOptionalFields = Boolean.valueOf(parameter);
                        break;
                    case "-h":
                        System.out.println(HELP_MESSAGE);
                        break;
                    case "-c":
                        simpleConfig.comment = parameter;
                        break;
                    case "-s":
                        simpleConfig.isStriped = Boolean.valueOf(parameter);
                        break;
                    case "-f":
                        simpleConfig.isFlagging = true;
                        i=i-1; //Do not use parameter
                        break;
                }
            }

            if (simpleConfig.isStriped && simpleConfig.isFlagging) {
                System.out.println("Striping and flagging cannot be enabled toghether.\n" + HELP_MESSAGE);
                System.exit(1);
            }

            if (mandatoryDatasetCheck) {
                System.out.println("Dataset path is needed.\n" + HELP_MESSAGE);
                System.exit(1);
            }
        } catch (RuntimeException ex) {
            System.out.println("Problem parsing commandline parameters.\n" + HELP_MESSAGE);
            System.out.println("Error details:" + ex.toString());
            System.exit(1);
        }

    }

}
