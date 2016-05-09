
import net.didion.jwnl.JWNLException;
import play.*;
import sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;
import sg.edu.nus.comp.nlp.ims.feature.CAllWordsFeatureExtractorCombination;
import sg.edu.nus.comp.nlp.ims.implement.CTester;
import sg.edu.nus.comp.nlp.ims.util.CJWNL;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPPOSTagger;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPSentenceSplitter;
import util.ImsWrapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Global extends GlobalSettings {

    public void onStart(Application app) {
        Logger.info("Application has started");

        String modelDir = "trainedDir";
        String statDir = "trainedDir";

        try {
            IEvaluator evaluator = ImsWrapper.getEvaluator(); // this initliases the evalutor for first time
            evaluator.setOptions(new String[]{"-m", modelDir, "-s", statDir});

            ImsWrapper.getWriter(); // init
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // initial JWordNet
        try {
            CJWNL.initial(new FileInputStream("lib/prop.xml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to initialise due to missing file", e);
        } catch (JWNLException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to initialise due to JWNL exception", e);
        }

        try {
            COpenNLPPOSTagger.setDefaultModel("lib/tag.bin.gz");
            COpenNLPPOSTagger.setDefaultPOSDictionary("lib/tagdict.txt");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Problem initialising COpenNLPPostTagger", e);
        }

        try {
            COpenNLPSentenceSplitter.setDefaultModel("lib/EnglishSD.bin.gz");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Problem initialising Sentence Splitter", e);
        }

        ImsWrapper.disambiguator = new CTester();

        try {
            ImsWrapper.disambiguator.setEvaluator(ImsWrapper.getEvaluator());
            ImsWrapper.disambiguator.setWriter(ImsWrapper.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Problem initialising IMS CTester", e);
        }


        ImsWrapper.disambiguator.setFeatureExtractorName(
                CAllWordsFeatureExtractorCombination.class.getName()
        );


    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }

}