package sg.edu.nus.comp.nlp.ims.feature;

import org.apache.commons.lang3.StringEscapeUtils;
import sg.edu.nus.comp.nlp.ims.corpus.AItem;
import sg.edu.nus.comp.nlp.ims.corpus.ICorpus;
import sg.edu.nus.comp.nlp.ims.corpus.IItem;
import sg.edu.nus.comp.nlp.ims.corpus.ISentence;
import sg.edu.nus.comp.nlp.ims.util.CSurroundingWordFilter;

import java.util.*;

public class CSennaContextSumFeatureExtractor implements IFeatureExtractor {
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
    public CSennaContextSumFeatureExtractor() {
        this.m_windowSize = 17 / 2;
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
     * get next feature, which is the sum of a dimension over the entire window
     *
     * @return feature
     */
    private IFeature getNext() {
        IFeature feature = null;

        if (this.m_dimensionIndex >= model.numDimensions) {
            return null;
        }

        // obtain key of feature
        String key = formName(m_dimensionIndex);
        double totalValue = 0;

      //  System.out.print(": " + this.m_Sentence.getItem(this.m_IndexInSentence) + "  " + key + "  ... ");
        for (int i = 0; i < this.m_Sentence.size(); i++) {
            if (i == m_IndexInSentence) {   // target word should be skipped
                continue;
            }

            /*int indexOfItemInSentence = this.m_IndexInSentence + i;
            if (indexOfItemInSentence < 0) {
                continue;
            }
            if (indexOfItemInSentence < m_Sentence.size()) {
                break;
            }*/


            IItem item = this.m_Sentence.getItem(i);
            String lemmaInSentence = item.get(AItem.Features.LEMMA.ordinal());

            if (this.filter(item.get(AItem.Features.TOKEN.ordinal()))) {
                continue;
            }


            double value;
            if (model.isWordInVocab(lemmaInSentence)) {
                List<Double> modelOutputGivenWord = model.get(lemmaInSentence);
                value = modelOutputGivenWord.get(m_dimensionIndex);
            } else {
                List<Double> modelOutputGivenWord = model.get("UNKNOWN");
                value = modelOutputGivenWord.get(m_dimensionIndex);
            }

     //       System.out.print(lemmaInSentence + ", ");

            totalValue += value;
        }
      //  System.out.println("= " + totalValue);

        feature = new CSennaContextFeature(key, totalValue);

        // update indexes for next feature
        m_dimensionIndex += 1;

        return feature;
    }

    private String formName( int dimensionIndex) {
        return ("CWVector_" + String.valueOf(dimensionIndex)).intern();
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
        this.m_dimensionIndex = 0;

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
