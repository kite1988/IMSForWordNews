
import net.didion.jwnl.JWNLException;
import play.*;
import sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;
import sg.edu.nus.comp.nlp.ims.util.CJWNL;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPPOSTagger;
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
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try{
            ImsWrapper.propXmlFile = new FileInputStream("lib/prop.xml");
        } catch (FileNotFoundException e) {
            System.out.println("initialisation of prop.xml failed");
            e.printStackTrace();
        }
        try{
            CJWNL.initial(ImsWrapper.propXmlFile);
            Logger.info("Initialised CJWNL");

            COpenNLPPOSTagger.setDefaultModel("lib/tag.bin.gz");
            COpenNLPPOSTagger.setDefaultPOSDictionary("lib/tagdict.txt");
        } catch (JWNLException e) {
            e.printStackTrace();
            System.out.println("failed to inixitalise CJWNL");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("failed to inixitalise CJWNL because of tag.bin.gz and tagdict");
        }
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }

}