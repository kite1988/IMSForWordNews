package sg.edu.nus.comp.nlp.ims.feature;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Each CSennaContextFeature represent a single dimension of a word in the context of a word in a sentence.
 * This means that an instance must have a list of CSennaContextFeature, representing every dimension of
 * every word in the context. d . (w−1) features will be added to each sample, where <br/>
 *  <ul>
 *     <li>* d is the word embeddings dimension and</li>
 *     <li>* w is the number of words in the window of text surrounding the target word.</li>
 *  </ul>
 */
public class CSennaContextFeature extends CDoubleFeature {


    public CSennaContextFeature(String key, double value) {
        this.m_Key = key;
        this.m_Value = value;
    }


    public static void main(String[] args) {
        // sanity check
        SennaWordEmbeddings model = SennaWordEmbeddings.instance();

        // sanity checks
        List<Double> intemediate1 = SennaWordEmbeddings.minus(model.get("king"), model.get("man"));
        List<Double> intemediate2 = SennaWordEmbeddings.plus(intemediate1, model.get("woman"));

        System.out.println(intemediate2.size());
        System.out.println(intemediate2);

        List<Double> queen = model.get("queen");

        System.out.println(SennaWordEmbeddings.cosineSimilarity(intemediate2, queen)); // 0.59665847

        System.out.println(SennaWordEmbeddings.cosineSimilarity(model.get("broccoli"), model.get("cauliflower"))); //
    }

}
