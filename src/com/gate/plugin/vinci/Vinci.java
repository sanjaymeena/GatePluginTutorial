
/**
 * This class is the Vinci Plugin for Gate

@author sanjay_meena
 */
package com.gate.plugin.vinci;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Resource;
import gate.creole.ANNIEConstants;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.gate.plugin.vinci.datastructure.ParagraphNode;
import com.gate.plugin.vinci.datastructure.SentenceNode;
import com.gate.plugin.vinci.datastructure.TextNode;

/**
 * This class is the implementation of the for the Vinci Plugin
 */
@CreoleResource(name = "Vinci Plugin for Gate", comment = "Example CREOLE plugin for gate. This plugin illustrates some common issues which are encountered while createing a gate plugin ")
public class Vinci extends AbstractLanguageAnalyser {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9137266679426024596L;
	private String inputAnnotationsName, outputAnnotationsName;
	private Document gateDocument;

	/* CREOLE parameters: what are we going to annotate, and how? */
	private String inputSentenceType;
	protected String annotationSetName;
	private String inputTokenType;
	private String vinciToken = "vinci";

	private int documentTokenCount = 0;

	/**
	 * Initialize the Parser resource. In particular, load the trained data
	 * file.
	 */
	@Override
	public Resource init() throws ResourceInstantiationException {
		fireStatusChanged("Creating instance of Vinci Plugin");

		return this;
	}

	@Override
	public void execute() throws ExecutionException {

		// get the gate document.
		gateDocument = getDocument();

		TextNode textNode = createTextStructure();

		try {
			addVinciCountToDocument(textNode);
		} catch (InvalidOffsetException e) {

			e.printStackTrace();
		}

	}

