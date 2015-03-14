package de.ugoe.cs.cpdp.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import de.ugoe.cs.util.FileTools;


class CSVMockusDataLoader implements SingleVersionLoader {

	@Override
	public Instances load(File file) {
		final String[] lines;
		try {
			
			lines = FileTools.getLinesFromFile(file.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
		// configure Instances
		final ArrayList<Attribute> atts = new ArrayList<Attribute>();

		String[] lineSplit = lines[0].split(",");
		for (int j = 0; j < lineSplit.length - 3; j++) {
			atts.add(new Attribute(lineSplit[j + 2]));
		}
		
		final ArrayList<String> classAttVals = new ArrayList<String>();
		classAttVals.add("0");
		classAttVals.add("1");
		final Attribute classAtt = new Attribute("bug", classAttVals);
		atts.add(classAtt);

		final Instances data = new Instances(file.getName(), atts, 0);
		data.setClass(classAtt);

		// fetch data
		for (int i = 1; i < lines.length; i++) {
			lineSplit = lines[i].split(",");
			double[] values = new double[lineSplit.length - 2];
			for (int j = 0; j < values.length - 1; j++) {
				values[j] = Double.parseDouble(lineSplit[j + 2].trim());
			}
			values[values.length - 1] = lineSplit[lineSplit.length - 1].trim()
					.equals("0") ? 0 : 1;
			data.add(new DenseInstance(1.0, values));
		}

		return data;
	}

	@Override
	public boolean filenameFilter(String filename) {
		return filename.endsWith(".csv");
	}

}

