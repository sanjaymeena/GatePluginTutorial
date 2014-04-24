/**
 * A standalone application that makes use of GATE PR's as well as a user defined one.
 * The program displays the features of each document as created by the PR "Vinci".
 *
 * @author Sanjay meena (sanjaymeena@gmail.com)
 *         -- last updated 20/04/2014
 */
package com.gate.plugin.vinci;

import gate.Annotation;
import gate.AnnotationSet;
import gate.FeatureMap;
import gate.Gate;
import gate.Document;
import gate.util.GateException;
import gate.Factory;
import gate.creole.ANNIEConstants;
import gate.creole.SerialAnalyserController;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.io.File;

import org.apache.commons.lang.time.StopWatch;

public class TotalVinciCount {

	private gate.Corpus corpus;

	public TotalVinciCount(String[] files) throws Exception {
		Gate.init();

		/**
		 * Register directories where plugins are kept . In this demo there are
		 * two directories. User directory where Vinci plugin is kept and
		 * directory for Gate Plugin - Annie
		 */

		// need resource data for Vinci
		Gate.getCreoleRegister().registerDirectories(
				new File(System.getProperty("user.dir")).toURL());
		// need ANNIE plugin for the Defaulttokeniser and SentenceSplitter
		Gate.getCreoleRegister().registerDirectories(
				new File(Gate.getPluginsHome(), ANNIEConstants.PLUGIN_DIR)
						.toURL());


		// add files to a corpus
		System.out.println("\n== OBTAINING DOCUMENTS ==");
		createCorpus(files);

		System.out.println("\n== USING GATE TO PROCESS THE DOCUMENTS ==");
		String[] processingResources = {
				"gate.creole.tokeniser.DefaultTokeniser",
				"gate.creole.splitter.SentenceSplitter",
				"com.gate.plugin.vinci.Vinci" };
		runProcessingResources(processingResources);

		System.out.println("\n== DOCUMENT FEATURES ==");
		displayDocumentFeatures();

		System.out.println("\nDemo done... :)");
	}

	private void createCorpus(String[] files) throws GateException {
		corpus = Factory.newCorpus("Transient Gate Corpus");

		for (int file = 0; file < files.length; file++) {
			System.out.print("\t " + (file + 1) + ") " + files[file]);
			try {
				corpus.add(Factory.newDocument(new File(files[file]).toURL()));
				System.out.println(" -- success");
			} catch (gate.creole.ResourceInstantiationException e) {
				System.out.println(" -- failed (" + e.getMessage() + ")");
			} catch (Exception e) {
				System.out.println(" -- " + e.getMessage());
			}
		}
	}

	private void runProcessingResources(String[] processingResource)
			throws GateException {
		SerialAnalyserController pipeline = (SerialAnalyserController) Factory
				.createResource("gate.creole.SerialAnalyserController");

		for (int pr = 0; pr < processingResource.length; pr++) {
			System.out.print("\t* Loading " + processingResource[pr] + " ... ");
			pipeline.add((gate.LanguageAnalyser) Factory
					.createResource(processingResource[pr]));
			System.out.println("done");
		}

		System.out.print("Creating corpus from documents obtained...");
		pipeline.setCorpus(corpus);
		System.out.println("done");

		System.out.print("Running processing resources over corpus...");
		pipeline.execute();
		System.out.println("done");
	}

	private void displayDocumentFeatures() {
		Iterator<Document> documentIterator = corpus.iterator();

		while (documentIterator.hasNext()) {
			Document currDoc = documentIterator.next();
			System.out.println("The features of document \""
					+ currDoc.getSourceUrl().getFile() + "\" are:");
			gate.FeatureMap documentFeatures = currDoc.getFeatures();
			AnnotationSet annotationSet = currDoc.getAnnotations("");

			Iterator featureIterator = documentFeatures.keySet().iterator();
			while (featureIterator.hasNext()) {
				String key = (String) featureIterator.next();
				System.out.println("\t*) " + key + " --> "
						+ documentFeatures.get(key));
			}

			List<Annotation> sentences = gate.Utils
					.inDocumentOrder(annotationSet
							.get(ANNIEConstants.SENTENCE_ANNOTATION_TYPE));

			for (Annotation annotation : sentences) {

				System.out.println("*****Sentence features**********");
				FeatureMap sentenceFeatureMap = annotation.getFeatures();
				for (Entry<Object, Object> entry : sentenceFeatureMap
						.entrySet()) {
					System.out.println((String) entry.getKey() + " : "
							+ entry.getValue());
				}

			}

			System.out.println();
		}
	}

	public static void main(String[] args) {
		StopWatch sw = new StopWatch();
		sw.start();
		if (args.length == 0)
			System.err
					.println("USAGE: java TotalVinciCount <file1> <file2> ...");
		else
			try {
				new TotalVinciCount(args);
				sw.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}

		System.out.println("Time: " + sw.toString());
	}
}