	private TextNode createTextStructure() {

		TextNode textNode = new TextNode();

		long paraStartOffset;
		long paraEndOffSet;

		// Get the content of this document
		String textdocument = gateDocument.getContent().toString();

		// get all annotations for this document
		AnnotationSet allAnnotations = gateDocument
				.getAnnotations(annotationSetName);

		// get paragraphs from text;
		AnnotationSet paragraphAnnotationsSet = gateDocument.getAnnotations(
				Gate.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("paragraph");

		// Sort all the paragraph by their order of appearance
		List<Annotation> paragraphList = new ArrayList<Annotation>(
				paragraphAnnotationsSet);
		Collections.sort(paragraphList, new gate.util.OffsetComparator());

		HashMap<Integer, ParagraphNode> paragraphMap = new HashMap<Integer, ParagraphNode>();

		// iterate over paragraph annotations
		for (Annotation annotation : paragraphList) {
			paraStartOffset = annotation.getStartNode().getOffset();
			paraEndOffSet = annotation.getEndNode().getOffset();

			ParagraphNode para = new ParagraphNode();
			HashMap<Integer, SentenceNode> sourceSentenceMap = new HashMap<Integer, SentenceNode>();
			String paragraphContent;
			int paragraphNo;

			// Get content of a paragraph;
			paragraphContent = gate.Utils.contentFor(gateDocument, annotation)
					.toString();
			// get id of paragraph annotation
			paragraphNo = annotation.getId();

			AnnotationSet sentences = allAnnotations.get(inputSentenceType)
					.get(paraStartOffset, paraEndOffSet);

			// Sort all the sentences by their order of appearance
			List<Annotation> sentenceList = new ArrayList<Annotation>(sentences);
			Collections.sort(sentenceList, new gate.util.OffsetComparator());
			for (Annotation sentence : sentenceList) {

				SentenceNode sn = new SentenceNode();
				String sentenceContent;
				int sentenceId;

				sentenceId = sentence.getId();
				sentenceContent = gate.Utils.contentFor(gateDocument, sentence)
						.toString();
				sn.setContent(sentenceContent);
				sn.setSentenceNumber(sentenceId);
				sn.setParagraphNumber(paragraphNo);

				sourceSentenceMap.put(sentenceId, sn);
			}

			para.setContent(paragraphContent);
			para.setParagraphNumber(paragraphNo);
			para.setSourceSentenceMap(sourceSentenceMap);

			paragraphMap.put(paragraphNo, para);
		}

		textNode.setParagraphMap(paragraphMap);
		textNode.setContent(textdocument);

		return textNode;
	}

	/**
	 * Find all the Sentence annotations and iterate through them, parsing one
	 * sentence at a time and storing the result in the output AS. (Sentences
	 * are scanned for Tokens. You have to run the ANNIE tokenizer and splitter
	 * before this PR.)
	 * 
	 * @param textNode
	 * @throws ExecutionException
	 * @throws InvalidOffsetException
	 */
	public void addVinciCountToDocument(TextNode textNode)
			throws ExecutionException, InvalidOffsetException {

		long paraStartOffset;
		long paraEndOffSet;

		// Get all annotations in the document
		AnnotationSet allAnnotations = gateDocument
				.getAnnotations(annotationSetName);

		// Get paragraphs Annotations from the Gate document;
		AnnotationSet paragraphAnnotationsSet = gateDocument.getAnnotations(
				Gate.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("paragraph");

		// Iterate over all paragraphs Annotations
		for (Annotation paragraphAnnotation : paragraphAnnotationsSet) {

			// get start offset of this paragraph annotation
			paraStartOffset = paragraphAnnotation.getStartNode().getOffset();

			// get end offset of this paragraph annotation
			paraEndOffSet = paragraphAnnotation.getEndNode().getOffset();

			FeatureMap paragraph_features = Factory.newFeatureMap();

			int paragraphNo;

			paragraphNo = paragraphAnnotation.getId();

			ParagraphNode para = textNode.getParagraphMap().get(paragraphNo);

			// get the sentence annotation set in Gate.
			AnnotationSet sentences = allAnnotations.get(inputSentenceType)
					.get(paraStartOffset, paraEndOffSet);

			// Iterate over each sentence in the sentence annotation set

			for (Annotation sentence : sentences) {

				String sentenceContent;
				int sentenceId;

				sentenceId = sentence.getId();
				sentenceContent = gate.Utils.contentFor(gateDocument, sentence)
						.toString();

				SentenceNode sentenceNode = para.getSourceSentenceMap().get(
						sentenceId);

				/**
				 * 
				 */
				addVinciCount(sentenceNode, sentence, allAnnotations);

				FeatureMap sentence_features = Factory.newFeatureMap();
				sentence_features.put("content", sentenceContent);
				sentence_features.put("vincicount",
						sentenceNode.getVinciCount());
				sentence_features.put("paragraphNo",
						sentenceNode.getParagraphNumber());
				sentence.setFeatures(sentence_features);

			}

			paragraph_features.put("content", para.getContent());
			// paragraph_features.put("vinciTokenCount", documentTokenCount);
			paragraphAnnotation.setFeatures(paragraph_features);

		}
		gateDocument.getFeatures().put("vinciCount", documentTokenCount);

	}

	/**
	 * 
	 * 
	 * @param sentenceNode
	 * @param sentence
	 * @param allAnnotations
	 * @throws InvalidOffsetException
	 */
	private void addVinciCount(SentenceNode sentenceNode, Annotation sentence,
			AnnotationSet allAnnotations) throws InvalidOffsetException {

		int tokenCount = 0;
		// get sentence offsets
		long sentenceStartOffset = sentence.getStartNode().getOffset();
		long sentenceEndOffset = sentence.getEndNode().getOffset();

		// Get token annotation set.

		AnnotationSet tokenAnnotationSet = allAnnotations.get(inputTokenType,
				sentenceStartOffset, sentenceEndOffset);

		// get tokens list for sentence and sort them in their order of
		// appearance.
		List<Annotation> tokenList = new ArrayList<Annotation>(
				tokenAnnotationSet);
		Collections.sort(tokenList, new gate.util.OffsetComparator());

		Iterator<Annotation> tokenIter = tokenList.iterator();

		while (tokenIter.hasNext()) {
			Annotation token = tokenIter.next();

			if (token.getType().equals(inputTokenType)) {

				String tokenString = gate.Utils.contentFor(gateDocument, token)
						.toString();
				if (tokenString.toLowerCase().equals(vinciToken)) {
					tokenCount++;
				}

			}
		}

		sentenceNode.setVinciCount(tokenCount);
		documentTokenCount += tokenCount;
	}

	@RunTime
	@CreoleParameter(comment = "input annotation type for each sentence", defaultValue = ANNIEConstants.SENTENCE_ANNOTATION_TYPE)
	public void setInputSentenceType(String sType) {
		this.inputSentenceType = sType;
	}

	public String getInputSentenceType() {
		return this.inputSentenceType;
	}

	@RunTime
	@CreoleParameter(comment = "input annotation type for each token", defaultValue = ANNIEConstants.TOKEN_ANNOTATION_TYPE)
	public void setInputTokenType(String tType) {
		this.inputTokenType = tType;
	}

	public String getInputTokenType() {
		return this.inputTokenType;
	}

	public String getAnnotationSetName() {
		return annotationSetName;
	}

	public String getinputASname() {
		return inputAnnotationsName;
	}

	public void setinputASname(String inputASname) {
		this.inputAnnotationsName = inputASname;
	}

	public String getoutputASname() {
		return outputAnnotationsName;
	}

	public void setoutputASname(String outputASname) {
		this.outputAnnotationsName = outputASname;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "annotationSet used for input (Token and "
			+ "Sentence annotations) and output")
	public void setAnnotationSetName(String annotationSetName) {
		this.annotationSetName = annotationSetName;
	}
}