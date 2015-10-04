package sg.edu.nus.comp.nlp.ims.feature;

import sg.edu.nus.comp.nlp.ims.corpus.AItem;
import sg.edu.nus.comp.nlp.ims.corpus.ICorpus;
import sg.edu.nus.comp.nlp.ims.corpus.IItem;
import sg.edu.nus.comp.nlp.ims.corpus.ISentence;
import sg.edu.nus.comp.nlp.ims.util.CSurroundingWordFilter;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.util.*;

public class CSennaContextFeatureExtractor implements IFeatureExtractor {
    // corpus to be extracted
    protected ICorpus m_Corpus = null;

    // index of current instance
    protected int m_Index = -1;

    // current sentence to process
    protected ISentence m_prevSentence = null;
    protected ISentence m_Sentence = null;
    protected ISentence m_nextSentence = null;

    // item index in current sentence
    protected int m_IndexInSentence;

    // item length
    protected int m_InstanceLength;


    // for extracting dimension values
    private int m_windowSize;
    private int m_windowIndex = -1 * m_windowSize; // initialise at the start of the window
    private int m_dimensionIndex = 0;


    // stop words filter
    protected CSurroundingWordFilter m_Filter = CSurroundingWordFilter.getInstance();

    // current feature
    protected IFeature m_CurrentFeature = null;

    // lemma index
    protected static int g_LIDX = AItem.Features.LEMMA.ordinal();

    // token index
    protected static int g_TIDX = AItem.Features.TOKEN.ordinal();

    private SennaWordEmbeddings model = SennaWordEmbeddings.instance();
    private static Map<String, String> tokenizationDifferencesMap = new HashMap<>();
    private static Set<String> tokensToSkip = new HashSet<>();
    private int wordsIgnoredForCurrentTargetWord;

    static {
        tokenizationDifferencesMap.put("-lrb-", "(");
        tokenizationDifferencesMap.put("-rrb-", ")");
        tokenizationDifferencesMap.put("-lsb-", "[");
        tokenizationDifferencesMap.put("-rsb-", "]");
        tokenizationDifferencesMap.put("-lcb-", "{");
        tokenizationDifferencesMap.put("-rcb-", "}");
    }

    /**
     * constructor
     */
    public CSennaContextFeatureExtractor() {
        this.m_windowSize = 9 / 2;
    }


