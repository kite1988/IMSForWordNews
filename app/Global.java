
import play.*;
import sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;
import util.ImsWrapper;

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

    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }

}