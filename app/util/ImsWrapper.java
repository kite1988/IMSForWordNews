package util;

import sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;

import java.io.FileInputStream;

/**
 * Created by kanghj on 26/10/15.
 */
public class ImsWrapper {

    static IEvaluator evaluator;

    public static FileInputStream propXmlFile;

    public static IEvaluator getEvaluator() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (evaluator == null) {
            String evaluatorName = CLibLinearEvaluator.class.getName();
            evaluator = (IEvaluator) Class.forName(evaluatorName).newInstance();
            evaluator.setOptions(new String[]{"-m", "trainedDir", "-s", "trainedDir"});
        }
        return evaluator;
    }
}
