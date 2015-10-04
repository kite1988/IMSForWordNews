/**
 * IMS (It Makes Sense) -- NUS WSD System
 * Copyright (c) 2010 National University of Singapore.
 * All Rights Reserved.
 */
package sg.edu.nus.comp.nlp.ims.feature;

import java.util.ArrayList;

/**
 * Feature extractor with combined extractors, including Senna Context Feature Extractor
 */
public class CAllWordsFeatureExtractorCombinationWithSenna extends CFeatureExtractorCombination {
	public CAllWordsFeatureExtractorCombinationWithSenna() {
		this.m_FeatureExtractors.clear();
		this.m_FeatureExtractors.add(new CPOSFeatureExtractor());
		this.m_FeatureExtractors.add(new CCollocationExtractor());
		this.m_FeatureExtractors.add(new CSurroundingWordExtractor(1, 1));
		this.m_FeatureExtractors.add(new CSennaContextSumFeatureExtractor());
	}

	public CAllWordsFeatureExtractorCombinationWithSenna(
			ArrayList<IFeatureExtractor> p_FeatureExtractors) {
		super(p_FeatureExtractors);
	}
}
