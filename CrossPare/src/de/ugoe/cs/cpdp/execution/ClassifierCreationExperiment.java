package de.ugoe.cs.cpdp.execution;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import weka.core.Instances;
import de.ugoe.cs.cpdp.ExperimentConfiguration;
import de.ugoe.cs.cpdp.dataprocessing.IProcessesingStrategy;
import de.ugoe.cs.cpdp.dataselection.IPointWiseDataselectionStrategy;
import de.ugoe.cs.cpdp.eval.IEvaluationStrategy;
import de.ugoe.cs.cpdp.loader.IVersionLoader;
import de.ugoe.cs.cpdp.training.ITrainer;
import de.ugoe.cs.cpdp.training.ITrainingStrategy;
import de.ugoe.cs.cpdp.training.IWekaCompatibleTrainer;
import de.ugoe.cs.cpdp.versions.SoftwareVersion;
import de.ugoe.cs.util.console.Console;

/**
 * Class responsible for executing an experiment according to an {@link ExperimentConfiguration}. The steps 
 * of this ClassifierCreationExperiment are as follows:
 * <ul>
 *  <li>load the data from the provided data path</li>
 *  <li>check if given resultsdir exists, if not create one</li>
 *  <li>execute the following steps for each data set:
 *  <ul>
 *   <li>load the dataset</li>
 *   <li>set testdata == traindata</li>
 *   <li>preprocess the data</li>
 *   <li>postprocess the data</li>
 *   <li>for each configured trainer do the following:</li>
 *   <ul>
 *   	<li>if the classifier should be saved, train it with the dataset</li>
 *   	<li>save it in the results dir</li>
 *   	<li>For each configured evaluator: Do the evaluation and save results</li>
 *   </ul>
 *  </ul>
 * </ul>
 *   
 * Note that this class implements {@link IExectuionStrategy}, i.e., each experiment can be started 
 * in its own thread.
 * 
 * @author Fabian Trautsch
 */
public class ClassifierCreationExperiment implements IExecutionStrategy {

	/**
	 * configuration of the experiment
	 */
	private final ExperimentConfiguration config;
	
	/**
	 * Constructor. Creates a new experiment based on a configuration.
	 * @param config configuration of the experiment
	 */
	public ClassifierCreationExperiment(ExperimentConfiguration config) {
		this.config = config;
	}
	
	/**
	 * Executes the experiment with the steps as described in the class comment.
	 * @see Runnable#run() 
	 */
	@Override
	public void run() {
		final List<SoftwareVersion> versions = new LinkedList<>();
		
		boolean writeHeader = true;
		
		for(IVersionLoader loader : config.getLoaders()) {
			versions.addAll(loader.load());
		}
	

		File resultsDir = new File(config.getResultsPath());
		if (!resultsDir.exists()) {
			resultsDir.mkdir();
		}
		
		
		int versionCount = 1;
		for( SoftwareVersion testVersion : versions ) {
			
			// At first: traindata == testdata
			Instances testdata = testVersion.getInstances();
			Instances traindata = new Instances(testdata);
			
			// Give the dataset a new name
			testdata.setRelationName(testVersion.getProject());
			
			for( IProcessesingStrategy processor : config.getPreProcessors() ) {
				Console.traceln(Level.FINE, String.format("[%s] [%02d/%02d] %s: applying preprocessor %s", config.getExperimentName(), versionCount, versions.size(), testVersion.getProject(), processor.getClass().getName()));
				processor.apply(testdata, traindata);
			}
			
			for( IPointWiseDataselectionStrategy dataselector : config.getPointWiseSelectors() ) {
				Console.traceln(Level.FINE, String.format("[%s] [%02d/%02d] %s: applying pointwise selection %s", config.getExperimentName(), versionCount, versions.size(), testVersion.getProject(), dataselector.getClass().getName()));
				traindata = dataselector.apply(testdata, traindata);
			}
			
			for( IProcessesingStrategy processor : config.getPostProcessors() ) {
				Console.traceln(Level.FINE, String.format("[%s] [%02d/%02d] %s: applying setwise postprocessor %s", config.getExperimentName(), versionCount, versions.size(), testVersion.getProject(), processor.getClass().getName()));
				processor.apply(testdata, traindata);
			}
			
		
			
			
			// Get bug/non bug (TODO)
			int traindataBug = 0;
			int traindataNonBug = 0;
						
			for(int i=0; i<traindata.numInstances(); i++) {
				double wekaInstanceValue = traindata.instance(i).classValue();
							
				if(wekaInstanceValue == 1.0) {
					traindataBug++;
				} else {
					traindataNonBug++;
				}
			}
						
			System.out.println("Traindata Bug: "+traindataBug);
			System.out.println("Traindata Non Bug: "+traindataNonBug);
						
			int testdataBug = 0;
			int testdataNonBug = 0;
						
			for(int i=0; i<testdata.numInstances(); i++) {
				double wekaInstanceValue = testdata.instance(i).classValue();
							
				if(wekaInstanceValue == 1.0) {
					testdataBug++;
				} else {
					testdataNonBug++;
				}
			}
						
			System.out.println("Testdata Bug: "+testdataBug);
			System.out.println("Testdata Non Bug: "+testdataNonBug);

			
			// Trainerlist for evaluation later on
			List<ITrainer> allTrainers = new LinkedList<>();
			
			for( ITrainingStrategy trainer : config.getTrainers() ) {

				// Add trainer to list for evaluation
				allTrainers.add(trainer);
				
				// Train classifier
				trainer.apply(traindata);
				
				if(config.getSaveClassifier()) {
					// If classifier should be saved, train him and save him
					// be careful with typecasting here!
					IWekaCompatibleTrainer trainerToSave = (IWekaCompatibleTrainer) trainer;
					//Console.println(trainerToSave.getClassifier().toString());
					try {
						weka.core.SerializationHelper.write(resultsDir.getAbsolutePath()+"/"+trainer.getName()+"-"+testVersion.getProject(), trainerToSave.getClassifier());
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}
			
			
			
			for( IEvaluationStrategy evaluator : config.getEvaluators() ) {
				Console.traceln(Level.FINE, String.format("[%s] [%02d/%02d] %s: applying evaluator %s", config.getExperimentName(), versionCount, versions.size(), testVersion.getProject(), evaluator.getClass().getName()));

				if( writeHeader ) {
					evaluator.setParameter(config.getResultsPath() + "/" + config.getExperimentName() + ".csv");
				}
				evaluator.apply(testdata, traindata, allTrainers, writeHeader);
				writeHeader = false;
			}
			
			versionCount++;
			
			Console.traceln(Level.INFO, String.format("[%s] [%02d/%02d] %s: finished", config.getExperimentName(), versionCount, versions.size(), testVersion.getProject()));
			
		}
		
	}
	
}