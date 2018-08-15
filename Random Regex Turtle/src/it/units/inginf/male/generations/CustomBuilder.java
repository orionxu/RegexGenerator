/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.inginf.male.generations;

import it.units.inginf.male.configuration.Configuration;
import it.units.inginf.male.inputs.Context;
import it.units.inginf.male.inputs.DataSet;
import it.units.inginf.male.tree.Node;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author orion
 */
public class CustomBuilder implements InitialPopulationBuilder{
    public static final boolean USE_CUSTOM = true;
    private List<Node> population = new LinkedList<>();
    
    @Override
    public List<Node> init(){
        return new LinkedList<Node>(population);
    }
    
    
    /**
     * Returns a copy of the shared population list
     * @param context
     * @return
     */
    public List<Node> init(Context context) {
        return this.setup(context.getConfiguration(), context.getCurrentDataSet());
    }

    /**
     * Updates the shared population object (into main configuration) 
     * @param configuration
     */
    public void setup(Configuration configuration) {
        DataSet trainingDataset = configuration.getDatasetContainer().getTrainingDataset();
        this.population.addAll(this.setup(configuration, trainingDataset));
    }
    
    private List<Node> setup(Configuration configuration, DataSet usedTrainingDataset) {
        List<Node> result = new ArrayList<Node>();
        System.out.println(usedTrainingDataset.initReg);
        result.add(usedTrainingDataset.initReg);
        return result;
    }
}