    /*
     * (non-Javadoc)
     * @see sg.edu.nus.comp.nlp.ims.feature.IFeatureExtractor#getCurrentInstanceID()
     */
    @Override
    public String getCurrentInstanceID() {
        if (this.validIndex(this.m_Index)) {
            return this.m_Corpus.getValue(this.m_Index, "id");
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see sg.edu.nus.comp.nlp.ims.feature.IFeatureExtractor#hasNext()
     */
    @Override
    public boolean hasNext() {
        if (this.m_CurrentFeature != null) {
            return true;
        }
        if (this.validIndex(this.m_Index)) {
            this.m_CurrentFeature = this.getNext();
            if (this.m_CurrentFeature != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * get next feature
     *
     * @return feature
     */
    private IFeature getNext() {
        IFeature feature = null;

        boolean isAtEndOfWindow = m_windowIndex > m_windowSize;
        if (m_windowIndex == 0 ) {
            m_windowIndex += 1; // skip window = 0 because that will refer to the target word itself
        }

        int currentSent = this.m_Corpus.getSentenceID(this.m_Index);

        // unused now
        //String keyWord = null;
        int lower = this.m_Corpus.getLowerBoundary(currentSent);
        int upper = this.m_Corpus.getUpperBoundary(currentSent);
        //

        /*
        for (int sentIdx = lower; sentIdx < upper; sentIdx++) {
            if (currentSent - sentIdx > 2
                    || sentIdx - currentSent > 2) {
                continue;
            }
            ISentence sentence = this.m_Corpus.getSentence(sentIdx);
            if (sentence != null) {
                for (int i = 0; i < sentence.size(); i++) {
                    //if (this.filter(keyWord)) {               // TODO can filter out stopwords in future
                     //   continue;
                    //}
                    keyWord = sentence.getItem(i).get(g_LIDX);

                }
            }
        }*/



        if (!isAtEndOfWindow) {


            String lemmaInSentence = null;
            while (lemmaInSentence == null) {
                // obtain the information for use in the feature
                int indexOfItemInSentence = m_IndexInSentence + m_windowIndex + wordsIgnoredForCurrentTargetWord;

                boolean hasExceededSentencePosition = indexOfItemInSentence < 0
                                                      || indexOfItemInSentence >= m_Sentence.size();
                int indexExceeded = indexOfItemInSentence < 0 ?
                                    indexOfItemInSentence :
                                    indexOfItemInSentence - m_Sentence.size();

                IItem item = null;
                if (hasExceededSentencePosition) {
                    String key = formName(m_windowIndex, m_dimensionIndex);

            /*    System.out.println(m_windowIndex + ", " + m_dimensionIndex + " , PADDING");

                List<Double> modelOutputGivenWord = model.get("PADDING");
                double value = modelOutputGivenWord.get(m_dimensionIndex);

                feature = new CSennaContextFeature(key, value);*/

                    currentSent = this.m_Corpus.getSentenceID(this.m_Index);
                    if (!(currentSent - 1 < lower || currentSent + 1 >= upper)) {

                        ISentence sentence = this.m_Corpus.getSentence(indexOfItemInSentence < 0 ? currentSent - 1 : currentSent + 1);

                        //System.out.println("From : " + m_Sentence);
                       // System.out.println("Go : " + sentence);

                        item = sentence.getItem(indexExceeded < 0 ?
                                                sentence.size() + indexExceeded :
                                                indexExceeded);
                    }

                } else {
                    item = m_Sentence.getItem(indexOfItemInSentence);
                }


                if (item != null) {
                    lemmaInSentence = item.get(AItem.Features.LEMMA.ordinal());
                    lemmaInSentence = lemmaInSentence.toLowerCase();
                } else {
                    lemmaInSentence = "PADDING";
                }


           /* if (tokenizationDifferencesMap.containsKey(tokenInSentence)) {
                System.out.println("converting " + tokenInSentence + " to "
                                    + tokenizationDifferencesMap.get(tokenInSentence));
            }*/

               // lemmaInSentence = tokenizationDifferencesMap.containsKey(lemmaInSentence) ?
                //        tokenizationDifferencesMap.get(lemmaInSentence) : lemmaInSentence;

                lemmaInSentence = StringEscapeUtils.unescapeHtml4(lemmaInSentence);

              //  boolean isLemmaStopword = this.filter(lemmaInSentence);
              //  if (isLemmaStopword) {
                    //System.out.println("skip " + lemmaInSentence );
               //     lemmaInSentence = null;
                   // wordsIgnoredForCurrentTargetWord ++;
               // }
            }

            /*
            while (!model.isWordInVocab(tokenInSentence)) {
                System.out.println(tokenInSentence);
                // if word is not in the vocab, then we skip the word, until reach a word in the model
                m_windowIndex += 1;
                m_dimensionIndex = 0;

                if (m_windowIndex > m_windowSize) { // reached the end of the window without finding any word
                    return null;
                }
            }*/

            // obtain key of feature
            String key = formName(m_windowIndex, m_dimensionIndex);
            if (model.isWordInVocab(lemmaInSentence)) {
            //    System.out.println(m_windowIndex + ", " + m_dimensionIndex + " , " + lemmaInSentence);
                // obtain value of feature
                List<Double> modelOutputGivenWord = model.get(lemmaInSentence);
                double value = modelOutputGivenWord.get(m_dimensionIndex);

                feature = new CSennaContextFeature(key, value);
            } else {
              //  System.out.println(m_windowIndex + ", " + m_dimensionIndex + " , " + lemmaInSentence + ", UNKNOWN");
                List<Double> modelOutputGivenWord = model.get("UNKNOWN");
                double value = modelOutputGivenWord.get(m_dimensionIndex);
                // need to return a non-null value
                feature = new CSennaContextFeature(key, value);
            }


            // update indexes for next feature
            m_dimensionIndex += 1;
            if (m_dimensionIndex == model.numDimensions) {
                m_windowIndex += 1;
                m_dimensionIndex = 0;
            }

            return feature;
        } else {
            return null;
        }
    }

    private String formName(int positionFromIndex, int dimensionIndex) {
        return "CWVector_" + String.valueOf(positionFromIndex) + "_" + String.valueOf(dimensionIndex);
    }

    /*
     * (non-Javadoc)
     * @see sg.edu.nus.comp.nlp.ims.feature.IFeatureExtractor#next()
     */
    @Override
    public IFeature next() {
        IFeature feature = null;
        if (this.hasNext()) {
            feature = this.m_CurrentFeature;
            this.m_CurrentFeature = null;
        }
        return feature;
    }

    /*
     * (non-Javadoc)
     * @see sg.edu.nus.comp.nlp.ims.feature.IFeatureExtractor#restart()
     */
    @Override
    public boolean restart() {

        this.m_CurrentFeature = null;
        this.m_windowIndex = -1 * this.m_windowSize;
        this.m_dimensionIndex = 0;
        this.wordsIgnoredForCurrentTargetWord = 0;
        return this.validIndex(this.m_Index);
    }

    /*
     * (non-Javadoc)
     * @see sg.edu.nus.comp.nlp.ims.feature.IFeatureExtractor#setCorpus(sg.edu.nus.comp.nlp.ims.corpus.ICorpus)
     */
    @Override
    public boolean setCorpus(ICorpus p_Corpus) {
        if (p_Corpus == null) {
            return false;
        }
        this.m_Corpus = p_Corpus;

        this.m_Index = 0;
        this.restart();
        this.m_Index = -1;
        this.m_IndexInSentence = -1;
        this.m_InstanceLength = -1;
        return true;
    }

    /**
     * check the validity of index
     *
     * @param p_Index
     *            index
     * @return valid or not
     */
    protected boolean validIndex(int p_Index) {
        if (this.m_Corpus != null && this.m_Corpus.size() > p_Index
                && p_Index >= 0) {
            return true;
        }
        return false;
    }

    /**
     * check whether word is in stop word list or contains no alphabet
     *
     * @param p_Word
     *            word
     * @return true if it should be filtered, else false
     */
    public boolean filter(String p_Word) {
        return this.m_Filter.filter(p_Word);
    }

    /*
     * (non-Javadoc)
     * @see sg.edu.nus.comp.nlp.ims.feature.IFeatureExtractor#setCurrentInstance(int)
     */
    @Override
    public boolean setCurrentInstance(int p_Index) {
        if (this.validIndex(p_Index)) {
            this.m_Index = p_Index;
            this.m_IndexInSentence = this.m_Corpus.getIndexInSentence(p_Index);
            this.m_InstanceLength = this.m_Corpus.getLength(p_Index);
            int currentSent = this.m_Corpus.getSentenceID(p_Index);
            this.m_Sentence = this.m_Corpus.getSentence(currentSent);


            String keyWord = null;
            int lower = this.m_Corpus.getLowerBoundary(currentSent);
            int upper = this.m_Corpus.getUpperBoundary(currentSent);
            /*for (int sentIdx = lower; sentIdx < upper; sentIdx++) {

                ISentence sentence = this.m_Corpus.getSentence(sentIdx);
                if (sentence != null) {

                }
            }*/
            this.restart();
            return true;
        }
        return false;
    }

}
