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
    
    private static DataSet createDataset5() {
        DataSet data = new DataSet("Sample DataSet 5", "", "");
        String rawPositive = "12/30/2004\n" +
"01/01/2004\n" +
"4/1/2001\n" +
"12/12/2001";
        String rawNegative = "10.03.1979\n" +
"09--02--2004\n" +
"15-15-2004\n" +
"13/12/2004\n" +
"02/31/2000\n" +
"55/5/3434\n" +
"1/1/01\n" +
"12 Jan 01\n" +
"1-1-2001\n" +
"12/55/2003";
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
        
        //\d{1,2}\/\d{1,2}\/\d{4}
        Node nodeN2 = new Anchor("\\d");
        Node nodeN1 = new MatchMinMax();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(new Constant("1"));
        nodeN1.getChildrens().add(new Constant("2"));
        Node nodeN3 = new Anchor("\\/");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN3);
        Node nodeN4 = new Concatenator();
        Node nodeN6 = new Anchor("\\d");
        Node nodeN5 = new MatchMinMax();
        nodeN5.getChildrens().add(nodeN6);
        nodeN5.getChildrens().add(new Constant("1"));
        nodeN5.getChildrens().add(new Constant("2"));
        nodeN4.getChildrens().add(nodeN0);
        nodeN4.getChildrens().add(nodeN5);
        Node nodeN7 = new Concatenator();
        Node nodeN8 = new Anchor("\\/");
        nodeN7.getChildrens().add(nodeN4);
        nodeN7.getChildrens().add(nodeN8);
        Node nodeN9 = new Concatenator();
        Node nodeN11 = new Anchor("\\d");
        Node nodeN10 = new MatchMinMax();
        nodeN10.getChildrens().add(nodeN11);
        nodeN10.getChildrens().add(new Constant("4"));
        nodeN10.getChildrens().add(new Constant("4"));
        nodeN9.getChildrens().add(nodeN7);
        nodeN9.getChildrens().add(nodeN10);

        data.initReg = nodeN9;
        return data;
    }
    
    private static DataSet createDataset6() {
        DataSet data = new DataSet("Sample DataSet 6", "", "");
        String rawPositive = "foo@demo.net\n" +
"bar.ba@test.co.uk\n" +
"bob-smith@foo.com\n" +
"bob.smith@foo.com\n" +
"bob_smith@foo.com\n" +
"me.you@home.co.uk\n" +
"fred&barney@stonehenge.com\n" +
"king-bart@home.simpsons.com\n" +
"bart@simpsons.info\n" +
"test@cde.com";
        String rawNegative = "test@t.com\n" +
"test@ab.com\n" +
"test@f.com\n" +
"test@gh.com\n" +
".test.@test.com\n" +
"spammer@[203.12.145.68]\n" +
"bla@bla\n" +
"A\n" +
"foo@do.net\n" +
"bar.ba@te.co.uk\n" +
"bob-smith@fo.com\n" +
"bob.smith@o.com\n" +
"(namely-,\n" +
".,(2-6\n" +
"Some;-)";
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
        
        //(([0-9a-zA-Z])+[\-a&+;m._p])*([0-9a-zA-Z])+@(([\-0-9a-zA-Z])+[.])+([a-zA-Z]){2,6}
        Node nodeN4 = new ListMatch();
        nodeN4.getChildrens().add(new RegexRange("0-9a-zA-Z"));
        Node nodeN3 = new MatchOneOrMore();
        nodeN3.getChildrens().add(nodeN4);
        Node nodeN5 = new ListMatch();
        nodeN5.getChildrens().add(new RegexRange("\\-a&+;m._p"));
        Node nodeN2 = new Concatenator();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN5);
        Node nodeN1 = new MatchZeroOrMore();
        nodeN1.getChildrens().add(nodeN2);
        Node nodeN7 = new ListMatch();
        nodeN7.getChildrens().add(new RegexRange("0-9a-zA-Z"));
        Node nodeN6 = new MatchOneOrMore();
        nodeN6.getChildrens().add(nodeN7);
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN6);
        Node nodeN8 = new Concatenator();
        Node nodeN9 = new Constant("@");
        nodeN8.getChildrens().add(nodeN0);
        nodeN8.getChildrens().add(nodeN9);
        Node nodeN10 = new Concatenator();
        Node nodeN14 = new ListMatch();
        nodeN14.getChildrens().add(new RegexRange("\\-0-9a-zA-Z"));
        Node nodeN13 = new MatchOneOrMore();
        nodeN13.getChildrens().add(nodeN14);
        Node nodeN15 = new ListMatch();
        nodeN15.getChildrens().add(new RegexRange("."));
        Node nodeN12 = new Concatenator();
        nodeN12.getChildrens().add(nodeN13);
        nodeN12.getChildrens().add(nodeN15);
        Node nodeN11 = new MatchOneOrMore();
        nodeN11.getChildrens().add(nodeN12);
        nodeN10.getChildrens().add(nodeN8);
        nodeN10.getChildrens().add(nodeN11);
        Node nodeN16 = new Concatenator();
        Node nodeN18 = new ListMatch();
        nodeN18.getChildrens().add(new RegexRange("a-zA-Z"));
        Node nodeN17 = new MatchMinMax();
        nodeN17.getChildrens().add(nodeN18);
        nodeN17.getChildrens().add(new Constant("2"));
        nodeN17.getChildrens().add(new Constant("6"));
        nodeN16.getChildrens().add(nodeN10);
        nodeN16.getChildrens().add(nodeN17);

        
        data.initReg = nodeN16;
        return data;
    }
    private static DataSet createDataset7() {
        DataSet data = new DataSet("Sample DataSet 7", "", "");
        String rawPositive = "foo@demo.net\n" +
"ba@test.co\n" +
"smith@foo.com\n" +
"smith@foo.com\n" +
"bob_smith@foo.com\n" +
"you@home.co\n" +
"barney@stonehenge.com\n" +
"test@gh.com\n" +
"test@f.com\n" +
"test@cde.com\n" +
"bart@simpsons.info\n" +
"test@t.com\n" +
"test@ab.comd\n" +
"bart@home.simp";
        String rawNegative = "you@home.coddd\n" +
"barney@stonehenge.comfs\n" +
"test@gh.comcs\n" +
"test@f.comads\n" +
"bar.\n" +
"bob-smith@foo.com\n" +
"ob.smith@foo.com\n" +
"bob_smith@foo.com\n" +
"me.you@home.co.uk\n" +
"fred&barney@stonehenge.com\n" +
"king-bart@home.simpsons.com\n" +
".test.@test.com\n" +
"spammer@[203.12.145.68]\n" +
"bla@bla";
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
        
        //\w+@[a-zA-Z_]+?\.[a-zA-Z]{2,3}
        Node nodeN2 = new Anchor("\\w");
        Node nodeN1 = new MatchOneOrMore();
        nodeN1.getChildrens().add(nodeN2);
        Node nodeN3 = new Constant("@");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN3);
        Node nodeN4 = new Concatenator();
        Node nodeN7 = new ListMatch();
        nodeN7.getChildrens().add(new RegexRange("a-zA-Z_"));
        Node nodeN6 = new MatchOneOrMore();
        nodeN6.getChildrens().add(nodeN7);
        Node nodeN5 = new MatchZeroOrOne();
        nodeN5.getChildrens().add(nodeN6);
        nodeN4.getChildrens().add(nodeN0);
        nodeN4.getChildrens().add(nodeN5);
        Node nodeN8 = new Concatenator();
        Node nodeN9 = new Anchor("\\.");
        nodeN8.getChildrens().add(nodeN4);
        nodeN8.getChildrens().add(nodeN9);
        Node nodeN10 = new Concatenator();
        Node nodeN12 = new ListMatch();
        nodeN12.getChildrens().add(new RegexRange("a-zA-Z"));
        Node nodeN11 = new MatchMinMax();
        nodeN11.getChildrens().add(nodeN12);
        nodeN11.getChildrens().add(new Constant("2"));
        nodeN11.getChildrens().add(new Constant("3"));
        nodeN10.getChildrens().add(nodeN8);
        nodeN10.getChildrens().add(nodeN11);

        
        data.initReg = nodeN10;
        return data;
    }
    private static DataSet createDataset8() {
        DataSet data = new DataSet("Sample DataSet 8", "", "");
        String rawPositive = "4984BAEC\n" +
"480EDBAC\n" +
"98439ACB\n" +
"E59CADB8\n" +
"78946C8E\n" +
"6A8B0678\n" +
"EA684B6E\n" +
"8A81646A\n" +
"8EBE84E7\n" +
"89F2D97E\n" +
"879989AD\n" +
"8998798A";
        String rawNegative = "ER987AE7\n" +
"RW897RA9\n" +
"7ER89W7E\n" +
"R979ER8T\n" +
"Q8979879\n" +
"ASDF9A9A\n" +
"7987E87R\n" +
"9W7E9R7\n" +
"98T79A7S\n" +
"9DF797ER\n" +
"8964Z5X6\n" +
"4D8EW7R9\n" +
"Q54R\n" +
"9R897B9\n" +
"78799\n" +
"89AD";
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
        
        //\w{8}
        Node nodeN1 = new Anchor("\\w");
        Node nodeN0 = new MatchMinMax();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(new Constant("8"));
        nodeN0.getChildrens().add(new Constant("8"));

        
        data.initReg = nodeN0;
        return data;
    }
    private static DataSet createDataset9() {
        DataSet data = new DataSet("Sample DataSet 9", "", "");
        String rawPositive = "113.173.40.255\n" +
"171.132.248.57\n" +
"79.93.28.178";
        String rawNegative = "111.222.333.444\n" +
"299.299.299.299\n" +
"189.57.135\n" +
"14.190.193999\n" +
"A.N.D.233\n" +
"0123456789\n" +
"_+-.,!@#$%^&*();\\/|<>\"'\n" +
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
        
        //(([0-2]*[0-9]+[0-9]+)\.([0-2]*[0-9]+[0-9]+)\.([0-2]*[0-9]+[0-9]+)\.([0-2]*[0-9]+[0-9]+))
        Node nodeN3 = new ListMatch();
        nodeN3.getChildrens().add(new RegexRange("0-2"));
        Node nodeN2 = new MatchZeroOrMore();
        nodeN2.getChildrens().add(nodeN3);
        Node nodeN5 = new ListMatch();
        nodeN5.getChildrens().add(new RegexRange("0-9"));
        Node nodeN4 = new MatchOneOrMore();
        nodeN4.getChildrens().add(nodeN5);
        Node nodeN1 = new Concatenator();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN4);
        Node nodeN6 = new Concatenator();
        Node nodeN8 = new ListMatch();
        nodeN8.getChildrens().add(new RegexRange("0-9"));
        Node nodeN7 = new MatchOneOrMore();
        nodeN7.getChildrens().add(nodeN8);
        nodeN6.getChildrens().add(nodeN1);
        nodeN6.getChildrens().add(nodeN7);
        Node nodeN9 = new Anchor("\\.");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN6);
        nodeN0.getChildrens().add(nodeN9);
        Node nodeN10 = new Concatenator();
        Node nodeN13 = new ListMatch();
        nodeN13.getChildrens().add(new RegexRange("0-2"));
        Node nodeN12 = new MatchZeroOrMore();
        nodeN12.getChildrens().add(nodeN13);
        Node nodeN15 = new ListMatch();
        nodeN15.getChildrens().add(new RegexRange("0-9"));
        Node nodeN14 = new MatchOneOrMore();
        nodeN14.getChildrens().add(nodeN15);
        Node nodeN11 = new Concatenator();
        nodeN11.getChildrens().add(nodeN12);
        nodeN11.getChildrens().add(nodeN14);
        Node nodeN16 = new Concatenator();
        Node nodeN18 = new ListMatch();
        nodeN18.getChildrens().add(new RegexRange("0-9"));
        Node nodeN17 = new MatchOneOrMore();
        nodeN17.getChildrens().add(nodeN18);
        nodeN16.getChildrens().add(nodeN11);
        nodeN16.getChildrens().add(nodeN17);
        nodeN10.getChildrens().add(nodeN0);
        nodeN10.getChildrens().add(nodeN16);
        Node nodeN19 = new Concatenator();
        Node nodeN20 = new Anchor("\\.");
        nodeN19.getChildrens().add(nodeN10);
        nodeN19.getChildrens().add(nodeN20);
        Node nodeN21 = new Concatenator();
        Node nodeN24 = new ListMatch();
        nodeN24.getChildrens().add(new RegexRange("0-2"));
        Node nodeN23 = new MatchZeroOrMore();
        nodeN23.getChildrens().add(nodeN24);
        Node nodeN26 = new ListMatch();
        nodeN26.getChildrens().add(new RegexRange("0-9"));
        Node nodeN25 = new MatchOneOrMore();
        nodeN25.getChildrens().add(nodeN26);
        Node nodeN22 = new Concatenator();
        nodeN22.getChildrens().add(nodeN23);
        nodeN22.getChildrens().add(nodeN25);
        Node nodeN27 = new Concatenator();
        Node nodeN29 = new ListMatch();
        nodeN29.getChildrens().add(new RegexRange("0-9"));
        Node nodeN28 = new MatchOneOrMore();
        nodeN28.getChildrens().add(nodeN29);
        nodeN27.getChildrens().add(nodeN22);
        nodeN27.getChildrens().add(nodeN28);
        nodeN21.getChildrens().add(nodeN19);
        nodeN21.getChildrens().add(nodeN27);
        Node nodeN30 = new Concatenator();
        Node nodeN31 = new Anchor("\\.");
        nodeN30.getChildrens().add(nodeN21);
        nodeN30.getChildrens().add(nodeN31);
        Node nodeN32 = new Concatenator();
        Node nodeN35 = new ListMatch();
        nodeN35.getChildrens().add(new RegexRange("0-2"));
        Node nodeN34 = new MatchZeroOrMore();
        nodeN34.getChildrens().add(nodeN35);
        Node nodeN37 = new ListMatch();
        nodeN37.getChildrens().add(new RegexRange("0-9"));
        Node nodeN36 = new MatchOneOrMore();
        nodeN36.getChildrens().add(nodeN37);
        Node nodeN33 = new Concatenator();
        nodeN33.getChildrens().add(nodeN34);
        nodeN33.getChildrens().add(nodeN36);
        Node nodeN38 = new Concatenator();
        Node nodeN40 = new ListMatch();
        nodeN40.getChildrens().add(new RegexRange("0-9"));
        Node nodeN39 = new MatchOneOrMore();
        nodeN39.getChildrens().add(nodeN40);
        nodeN38.getChildrens().add(nodeN33);
        nodeN38.getChildrens().add(nodeN39);
        nodeN32.getChildrens().add(nodeN30);
        nodeN32.getChildrens().add(nodeN38);

        
        data.initReg = nodeN32;
        return data;
    }
    private static DataSet createDataset10() {
        DataSet data = new DataSet("Sample DataSet 10", "", "");
        String rawPositive = "5125632154125412\n" +
"5225632154125412\n" +
"5525632154125412\n" +
"5525632154125412\n" +
"5125632154125412\n" +
"5325632154125412\n" +
"5425632154125412\n" +
"5425632154125412\n" +
"5211632154125412";
        String rawNegative = "1525632154125412\n" +
"2525632154125412\n" +
"3525632154125412\n" +
"1599999999999999\n" +
"4525632154125412\n" +
"1525632154125412\n" +
"3525632154125412\n" +
"5625632154125412\n" +
"4825632154125412\n" +
"6011632154125412\n" +
"6011-1111-1111-1111\n" +
"5423-1111-1111-1111\n" +
"341111111111111";
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
        
        //([51|52|53|54|55]{2})([0-9]{14})
        Node nodeN2 = new ListMatch();
        nodeN2.getChildrens().add(new RegexRange("1234555555||||"));
        Node nodeN1 = new MatchMinMax();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(new Constant("2"));
        nodeN1.getChildrens().add(new Constant("2"));
        Node nodeN4 = new ListMatch();
        nodeN4.getChildrens().add(new RegexRange("0-9"));
        Node nodeN3 = new MatchMinMax();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(new Constant("14"));
        nodeN3.getChildrens().add(new Constant("14"));
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN3);

        
        data.initReg = nodeN0;
        return data;
    }
    private static DataSet createDataset11() {
        DataSet data = new DataSet("Sample DataSet 11", "", "");
        String rawPositive = "xy9D29rwer\n" +
"d9vNUNswe\n" +
"gskinner\n" +
"Expression\n" +
"abcdefghijklmnopqrstuvwxyz\n" +
"ABCDEFGHIJKLMNOPQRSTUVWXYZ\n" +
"a1#Zv96g@*Yfasd4 \n" +
"q@12X*567\n" +
"d#67jhgt@erd";
        String rawNegative = "awdif\n" +
"DxRsf\n" +
"0pAbdef\n" +
"9D29rwer\n" +
"r\n" +
"9\n" +
"t5\n" +
"d9b\n" +
"29gpwerwr\n" +
"z8fn5wer\n" +
"v9wSnx0wer\n" +
"2nf9bm4d\n" +
"9vNUNswe\n" +
"Welcome\n" +
"to\n" +
"RegExr\n" +
"gskinner.com\n" +
"2proudly\n" +
"emple!\n" +
"matches\n" +
"45matches\n" +
"0123456789\n" +
"_+-.,!@#$%^&*();\\/|<>\"'\n" +
"@12X*567\n" +
"1#Zv96g@*Yfasd4\n" +
"#67jhgt@erd\n" +
"Non-Matches\n" +
"$12X*567\n" +
"1#Zv_96\n" +
"+678jhgt@erd";
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
        
        //[a-zA-Z0-9@*#]{8,15}
        Node nodeN1 = new ListMatch();
        nodeN1.getChildrens().add(new RegexRange("#*0-9a-zA-Z@"));
        Node nodeN0 = new MatchMinMax();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(new Constant("8"));
        nodeN0.getChildrens().add(new Constant("15"));

        
        data.initReg = nodeN0;
        return data;
    }
    private static DataSet createDataset12() {
        DataSet data = new DataSet("Sample DataSet 12", "", "");
        String rawPositive = "AL\n" +
"CA\n" +
"AA\n" +
"AE\n" +
"OK\n" +
"NV";
        String rawNegative = "Non-Matches\n" +
"New York\n" +
"California\n" +
"ny\n" +
"S\n" +
"SD\n" +
"KJ\n" +
"JO\n" +
"DS\n" +
"JO\n" +
"PR\n" +
"LSPWERLKJLKWEQJRPOJKLNK<ZMC\n" +
"awdif\n" +
"DxRsf\n" +
"0pAbdef\n" +
"xy9D29r\n" +
"r\n" +
"t5\n" +
"d9b\n" +
"29gp\n" +
"2nf9bm4\n" +
"d9vNU";
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
        
        //A[LKSZRAP]|C[AOT]|D[EC]|F[LM]|G[AU]|HI|I[ADLN]|K[SY]|LA|M[ADEHINOPST]|N[CDEHJMVY]|O[HKR]|P[ARW]|RI|S[CD]|T[NX]|UT|V[AIT]|W[AIVY]
        Node nodeN19 = new Constant("A");
        Node nodeN20 = new ListMatch();
        nodeN20.getChildrens().add(new RegexRange("ARSZKLP"));
        Node nodeN18 = new Concatenator();
        nodeN18.getChildrens().add(nodeN19);
        nodeN18.getChildrens().add(nodeN20);
        Node nodeN22 = new Constant("C");
        Node nodeN23 = new ListMatch();
        nodeN23.getChildrens().add(new RegexRange("ATO"));
        Node nodeN21 = new Concatenator();
        nodeN21.getChildrens().add(nodeN22);
        nodeN21.getChildrens().add(nodeN23);
        Node nodeN17 = new Or();
        nodeN17.getChildrens().add(nodeN18);
        nodeN17.getChildrens().add(nodeN21);
        Node nodeN25 = new Constant("D");
        Node nodeN26 = new ListMatch();
        nodeN26.getChildrens().add(new RegexRange("CE"));
        Node nodeN24 = new Concatenator();
        nodeN24.getChildrens().add(nodeN25);
        nodeN24.getChildrens().add(nodeN26);
        Node nodeN16 = new Or();
        nodeN16.getChildrens().add(nodeN17);
        nodeN16.getChildrens().add(nodeN24);
        Node nodeN28 = new Constant("F");
        Node nodeN29 = new ListMatch();
        nodeN29.getChildrens().add(new RegexRange("LM"));
        Node nodeN27 = new Concatenator();
        nodeN27.getChildrens().add(nodeN28);
        nodeN27.getChildrens().add(nodeN29);
        Node nodeN15 = new Or();
        nodeN15.getChildrens().add(nodeN16);
        nodeN15.getChildrens().add(nodeN27);
        Node nodeN31 = new Constant("G");
        Node nodeN32 = new ListMatch();
        nodeN32.getChildrens().add(new RegexRange("AU"));
        Node nodeN30 = new Concatenator();
        nodeN30.getChildrens().add(nodeN31);
        nodeN30.getChildrens().add(nodeN32);
        Node nodeN14 = new Or();
        nodeN14.getChildrens().add(nodeN15);
        nodeN14.getChildrens().add(nodeN30);
        Node nodeN34 = new Constant("H");
        Node nodeN35 = new Constant("I");
        Node nodeN33 = new Concatenator();
        nodeN33.getChildrens().add(nodeN34);
        nodeN33.getChildrens().add(nodeN35);
        Node nodeN13 = new Or();
        nodeN13.getChildrens().add(nodeN14);
        nodeN13.getChildrens().add(nodeN33);
        Node nodeN37 = new Constant("I");
        Node nodeN38 = new ListMatch();
        nodeN38.getChildrens().add(new RegexRange("ADLN"));
        Node nodeN36 = new Concatenator();
        nodeN36.getChildrens().add(nodeN37);
        nodeN36.getChildrens().add(nodeN38);
        Node nodeN12 = new Or();
        nodeN12.getChildrens().add(nodeN13);
        nodeN12.getChildrens().add(nodeN36);
        Node nodeN40 = new Constant("K");
        Node nodeN41 = new ListMatch();
        nodeN41.getChildrens().add(new RegexRange("SY"));
        Node nodeN39 = new Concatenator();
        nodeN39.getChildrens().add(nodeN40);
        nodeN39.getChildrens().add(nodeN41);
        Node nodeN11 = new Or();
        nodeN11.getChildrens().add(nodeN12);
        nodeN11.getChildrens().add(nodeN39);
        Node nodeN43 = new Constant("L");
        Node nodeN44 = new Constant("A");
        Node nodeN42 = new Concatenator();
        nodeN42.getChildrens().add(nodeN43);
        nodeN42.getChildrens().add(nodeN44);
        Node nodeN10 = new Or();
        nodeN10.getChildrens().add(nodeN11);
        nodeN10.getChildrens().add(nodeN42);
        Node nodeN46 = new Constant("M");
        Node nodeN47 = new ListMatch();
        nodeN47.getChildrens().add(new RegexRange("ASDTEHINOP"));
        Node nodeN45 = new Concatenator();
        nodeN45.getChildrens().add(nodeN46);
        nodeN45.getChildrens().add(nodeN47);
        Node nodeN9 = new Or();
        nodeN9.getChildrens().add(nodeN10);
        nodeN9.getChildrens().add(nodeN45);
        Node nodeN49 = new Constant("N");
        Node nodeN50 = new ListMatch();
        nodeN50.getChildrens().add(new RegexRange("CDEVHYJM"));
        Node nodeN48 = new Concatenator();
        nodeN48.getChildrens().add(nodeN49);
        nodeN48.getChildrens().add(nodeN50);
        Node nodeN8 = new Or();
        nodeN8.getChildrens().add(nodeN9);
        nodeN8.getChildrens().add(nodeN48);
        Node nodeN52 = new Constant("O");
        Node nodeN53 = new ListMatch();
        nodeN53.getChildrens().add(new RegexRange("RHK"));
        Node nodeN51 = new Concatenator();
        nodeN51.getChildrens().add(nodeN52);
        nodeN51.getChildrens().add(nodeN53);
        Node nodeN7 = new Or();
        nodeN7.getChildrens().add(nodeN8);
        nodeN7.getChildrens().add(nodeN51);
        Node nodeN55 = new Constant("P");
        Node nodeN56 = new ListMatch();
        nodeN56.getChildrens().add(new RegexRange("ARW"));
        Node nodeN54 = new Concatenator();
        nodeN54.getChildrens().add(nodeN55);
        nodeN54.getChildrens().add(nodeN56);
        Node nodeN6 = new Or();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(nodeN54);
        Node nodeN58 = new Constant("R");
        Node nodeN59 = new Constant("I");
        Node nodeN57 = new Concatenator();
        nodeN57.getChildrens().add(nodeN58);
        nodeN57.getChildrens().add(nodeN59);
        Node nodeN5 = new Or();
        nodeN5.getChildrens().add(nodeN6);
        nodeN5.getChildrens().add(nodeN57);
        Node nodeN61 = new Constant("S");
        Node nodeN62 = new ListMatch();
        nodeN62.getChildrens().add(new RegexRange("CD"));
        Node nodeN60 = new Concatenator();
        nodeN60.getChildrens().add(nodeN61);
        nodeN60.getChildrens().add(nodeN62);
        Node nodeN4 = new Or();
        nodeN4.getChildrens().add(nodeN5);
        nodeN4.getChildrens().add(nodeN60);
        Node nodeN64 = new Constant("T");
        Node nodeN65 = new ListMatch();
        nodeN65.getChildrens().add(new RegexRange("XN"));
        Node nodeN63 = new Concatenator();
        nodeN63.getChildrens().add(nodeN64);
        nodeN63.getChildrens().add(nodeN65);
        Node nodeN3 = new Or();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(nodeN63);
        Node nodeN67 = new Constant("U");
        Node nodeN68 = new Constant("T");
        Node nodeN66 = new Concatenator();
        nodeN66.getChildrens().add(nodeN67);
        nodeN66.getChildrens().add(nodeN68);
        Node nodeN2 = new Or();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN66);
        Node nodeN70 = new Constant("V");
        Node nodeN71 = new ListMatch();
        nodeN71.getChildrens().add(new RegexRange("ATI"));
        Node nodeN69 = new Concatenator();
        nodeN69.getChildrens().add(nodeN70);
        nodeN69.getChildrens().add(nodeN71);
        Node nodeN1 = new Or();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN69);
        Node nodeN73 = new Constant("W");
        Node nodeN74 = new ListMatch();
        nodeN74.getChildrens().add(new RegexRange("AVIY"));
        Node nodeN72 = new Concatenator();
        nodeN72.getChildrens().add(nodeN73);
        nodeN72.getChildrens().add(nodeN74);
        Node nodeN0 = new Or();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN72);

        
        data.initReg = nodeN0;
        return data;
    }
    private static DataSet createDataset13() {
        DataSet data = new DataSet("Sample DataSet 13", "", "");
        String rawPositive = "12:15\n" +
"10:26:59\n" +
"22:01:15\n" +
"08:30\n" +
"04:10\n" +
"09:11\n" +
"07:30:22\n" +
"10:12:24\n" +
"04:10:25\n" +
"13:12:06\n" +
"08:14:56";
        String rawNegative = "10:6:59\n" +
"2:01:15\n" +
"08:3\n" +
"4:10\n" +
"09:1\n" +
"7:3:22\n" +
"24:10:25\n" +
"13:2:60\n" +
"13:2:06\n" +
"11/30/2003\n" +
"10:12:24am\n" +
"2/29/2003\n" +
"08:14:6\n" +
"8:14:56pm\n" +
"5/22/2003\n" +
"09/09/2005\n" +
"04/02/1998\n" +
"06/02/1998";
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
        
        //(([0-1]?[0-9])|([2][0-3])):([0-5]?[0-9])(:([0-5]?[0-9]))?
        Node nodeN4 = new ListMatch();
        nodeN4.getChildrens().add(new RegexRange("0-1"));
        Node nodeN3 = new MatchZeroOrOne();
        nodeN3.getChildrens().add(nodeN4);
        Node nodeN5 = new ListMatch();
        nodeN5.getChildrens().add(new RegexRange("0-9"));
        Node nodeN2 = new Concatenator();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN5);
        Node nodeN7 = new ListMatch();
        nodeN7.getChildrens().add(new RegexRange("2"));
        Node nodeN8 = new ListMatch();
        nodeN8.getChildrens().add(new RegexRange("0-3"));
        Node nodeN6 = new Concatenator();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(nodeN8);
        Node nodeN1 = new Or();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN6);
        Node nodeN9 = new Constant(":");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN9);
        Node nodeN10 = new Concatenator();
        Node nodeN13 = new ListMatch();
        nodeN13.getChildrens().add(new RegexRange("0-5"));
        Node nodeN12 = new MatchZeroOrOne();
        nodeN12.getChildrens().add(nodeN13);
        Node nodeN14 = new ListMatch();
        nodeN14.getChildrens().add(new RegexRange("0-9"));
        Node nodeN11 = new Concatenator();
        nodeN11.getChildrens().add(nodeN12);
        nodeN11.getChildrens().add(nodeN14);
        nodeN10.getChildrens().add(nodeN0);
        nodeN10.getChildrens().add(nodeN11);
        Node nodeN15 = new Concatenator();
        Node nodeN18 = new Constant(":");
        Node nodeN21 = new ListMatch();
        nodeN21.getChildrens().add(new RegexRange("0-5"));
        Node nodeN20 = new MatchZeroOrOne();
        nodeN20.getChildrens().add(nodeN21);
        Node nodeN22 = new ListMatch();
        nodeN22.getChildrens().add(new RegexRange("0-9"));
        Node nodeN19 = new Concatenator();
        nodeN19.getChildrens().add(nodeN20);
        nodeN19.getChildrens().add(nodeN22);
        Node nodeN17 = new Concatenator();
        nodeN17.getChildrens().add(nodeN18);
        nodeN17.getChildrens().add(nodeN19);
        Node nodeN16 = new MatchZeroOrOne();
        nodeN16.getChildrens().add(nodeN17);
        nodeN15.getChildrens().add(nodeN10);
        nodeN15.getChildrens().add(nodeN16);

        
        data.initReg = nodeN15;
        return data;
    }
    private static DataSet createDataset14() {
        DataSet data = new DataSet("Sample DataSet 14", "", "");
        String rawPositive = "JG103759A\n" +
"AP019283D\n" +
"ZX047829C\n" +
"KL192845";
        String rawNegative = "AZ106878D\n" +
"AZ019283D\n" +
"ZZ047829C\n" +
"DC135798A\n" +
"FQ987654C\n" +
"KL192845T\n" +
"awdif\n" +
"DxRsf\n" +
"0pAbdef\n" +
"xy9D29rwer\n" +
"d9b\n" +
"29gpwerwr\n" +
"z8fn5wer\n" +
"v9wSnx0wer\n" +
"2nf9bm4d\n" +
"d9vNUNswe\n" +
"Welcome\n" +
"RegExrv2.1\n" +
"gskinner.com,\n" +
"2proudly\n" +
"hosted\n" +
"Edit\n" +
"q@12X*567\n" +
"a1#Zv96g@*Yfasd4d#67jhgt@erd\n" +
"Non-Matches\n" +
"$12X*567\n" +
"1#Zv_96\n" +
"+678jhgt@erd";
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
        
        //[A-CEGHJ-PR-TW-Z]{1}[A-CEGHJ-NPR-TW-Z]{1}[0-9]{6}[A-DFM]{0,1}
        Node nodeN2 = new ListMatch();
        nodeN2.getChildrens().add(new RegexRange("W-ZEA-CGHR-TJ-P"));
        Node nodeN1 = new MatchMinMax();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(new Constant("1"));
        nodeN1.getChildrens().add(new Constant("1"));
        Node nodeN4 = new ListMatch();
        nodeN4.getChildrens().add(new RegexRange("W-ZEA-CGHR-TJ-NP"));
        Node nodeN3 = new MatchMinMax();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(new Constant("1"));
        nodeN3.getChildrens().add(new Constant("1"));
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN3);
        Node nodeN5 = new Concatenator();
        Node nodeN7 = new ListMatch();
        nodeN7.getChildrens().add(new RegexRange("0-9"));
        Node nodeN6 = new MatchMinMax();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(new Constant("6"));
        nodeN6.getChildrens().add(new Constant("6"));
        nodeN5.getChildrens().add(nodeN0);
        nodeN5.getChildrens().add(nodeN6);
        Node nodeN8 = new Concatenator();
        Node nodeN10 = new ListMatch();
        nodeN10.getChildrens().add(new RegexRange("FA-DM"));
        Node nodeN9 = new MatchMinMax();
        nodeN9.getChildrens().add(nodeN10);
        nodeN9.getChildrens().add(new Constant("0"));
        nodeN9.getChildrens().add(new Constant("1"));
        nodeN8.getChildrens().add(nodeN5);
        nodeN8.getChildrens().add(nodeN9);

        
        data.initReg = nodeN8;
        return data;
    }
    
    private static DataSet createDataset15() {
        DataSet data = new DataSet("Sample DataSet 15", "", "");
        String rawPositive = "http://someserver\n" +
"http://www.someserver.com/\n" +
"http://www.someserver.com/somefile.txt\n" +
"http://foo.co.uk/\n" +
"http://regexr.com/foo.html?q=bar\n" +
"https://mediatemple.net\n" +
"www.someserver.com/\n" +
"www.someserver.com/somefile.txt\n" +
"foo.co.uk/\n" +
"regexr.com/foo.html?q=bar\n" +
"mediatemple.net\n" +
"www.co.uk/\n" +
"www.test.com\n" +
"www.domain.com\n" +
"http://regexr.com/foo.html?q=bar\n" +
"https://mediatemple.net";
        String rawNegative = "-domain.com domain-.com\n" +
"3SquareBand.com\n" +
"asp.net\n" +
"army.mil\n" +
"$SquareBand.com\n" +
"asp/dot.net\n" +
"army.military";
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
        
        //[a-zA-Z]{3,}://[a-zA-Z0-9\.]+/*[a-zA-Z0-9/\\%_.]*\?*[a-zA-Z0-9/\\%_.=&amp;]*
        Node nodeN2 = new ListMatch();
        nodeN2.getChildrens().add(new RegexRange("a-zA-Z"));
        Node nodeN1 = new MatchMinMax();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(new Constant("3"));
        nodeN1.getChildrens().add(new Constant("65536"));
        Node nodeN3 = new Constant(":");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN3);
        Node nodeN4 = new Concatenator();
        Node nodeN5 = new Constant("/");
        nodeN4.getChildrens().add(nodeN0);
        nodeN4.getChildrens().add(nodeN5);
        Node nodeN6 = new Concatenator();
        Node nodeN7 = new Constant("/");
        nodeN6.getChildrens().add(nodeN4);
        nodeN6.getChildrens().add(nodeN7);
        Node nodeN8 = new Concatenator();
        Node nodeN10 = new ListMatch();
        nodeN10.getChildrens().add(new RegexRange("\\.0-9a-zA-Z"));
        Node nodeN9 = new MatchOneOrMore();
        nodeN9.getChildrens().add(nodeN10);
        nodeN8.getChildrens().add(nodeN6);
        nodeN8.getChildrens().add(nodeN9);
        Node nodeN11 = new Concatenator();
        Node nodeN13 = new Constant("/");
        Node nodeN12 = new MatchZeroOrMore();
        nodeN12.getChildrens().add(nodeN13);
        nodeN11.getChildrens().add(nodeN8);
        nodeN11.getChildrens().add(nodeN12);
        Node nodeN14 = new Concatenator();
        Node nodeN16 = new ListMatch();
        nodeN16.getChildrens().add(new RegexRange("%0-9a-zA-Z./_\\"));
        Node nodeN15 = new MatchZeroOrMore();
        nodeN15.getChildrens().add(nodeN16);
        nodeN14.getChildrens().add(nodeN11);
        nodeN14.getChildrens().add(nodeN15);
        Node nodeN17 = new Concatenator();
        Node nodeN19 = new Anchor("\\?");
        Node nodeN18 = new MatchZeroOrMore();
        nodeN18.getChildrens().add(nodeN19);
        nodeN17.getChildrens().add(nodeN14);
        nodeN17.getChildrens().add(nodeN18);
        Node nodeN20 = new Concatenator();
        Node nodeN22 = new ListMatch();
        nodeN22.getChildrens().add(new RegexRange("a%&a-zmA-Z./p;0-9=_\\"));
        Node nodeN21 = new MatchZeroOrMore();
        nodeN21.getChildrens().add(nodeN22);
        nodeN20.getChildrens().add(nodeN17);
        nodeN20.getChildrens().add(nodeN21);

        
        data.initReg = nodeN20;
        return data;
    }
    private static DataSet createDataset16() {
        DataSet data = new DataSet("Sample DataSet 16", "", "");
        String rawPositive = "http://someserver\n" +
"http://www.someserver.com/\n" +
"http://www.someserver.com/somefile.txt\n" +
"http://foo.co.uk/\n" +
"http://regexr.com/foo.html?q=bar\n" +
"https://mediatemple.net\n" +
"http://foo.co.uk/http://someserver\n" +
"ftps://www.someserver.com/\n" +
"ftps://www.someserver.com/somefile.txt\n" +
"ftps://foo.co.uk/\n" +
"ftp://regexr.com/foo.html?q=bar\n" +
"ftps://mediatemple.net\n" +
"ftps://regexr.com/foo.html?q=bar\n" +
"https://mediatemple.net\n" +
"http://yourmoma\n" +
"http://www.aspemporium.com\n" +
"mailto:dominionx@hotmail.com\n" +
"ftp://ftp.test.com\n" +
"http://regexlib.com/REDetails.aspx?regexp_id=37";
        String rawNegative = "www.yahoo.com/period\n" +
"foo@demo.net\n" +
"bar.ba@test.co.uk\n" +
"www.demo.com\n" +
"www.abc.co.il\n" +
"-domain.com\n" +
"domain-.com ";
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
        
        //(mailto\:|news|ht|ftp\:){1}(\S)+
        Node nodeN6 = new Constant("m");
        Node nodeN7 = new Constant("a");
        Node nodeN5 = new Concatenator();
        nodeN5.getChildrens().add(nodeN6);
        nodeN5.getChildrens().add(nodeN7);
        Node nodeN8 = new Concatenator();
        Node nodeN9 = new Constant("i");
        nodeN8.getChildrens().add(nodeN5);
        nodeN8.getChildrens().add(nodeN9);
        Node nodeN10 = new Concatenator();
        Node nodeN11 = new Constant("l");
        nodeN10.getChildrens().add(nodeN8);
        nodeN10.getChildrens().add(nodeN11);
        Node nodeN12 = new Concatenator();
        Node nodeN13 = new Constant("t");
        nodeN12.getChildrens().add(nodeN10);
        nodeN12.getChildrens().add(nodeN13);
        Node nodeN14 = new Concatenator();
        Node nodeN15 = new Constant("o");
        nodeN14.getChildrens().add(nodeN12);
        nodeN14.getChildrens().add(nodeN15);
        Node nodeN16 = new Concatenator();
        Node nodeN17 = new Anchor("\\:");
        nodeN16.getChildrens().add(nodeN14);
        nodeN16.getChildrens().add(nodeN17);
        Node nodeN19 = new Constant("n");
        Node nodeN20 = new Constant("e");
        Node nodeN18 = new Concatenator();
        nodeN18.getChildrens().add(nodeN19);
        nodeN18.getChildrens().add(nodeN20);
        Node nodeN21 = new Concatenator();
        Node nodeN22 = new Constant("w");
        nodeN21.getChildrens().add(nodeN18);
        nodeN21.getChildrens().add(nodeN22);
        Node nodeN23 = new Concatenator();
        Node nodeN24 = new Constant("s");
        nodeN23.getChildrens().add(nodeN21);
        nodeN23.getChildrens().add(nodeN24);
        Node nodeN4 = new Or();
        nodeN4.getChildrens().add(nodeN16);
        nodeN4.getChildrens().add(nodeN23);
        Node nodeN26 = new Constant("h");
        Node nodeN27 = new Constant("t");
        Node nodeN25 = new Concatenator();
        nodeN25.getChildrens().add(nodeN26);
        nodeN25.getChildrens().add(nodeN27);
        Node nodeN3 = new Or();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(nodeN25);
        Node nodeN29 = new Constant("f");
        Node nodeN30 = new Constant("t");
        Node nodeN28 = new Concatenator();
        nodeN28.getChildrens().add(nodeN29);
        nodeN28.getChildrens().add(nodeN30);
        Node nodeN31 = new Concatenator();
        Node nodeN32 = new Constant("p");
        nodeN31.getChildrens().add(nodeN28);
        nodeN31.getChildrens().add(nodeN32);
        Node nodeN33 = new Concatenator();
        Node nodeN34 = new Anchor("\\:");
        nodeN33.getChildrens().add(nodeN31);
        nodeN33.getChildrens().add(nodeN34);
        Node nodeN2 = new Or();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN33);
        Node nodeN1 = new MatchMinMax();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(new Constant("1"));
        nodeN1.getChildrens().add(new Constant("1"));
        Node nodeN36 = new Anchor("\\S");
        Node nodeN35 = new MatchOneOrMore();
        nodeN35.getChildrens().add(nodeN36);
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN35);

        
        data.initReg = nodeN0;
        return data;
    }
    private static DataSet createDataset17() {
        DataSet data = new DataSet("Sample DataSet 17", "", "");
        String rawPositive = "http://www.someserver.com/\n" +
"http://www.someserver.com/somefile.txt\n" +
"http://foo.co.uk/\n" +
"http://regexr.com/foo.html?q=bar\n" +
"https://mediatemple.net\n" +
"http://foo.co.uk/\n" +
"http://regexr.com/foo.html?q=bar\n" +
"https://mediatemple.net\n" +
"http://www.aspemporium.com\n" +
"ftp://ftp.test.com\n" +
"http://regexlib.com/REDetails.aspx?regexp_id=37";
        String rawNegative = "http://someserver\n" +
"www.yahoo.com/period\n" +
"foo@demo.net\n" +
"bar.ba@test.co.uk\n" +
"www.demo.com\n" +
"-domain.com\n" +
"domain-.com\n" +
"http://yourmoma\n" +
"mailto:dominionx@hotmail.com\n" +
"www.abc.co.il\n" +
"http://www.yahoo\n" +
"http://www.textlink";
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
        
        //(http|ftp|https):\/\/[\w\-_]+(\.[\w\-_]+)+([\w\-\.,@?^=%&amp;:/~\+#]*[\w\-\@?^=%&amp;/~\+#])?
        Node nodeN4 = new Constant("h");
        Node nodeN5 = new Constant("t");
        Node nodeN3 = new Concatenator();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(nodeN5);
        Node nodeN6 = new Concatenator();
        Node nodeN7 = new Constant("t");
        nodeN6.getChildrens().add(nodeN3);
        nodeN6.getChildrens().add(nodeN7);
        Node nodeN8 = new Concatenator();
        Node nodeN9 = new Constant("p");
        nodeN8.getChildrens().add(nodeN6);
        nodeN8.getChildrens().add(nodeN9);
        Node nodeN11 = new Constant("f");
        Node nodeN12 = new Constant("t");
        Node nodeN10 = new Concatenator();
        nodeN10.getChildrens().add(nodeN11);
        nodeN10.getChildrens().add(nodeN12);
        Node nodeN13 = new Concatenator();
        Node nodeN14 = new Constant("p");
        nodeN13.getChildrens().add(nodeN10);
        nodeN13.getChildrens().add(nodeN14);
        Node nodeN2 = new Or();
        nodeN2.getChildrens().add(nodeN8);
        nodeN2.getChildrens().add(nodeN13);
        Node nodeN16 = new Constant("h");
        Node nodeN17 = new Constant("t");
        Node nodeN15 = new Concatenator();
        nodeN15.getChildrens().add(nodeN16);
        nodeN15.getChildrens().add(nodeN17);
        Node nodeN18 = new Concatenator();
        Node nodeN19 = new Constant("t");
        nodeN18.getChildrens().add(nodeN15);
        nodeN18.getChildrens().add(nodeN19);
        Node nodeN20 = new Concatenator();
        Node nodeN21 = new Constant("p");
        nodeN20.getChildrens().add(nodeN18);
        nodeN20.getChildrens().add(nodeN21);
        Node nodeN22 = new Concatenator();
        Node nodeN23 = new Constant("s");
        nodeN22.getChildrens().add(nodeN20);
        nodeN22.getChildrens().add(nodeN23);
        Node nodeN1 = new Or();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN22);
        Node nodeN24 = new Constant(":");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN24);
        Node nodeN25 = new Concatenator();
        Node nodeN26 = new Anchor("\\/");
        nodeN25.getChildrens().add(nodeN0);
        nodeN25.getChildrens().add(nodeN26);
        Node nodeN27 = new Concatenator();
        Node nodeN28 = new Anchor("\\/");
        nodeN27.getChildrens().add(nodeN25);
        nodeN27.getChildrens().add(nodeN28);
        Node nodeN29 = new Concatenator();
        Node nodeN31 = new ListMatch();
        nodeN31.getChildrens().add(new RegexRange("\\-\\w_"));
        Node nodeN30 = new MatchOneOrMore();
        nodeN30.getChildrens().add(nodeN31);
        nodeN29.getChildrens().add(nodeN27);
        nodeN29.getChildrens().add(nodeN30);
        Node nodeN32 = new Concatenator();
        Node nodeN35 = new Anchor("\\.");
        Node nodeN37 = new ListMatch();
        nodeN37.getChildrens().add(new RegexRange("\\-\\w_"));
        Node nodeN36 = new MatchOneOrMore();
        nodeN36.getChildrens().add(nodeN37);
        Node nodeN34 = new Concatenator();
        nodeN34.getChildrens().add(nodeN35);
        nodeN34.getChildrens().add(nodeN36);
        Node nodeN33 = new MatchOneOrMore();
        nodeN33.getChildrens().add(nodeN34);
        nodeN32.getChildrens().add(nodeN29);
        nodeN32.getChildrens().add(nodeN33);
        Node nodeN38 = new Concatenator();
        Node nodeN42 = new ListMatch();
        nodeN42.getChildrens().add(new RegexRange("a#%&,m/\\+p\\-\\.:\\w;=^~?@"));
        Node nodeN41 = new MatchZeroOrMore();
        nodeN41.getChildrens().add(nodeN42);
        Node nodeN43 = new ListMatch();
        nodeN43.getChildrens().add(new RegexRange("a#\\@%&m/\\+p\\-\\w;=^~?"));
        Node nodeN40 = new Concatenator();
        nodeN40.getChildrens().add(nodeN41);
        nodeN40.getChildrens().add(nodeN43);
        Node nodeN39 = new MatchZeroOrOne();
        nodeN39.getChildrens().add(nodeN40);
        nodeN38.getChildrens().add(nodeN32);
        nodeN38.getChildrens().add(nodeN39);
        
        data.initReg = nodeN38;
        return data;
    }
    private static DataSet createDataset18() {
        DataSet data = new DataSet("Sample DataSet 18", "", "");
        String rawPositive = "3SquareBand.com\n" +
"asp.net\n" +
"army.mil\n" +
"gskinner.com\n" +
"demo.net\n" +
"regexr.com\n" +
"mediatemple.net\n" +
"www.demo.com";
        String rawNegative = "$SquareBand.com\n" +
"asp/dot.net\n" +
"army.military\n" +
"Welcom\n" +
"Temple!\n" +
"Edit&\n" +
"matches.\n" +
"ctrl-z.\n" +
"utorial.\n" +
"abcdefghijklmnopqrstuvwxyz\n" +
"ABCDEFGHIJKLMNOPQRSTUVWXYZ\n" +
"0123456789\n" +
"_+-.,!@#$%^&*();\\/|<>\"'\n" +
"foo@\n" +
"bar.ba@test.co.uk\n" +
"http://foo.co.uk/\n" +
"http://regexr.com/foo.html?q=bar\n" +
"https://mediatemple.net\n" +
"-domain.com\n" +
"domain-.com";
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
        
        //[a-zA-Z0-9\-\.]+\.(com|org|net|mil|edu|COM|ORG|NET|MIL|EDU)
        Node nodeN2 = new ListMatch();
        nodeN2.getChildrens().add(new RegexRange("\\-\\.0-9a-zA-Z"));
        Node nodeN1 = new MatchOneOrMore();
        nodeN1.getChildrens().add(nodeN2);
        Node nodeN3 = new Anchor("\\.");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN3);
        Node nodeN4 = new Concatenator();
        Node nodeN15 = new Constant("c");
        Node nodeN16 = new Constant("o");
        Node nodeN14 = new Concatenator();
        nodeN14.getChildrens().add(nodeN15);
        nodeN14.getChildrens().add(nodeN16);
        Node nodeN17 = new Concatenator();
        Node nodeN18 = new Constant("m");
        nodeN17.getChildrens().add(nodeN14);
        nodeN17.getChildrens().add(nodeN18);
        Node nodeN20 = new Constant("o");
        Node nodeN21 = new Constant("r");
        Node nodeN19 = new Concatenator();
        nodeN19.getChildrens().add(nodeN20);
        nodeN19.getChildrens().add(nodeN21);
        Node nodeN22 = new Concatenator();
        Node nodeN23 = new Constant("g");
        nodeN22.getChildrens().add(nodeN19);
        nodeN22.getChildrens().add(nodeN23);
        Node nodeN13 = new Or();
        nodeN13.getChildrens().add(nodeN17);
        nodeN13.getChildrens().add(nodeN22);
        Node nodeN25 = new Constant("n");
        Node nodeN26 = new Constant("e");
        Node nodeN24 = new Concatenator();
        nodeN24.getChildrens().add(nodeN25);
        nodeN24.getChildrens().add(nodeN26);
        Node nodeN27 = new Concatenator();
        Node nodeN28 = new Constant("t");
        nodeN27.getChildrens().add(nodeN24);
        nodeN27.getChildrens().add(nodeN28);
        Node nodeN12 = new Or();
        nodeN12.getChildrens().add(nodeN13);
        nodeN12.getChildrens().add(nodeN27);
        Node nodeN30 = new Constant("m");
        Node nodeN31 = new Constant("i");
        Node nodeN29 = new Concatenator();
        nodeN29.getChildrens().add(nodeN30);
        nodeN29.getChildrens().add(nodeN31);
        Node nodeN32 = new Concatenator();
        Node nodeN33 = new Constant("l");
        nodeN32.getChildrens().add(nodeN29);
        nodeN32.getChildrens().add(nodeN33);
        Node nodeN11 = new Or();
        nodeN11.getChildrens().add(nodeN12);
        nodeN11.getChildrens().add(nodeN32);
        Node nodeN35 = new Constant("e");
        Node nodeN36 = new Constant("d");
        Node nodeN34 = new Concatenator();
        nodeN34.getChildrens().add(nodeN35);
        nodeN34.getChildrens().add(nodeN36);
        Node nodeN37 = new Concatenator();
        Node nodeN38 = new Constant("u");
        nodeN37.getChildrens().add(nodeN34);
        nodeN37.getChildrens().add(nodeN38);
        Node nodeN10 = new Or();
        nodeN10.getChildrens().add(nodeN11);
        nodeN10.getChildrens().add(nodeN37);
        Node nodeN40 = new Constant("C");
        Node nodeN41 = new Constant("O");
        Node nodeN39 = new Concatenator();
        nodeN39.getChildrens().add(nodeN40);
        nodeN39.getChildrens().add(nodeN41);
        Node nodeN42 = new Concatenator();
        Node nodeN43 = new Constant("M");
        nodeN42.getChildrens().add(nodeN39);
        nodeN42.getChildrens().add(nodeN43);
        Node nodeN9 = new Or();
        nodeN9.getChildrens().add(nodeN10);
        nodeN9.getChildrens().add(nodeN42);
        Node nodeN45 = new Constant("O");
        Node nodeN46 = new Constant("R");
        Node nodeN44 = new Concatenator();
        nodeN44.getChildrens().add(nodeN45);
        nodeN44.getChildrens().add(nodeN46);
        Node nodeN47 = new Concatenator();
        Node nodeN48 = new Constant("G");
        nodeN47.getChildrens().add(nodeN44);
        nodeN47.getChildrens().add(nodeN48);
        Node nodeN8 = new Or();
        nodeN8.getChildrens().add(nodeN9);
        nodeN8.getChildrens().add(nodeN47);
        Node nodeN50 = new Constant("N");
        Node nodeN51 = new Constant("E");
        Node nodeN49 = new Concatenator();
        nodeN49.getChildrens().add(nodeN50);
        nodeN49.getChildrens().add(nodeN51);
        Node nodeN52 = new Concatenator();
        Node nodeN53 = new Constant("T");
        nodeN52.getChildrens().add(nodeN49);
        nodeN52.getChildrens().add(nodeN53);
        Node nodeN7 = new Or();
        nodeN7.getChildrens().add(nodeN8);
        nodeN7.getChildrens().add(nodeN52);
        Node nodeN55 = new Constant("M");
        Node nodeN56 = new Constant("I");
        Node nodeN54 = new Concatenator();
        nodeN54.getChildrens().add(nodeN55);
        nodeN54.getChildrens().add(nodeN56);
        Node nodeN57 = new Concatenator();
        Node nodeN58 = new Constant("L");
        nodeN57.getChildrens().add(nodeN54);
        nodeN57.getChildrens().add(nodeN58);
        Node nodeN6 = new Or();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(nodeN57);
        Node nodeN60 = new Constant("E");
        Node nodeN61 = new Constant("D");
        Node nodeN59 = new Concatenator();
        nodeN59.getChildrens().add(nodeN60);
        nodeN59.getChildrens().add(nodeN61);
        Node nodeN62 = new Concatenator();
        Node nodeN63 = new Constant("U");
        nodeN62.getChildrens().add(nodeN59);
        nodeN62.getChildrens().add(nodeN63);
        Node nodeN5 = new Or();
        nodeN5.getChildrens().add(nodeN6);
        nodeN5.getChildrens().add(nodeN62);
        nodeN4.getChildrens().add(nodeN0);
        nodeN4.getChildrens().add(nodeN5);

        
        data.initReg = nodeN4;
        return data;
    }
    private static DataSet createDataset19() {
        DataSet data = new DataSet("Sample DataSet 19", "", "");
        String rawPositive = "$0.84\n" +
"$123\n" +
"$1,234,567.89\n" +
"$123,456.01\n" +
"$1.23\n" +
"$0.84\n" +
"$23,458\n" +
"$4,567,123.89";
        String rawNegative = "$123458\n" +
"$1234567.89\n" +
"$12,3456.01\n" +
"12345\n" +
"$1.234\n" +
"$1234\n" +
"567.89\n" +
"$1234,567.89\n" +
"$56543841.01\n" +
"2345\n" +
"$1.234\n" +
"0123456789\n" +
"_+-.,!@#$%^&*();\\/|<>\"'\n" +
"12345 \n" +
"-98.7\n" +
"3.141\n" +
"6180\n" +
"9,000\n" +
"+42";
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
        
        //\$(\d{1,3}(\,\d{3})*|(\d+))(\.\d{2})?
        Node nodeN1 = new Anchor("\\$");
        Node nodeN5 = new Anchor("\\d");
        Node nodeN4 = new MatchMinMax();
        nodeN4.getChildrens().add(nodeN5);
        nodeN4.getChildrens().add(new Constant("1"));
        nodeN4.getChildrens().add(new Constant("3"));
        Node nodeN8 = new Anchor("\\,");
        Node nodeN10 = new Anchor("\\d");
        Node nodeN9 = new MatchMinMax();
        nodeN9.getChildrens().add(nodeN10);
        nodeN9.getChildrens().add(new Constant("3"));
        nodeN9.getChildrens().add(new Constant("3"));
        Node nodeN7 = new Concatenator();
        nodeN7.getChildrens().add(nodeN8);
        nodeN7.getChildrens().add(nodeN9);
        Node nodeN6 = new MatchZeroOrMore();
        nodeN6.getChildrens().add(nodeN7);
        Node nodeN3 = new Concatenator();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(nodeN6);
        Node nodeN12 = new Anchor("\\d");
        Node nodeN11 = new MatchOneOrMore();
        nodeN11.getChildrens().add(nodeN12);
        Node nodeN2 = new Or();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN11);
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN2);
        Node nodeN13 = new Concatenator();
        Node nodeN16 = new Anchor("\\.");
        Node nodeN18 = new Anchor("\\d");
        Node nodeN17 = new MatchMinMax();
        nodeN17.getChildrens().add(nodeN18);
        nodeN17.getChildrens().add(new Constant("2"));
        nodeN17.getChildrens().add(new Constant("2"));
        Node nodeN15 = new Concatenator();
        nodeN15.getChildrens().add(nodeN16);
        nodeN15.getChildrens().add(nodeN17);
        Node nodeN14 = new MatchZeroOrOne();
        nodeN14.getChildrens().add(nodeN15);
        nodeN13.getChildrens().add(nodeN0);
        nodeN13.getChildrens().add(nodeN14);

        
        data.initReg = nodeN13;
        return data;
    }
    private static DataSet createDataset20() {
        DataSet data = new DataSet("Sample DataSet 20", "", "");
        String rawPositive = "12/2002\n" +
"11/1900\n" +
"02/1977\n" +
"2/1977\n" +
"5/2002";
        String rawNegative = "0/2003\n" +
"13/2002\n" +
"12/02\n" +
"15/1900\n" +
"10/10/1977\n" +
"12-2002\n" +
"2002/12";
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
        
        //((0[1-9])|(1[0-2]))\/(\d{4})
        Node nodeN3 = new Constant("0");
        Node nodeN4 = new ListMatch();
        nodeN4.getChildrens().add(new RegexRange("1-9"));
        Node nodeN2 = new Concatenator();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN4);
        Node nodeN6 = new Constant("1");
        Node nodeN7 = new ListMatch();
        nodeN7.getChildrens().add(new RegexRange("0-2"));
        Node nodeN5 = new Concatenator();
        nodeN5.getChildrens().add(nodeN6);
        nodeN5.getChildrens().add(nodeN7);
        Node nodeN1 = new Or();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN5);
        Node nodeN8 = new Anchor("\\/");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN8);
        Node nodeN9 = new Concatenator();
        Node nodeN11 = new Anchor("\\d");
        Node nodeN10 = new MatchMinMax();
        nodeN10.getChildrens().add(nodeN11);
        nodeN10.getChildrens().add(new Constant("4"));
        nodeN10.getChildrens().add(new Constant("4"));
        nodeN9.getChildrens().add(nodeN0);
        nodeN9.getChildrens().add(nodeN10);

        
        data.initReg = nodeN9;
        return data;
    }
    
    private static DataSet createDataset21() {
        DataSet data = new DataSet("Sample DataSet 21", "", "");
        String rawPositive = "0.0.0.0\n" +
"255.255.255.2\n" +
"192.168.0.136\n" +
"8.8.4.10";
        String rawNegative = "01.01.02.02\n" +
"00.00.00.00\n" +
"255.55.255.02\n" +
"192.168.00.136\n" +
"008.008.004.010\n" +
"256.257.255.1";
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
        
        //(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])(\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])){3}
        Node nodeN5 = new Anchor("\\d");
        Node nodeN4 = new MatchMinMax();
        nodeN4.getChildrens().add(nodeN5);
        nodeN4.getChildrens().add(new Constant("1"));
        nodeN4.getChildrens().add(new Constant("2"));
        Node nodeN7 = new Constant("1");
        Node nodeN8 = new Anchor("\\d");
        Node nodeN6 = new Concatenator();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(nodeN8);
        Node nodeN9 = new Concatenator();
        Node nodeN10 = new Anchor("\\d");
        nodeN9.getChildrens().add(nodeN6);
        nodeN9.getChildrens().add(nodeN10);
        Node nodeN3 = new Or();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(nodeN9);
        Node nodeN12 = new Constant("2");
        Node nodeN13 = new ListMatch();
        nodeN13.getChildrens().add(new RegexRange("0-4"));
        Node nodeN11 = new Concatenator();
        nodeN11.getChildrens().add(nodeN12);
        nodeN11.getChildrens().add(nodeN13);
        Node nodeN14 = new Concatenator();
        Node nodeN15 = new Anchor("\\d");
        nodeN14.getChildrens().add(nodeN11);
        nodeN14.getChildrens().add(nodeN15);
        Node nodeN2 = new Or();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN14);
        Node nodeN17 = new Constant("2");
        Node nodeN18 = new Constant("5");
        Node nodeN16 = new Concatenator();
        nodeN16.getChildrens().add(nodeN17);
        nodeN16.getChildrens().add(nodeN18);
        Node nodeN19 = new Concatenator();
        Node nodeN20 = new ListMatch();
        nodeN20.getChildrens().add(new RegexRange("0-5"));
        nodeN19.getChildrens().add(nodeN16);
        nodeN19.getChildrens().add(nodeN20);
        Node nodeN1 = new Or();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN19);
        Node nodeN23 = new Anchor("\\.");
        Node nodeN28 = new Anchor("\\d");
        Node nodeN27 = new MatchMinMax();
        nodeN27.getChildrens().add(nodeN28);
        nodeN27.getChildrens().add(new Constant("1"));
        nodeN27.getChildrens().add(new Constant("2"));
        Node nodeN30 = new Constant("1");
        Node nodeN31 = new Anchor("\\d");
        Node nodeN29 = new Concatenator();
        nodeN29.getChildrens().add(nodeN30);
        nodeN29.getChildrens().add(nodeN31);
        Node nodeN32 = new Concatenator();
        Node nodeN33 = new Anchor("\\d");
        nodeN32.getChildrens().add(nodeN29);
        nodeN32.getChildrens().add(nodeN33);
        Node nodeN26 = new Or();
        nodeN26.getChildrens().add(nodeN27);
        nodeN26.getChildrens().add(nodeN32);
        Node nodeN35 = new Constant("2");
        Node nodeN36 = new ListMatch();
        nodeN36.getChildrens().add(new RegexRange("0-4"));
        Node nodeN34 = new Concatenator();
        nodeN34.getChildrens().add(nodeN35);
        nodeN34.getChildrens().add(nodeN36);
        Node nodeN37 = new Concatenator();
        Node nodeN38 = new Anchor("\\d");
        nodeN37.getChildrens().add(nodeN34);
        nodeN37.getChildrens().add(nodeN38);
        Node nodeN25 = new Or();
        nodeN25.getChildrens().add(nodeN26);
        nodeN25.getChildrens().add(nodeN37);
        Node nodeN40 = new Constant("2");
        Node nodeN41 = new Constant("5");
        Node nodeN39 = new Concatenator();
        nodeN39.getChildrens().add(nodeN40);
        nodeN39.getChildrens().add(nodeN41);
        Node nodeN42 = new Concatenator();
        Node nodeN43 = new ListMatch();
        nodeN43.getChildrens().add(new RegexRange("0-5"));
        nodeN42.getChildrens().add(nodeN39);
        nodeN42.getChildrens().add(nodeN43);
        Node nodeN24 = new Or();
        nodeN24.getChildrens().add(nodeN25);
        nodeN24.getChildrens().add(nodeN42);
        Node nodeN22 = new Concatenator();
        nodeN22.getChildrens().add(nodeN23);
        nodeN22.getChildrens().add(nodeN24);
        Node nodeN21 = new MatchMinMax();
        nodeN21.getChildrens().add(nodeN22);
        nodeN21.getChildrens().add(new Constant("3"));
        nodeN21.getChildrens().add(new Constant("3"));
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN21);

        
        data.initReg = nodeN0;
        return data;
    }
    private static DataSet createDataset22() {
        DataSet data = new DataSet("Sample DataSet 22", "", "");
        String rawPositive = "9999999\n" +
"99999.99999\n" +
"99,999,999.9999\n" +
"123,456,789.9\n" +
"954,652,000.03";
        String rawNegative = "9999.\n" +
"9,99,99999.999\n" +
"999.9999.9999\n" +
"100,000,0\n" +
"100,00\n" +
"200,999,00";
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
        
        //(((\d{1,3})(,\d{3})*)|(\d+))(.\d+)?
        Node nodeN4 = new Anchor("\\d");
        Node nodeN3 = new MatchMinMax();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(new Constant("1"));
        nodeN3.getChildrens().add(new Constant("3"));
        Node nodeN7 = new Constant(",");
        Node nodeN9 = new Anchor("\\d");
        Node nodeN8 = new MatchMinMax();
        nodeN8.getChildrens().add(nodeN9);
        nodeN8.getChildrens().add(new Constant("3"));
        nodeN8.getChildrens().add(new Constant("3"));
        Node nodeN6 = new Concatenator();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(nodeN8);
        Node nodeN5 = new MatchZeroOrMore();
        nodeN5.getChildrens().add(nodeN6);
        Node nodeN2 = new Concatenator();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN5);
        Node nodeN11 = new Anchor("\\d");
        Node nodeN10 = new MatchOneOrMore();
        nodeN10.getChildrens().add(nodeN11);
        Node nodeN1 = new Or();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN10);
        Node nodeN14 = new Anchor(".");
        Node nodeN16 = new Anchor("\\d");
        Node nodeN15 = new MatchOneOrMore();
        nodeN15.getChildrens().add(nodeN16);
        Node nodeN13 = new Concatenator();
        nodeN13.getChildrens().add(nodeN14);
        nodeN13.getChildrens().add(nodeN15);
        Node nodeN12 = new MatchZeroOrOne();
        nodeN12.getChildrens().add(nodeN13);
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN12);

        
        data.initReg = nodeN0;
        return data;
    }
    private static DataSet createDataset23() {
        DataSet data = new DataSet("Sample DataSet 23", "", "");
        String rawPositive = "333-22-4444\n" +
"123-45-6789\n" +
"078-05-1120\n" +
"678-15-2200\n" +
"772-10-1600";
        String rawNegative = "773-00-0000\n" +
"780-22-4444\n" +
"999-88-7777\n" +
"800-05-1120\n" +
"123456789\n" +
"078051120\n" +
"333224444\n" +
"333 22 4444\n" +
"SSN";
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
        
        //\d{3}-\d{2}-\d{4}
        Node nodeN2 = new Anchor("\\d");
        Node nodeN1 = new MatchMinMax();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(new Constant("3"));
        nodeN1.getChildrens().add(new Constant("3"));
        Node nodeN3 = new Constant("-");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN3);
        Node nodeN4 = new Concatenator();
        Node nodeN6 = new Anchor("\\d");
        Node nodeN5 = new MatchMinMax();
        nodeN5.getChildrens().add(nodeN6);
        nodeN5.getChildrens().add(new Constant("2"));
        nodeN5.getChildrens().add(new Constant("2"));
        nodeN4.getChildrens().add(nodeN0);
        nodeN4.getChildrens().add(nodeN5);
        Node nodeN7 = new Concatenator();
        Node nodeN8 = new Constant("-");
        nodeN7.getChildrens().add(nodeN4);
        nodeN7.getChildrens().add(nodeN8);
        Node nodeN9 = new Concatenator();
        Node nodeN11 = new Anchor("\\d");
        Node nodeN10 = new MatchMinMax();
        nodeN10.getChildrens().add(nodeN11);
        nodeN10.getChildrens().add(new Constant("4"));
        nodeN10.getChildrens().add(new Constant("4"));
        nodeN9.getChildrens().add(nodeN7);
        nodeN9.getChildrens().add(nodeN10);

        
        data.initReg = nodeN9;
        return data;
    }
    
    private static DataSet createDataset24() {
        DataSet data = new DataSet("Sample DataSet 24", "", "");
        String rawPositive = "+97150 3827741\n" +
"0503827741\n" +
"050-3827741\n" +
"+97155 3827741\n" +
"0553827741\n" +
"055-3827741";
        String rawNegative = "040 3827741\n" +
"05 3827741\n" +
"050        3827741\n" +
"0563827741\n" +
"0-50-3827741\n" +
"055-382774";
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
        
        //(\+97[\s]{0,1}[\-]{0,1}[\s]{0,1}1|0)50[\s]{0,1}[\-]{0,1}[\s]{0,1}[1-9]{1}[0-9]{6}
        Node nodeN3 = new Anchor("\\+");
        Node nodeN4 = new Constant("9");
        Node nodeN2 = new Concatenator();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN4);
        Node nodeN5 = new Concatenator();
        Node nodeN6 = new Constant("7");
        nodeN5.getChildrens().add(nodeN2);
        nodeN5.getChildrens().add(nodeN6);
        Node nodeN7 = new Concatenator();
        Node nodeN9 = new ListMatch();
        nodeN9.getChildrens().add(new RegexRange("\\s"));
        Node nodeN8 = new MatchMinMax();
        nodeN8.getChildrens().add(nodeN9);
        nodeN8.getChildrens().add(new Constant("0"));
        nodeN8.getChildrens().add(new Constant("1"));
        nodeN7.getChildrens().add(nodeN5);
        nodeN7.getChildrens().add(nodeN8);
        Node nodeN10 = new Concatenator();
        Node nodeN12 = new ListMatch();
        nodeN12.getChildrens().add(new RegexRange("\\-"));
        Node nodeN11 = new MatchMinMax();
        nodeN11.getChildrens().add(nodeN12);
        nodeN11.getChildrens().add(new Constant("0"));
        nodeN11.getChildrens().add(new Constant("1"));
        nodeN10.getChildrens().add(nodeN7);
        nodeN10.getChildrens().add(nodeN11);
        Node nodeN13 = new Concatenator();
        Node nodeN15 = new ListMatch();
        nodeN15.getChildrens().add(new RegexRange("\\s"));
        Node nodeN14 = new MatchMinMax();
        nodeN14.getChildrens().add(nodeN15);
        nodeN14.getChildrens().add(new Constant("0"));
        nodeN14.getChildrens().add(new Constant("1"));
        nodeN13.getChildrens().add(nodeN10);
        nodeN13.getChildrens().add(nodeN14);
        Node nodeN16 = new Concatenator();
        Node nodeN17 = new Constant("1");
        nodeN16.getChildrens().add(nodeN13);
        nodeN16.getChildrens().add(nodeN17);
        Node nodeN18 = new Constant("0");
        Node nodeN1 = new Or();
        nodeN1.getChildrens().add(nodeN16);
        nodeN1.getChildrens().add(nodeN18);
        Node nodeN19 = new Constant("5");
        Node nodeN0 = new Concatenator();
        nodeN0.getChildrens().add(nodeN1);
        nodeN0.getChildrens().add(nodeN19);
        Node nodeN20 = new Concatenator();
        Node nodeN21 = new Constant("0");
        nodeN20.getChildrens().add(nodeN0);
        nodeN20.getChildrens().add(nodeN21);
        Node nodeN22 = new Concatenator();
        Node nodeN24 = new ListMatch();
        nodeN24.getChildrens().add(new RegexRange("\\s"));
        Node nodeN23 = new MatchMinMax();
        nodeN23.getChildrens().add(nodeN24);
        nodeN23.getChildrens().add(new Constant("0"));
        nodeN23.getChildrens().add(new Constant("1"));
        nodeN22.getChildrens().add(nodeN20);
        nodeN22.getChildrens().add(nodeN23);
        Node nodeN25 = new Concatenator();
        Node nodeN27 = new ListMatch();
        nodeN27.getChildrens().add(new RegexRange("\\-"));
        Node nodeN26 = new MatchMinMax();
        nodeN26.getChildrens().add(nodeN27);
        nodeN26.getChildrens().add(new Constant("0"));
        nodeN26.getChildrens().add(new Constant("1"));
        nodeN25.getChildrens().add(nodeN22);
        nodeN25.getChildrens().add(nodeN26);
        Node nodeN28 = new Concatenator();
        Node nodeN30 = new ListMatch();
        nodeN30.getChildrens().add(new RegexRange("\\s"));
        Node nodeN29 = new MatchMinMax();
        nodeN29.getChildrens().add(nodeN30);
        nodeN29.getChildrens().add(new Constant("0"));
        nodeN29.getChildrens().add(new Constant("1"));
        nodeN28.getChildrens().add(nodeN25);
        nodeN28.getChildrens().add(nodeN29);
        Node nodeN31 = new Concatenator();
        Node nodeN33 = new ListMatch();
        nodeN33.getChildrens().add(new RegexRange("1-9"));
        Node nodeN32 = new MatchMinMax();
        nodeN32.getChildrens().add(nodeN33);
        nodeN32.getChildrens().add(new Constant("1"));
        nodeN32.getChildrens().add(new Constant("1"));
        nodeN31.getChildrens().add(nodeN28);
        nodeN31.getChildrens().add(nodeN32);
        Node nodeN34 = new Concatenator();
        Node nodeN36 = new ListMatch();
        nodeN36.getChildrens().add(new RegexRange("0-9"));
        Node nodeN35 = new MatchMinMax();
        nodeN35.getChildrens().add(nodeN36);
        nodeN35.getChildrens().add(new Constant("6"));
        nodeN35.getChildrens().add(new Constant("6"));
        nodeN34.getChildrens().add(nodeN31);
        nodeN34.getChildrens().add(nodeN35);

        
        data.initReg = nodeN34;
        return data;
    }
    
    private static DataSet createDataset25() {
        DataSet data = new DataSet("Sample DataSet 25", "", "");
        String rawPositive = "6011-1111-1111-1111\n" +
"5423-1111-1111-1111\n" +
"341111111111111\n" +
"5423 1111 1111 1111\n" +
"6011 1111 1234 5678";
        String rawNegative = "4111-111-111-111\n" +
"4111 111 111 111\n" +
"3411-1111-1111-111\n" +
"Visa\n" +
"12345678910\n" +
"0000000000000000";
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
        
        //((4\d{3})|(5[1-5]\d{2})|(6011))-?\d{4}-?\d{4}-?\d{4}|3[4,7]\d{13}
        Node nodeN5 = new Constant("4");
        Node nodeN7 = new Anchor("\\d");
        Node nodeN6 = new MatchMinMax();
        nodeN6.getChildrens().add(nodeN7);
        nodeN6.getChildrens().add(new Constant("3"));
        nodeN6.getChildrens().add(new Constant("3"));
        Node nodeN4 = new Concatenator();
        nodeN4.getChildrens().add(nodeN5);
        nodeN4.getChildrens().add(nodeN6);
        Node nodeN9 = new Constant("5");
        Node nodeN10 = new ListMatch();
        nodeN10.getChildrens().add(new RegexRange("1-5"));
        Node nodeN8 = new Concatenator();
        nodeN8.getChildrens().add(nodeN9);
        nodeN8.getChildrens().add(nodeN10);
        Node nodeN11 = new Concatenator();
        Node nodeN13 = new Anchor("\\d");
        Node nodeN12 = new MatchMinMax();
        nodeN12.getChildrens().add(nodeN13);
        nodeN12.getChildrens().add(new Constant("2"));
        nodeN12.getChildrens().add(new Constant("2"));
        nodeN11.getChildrens().add(nodeN8);
        nodeN11.getChildrens().add(nodeN12);
        Node nodeN3 = new Or();
        nodeN3.getChildrens().add(nodeN4);
        nodeN3.getChildrens().add(nodeN11);
        Node nodeN15 = new Constant("6");
        Node nodeN16 = new Constant("0");
        Node nodeN14 = new Concatenator();
        nodeN14.getChildrens().add(nodeN15);
        nodeN14.getChildrens().add(nodeN16);
        Node nodeN17 = new Concatenator();
        Node nodeN18 = new Constant("1");
        nodeN17.getChildrens().add(nodeN14);
        nodeN17.getChildrens().add(nodeN18);
        Node nodeN19 = new Concatenator();
        Node nodeN20 = new Constant("1");
        nodeN19.getChildrens().add(nodeN17);
        nodeN19.getChildrens().add(nodeN20);
        Node nodeN2 = new Or();
        nodeN2.getChildrens().add(nodeN3);
        nodeN2.getChildrens().add(nodeN19);
        Node nodeN22 = new Constant("-");
        Node nodeN21 = new MatchZeroOrOne();
        nodeN21.getChildrens().add(nodeN22);
        Node nodeN1 = new Concatenator();
        nodeN1.getChildrens().add(nodeN2);
        nodeN1.getChildrens().add(nodeN21);
        Node nodeN23 = new Concatenator();
        Node nodeN25 = new Anchor("\\d");
        Node nodeN24 = new MatchMinMax();
        nodeN24.getChildrens().add(nodeN25);
        nodeN24.getChildrens().add(new Constant("4"));
        nodeN24.getChildrens().add(new Constant("4"));
        nodeN23.getChildrens().add(nodeN1);
        nodeN23.getChildrens().add(nodeN24);
        Node nodeN26 = new Concatenator();
        Node nodeN28 = new Constant("-");
        Node nodeN27 = new MatchZeroOrOne();
        nodeN27.getChildrens().add(nodeN28);
        nodeN26.getChildrens().add(nodeN23);
        nodeN26.getChildrens().add(nodeN27);
        Node nodeN29 = new Concatenator();
        Node nodeN31 = new Anchor("\\d");
        Node nodeN30 = new MatchMinMax();
        nodeN30.getChildrens().add(nodeN31);
        nodeN30.getChildrens().add(new Constant("4"));
        nodeN30.getChildrens().add(new Constant("4"));
        nodeN29.getChildrens().add(nodeN26);
        nodeN29.getChildrens().add(nodeN30);
        Node nodeN32 = new Concatenator();
        Node nodeN34 = new Constant("-");
        Node nodeN33 = new MatchZeroOrOne();
        nodeN33.getChildrens().add(nodeN34);
        nodeN32.getChildrens().add(nodeN29);
        nodeN32.getChildrens().add(nodeN33);
        Node nodeN35 = new Concatenator();
        Node nodeN37 = new Anchor("\\d");
        Node nodeN36 = new MatchMinMax();
        nodeN36.getChildrens().add(nodeN37);
        nodeN36.getChildrens().add(new Constant("4"));
        nodeN36.getChildrens().add(new Constant("4"));
        nodeN35.getChildrens().add(nodeN32);
        nodeN35.getChildrens().add(nodeN36);
        Node nodeN39 = new Constant("3");
        Node nodeN40 = new ListMatch();
        nodeN40.getChildrens().add(new RegexRange("47,"));
        Node nodeN38 = new Concatenator();
        nodeN38.getChildrens().add(nodeN39);
        nodeN38.getChildrens().add(nodeN40);
        Node nodeN41 = new Concatenator();
        Node nodeN43 = new Anchor("\\d");
        Node nodeN42 = new MatchMinMax();
        nodeN42.getChildrens().add(nodeN43);
        nodeN42.getChildrens().add(new Constant("13"));
        nodeN42.getChildrens().add(new Constant("13"));
        nodeN41.getChildrens().add(nodeN38);
        nodeN41.getChildrens().add(nodeN42);
        Node nodeN0 = new Or();
        nodeN0.getChildrens().add(nodeN35);
        nodeN0.getChildrens().add(nodeN41);

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
