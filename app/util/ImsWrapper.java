package util;

import sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;
import sg.edu.nus.comp.nlp.ims.io.CResultWriter;
import sg.edu.nus.comp.nlp.ims.io.IResultWriter;

/**
 * Created by kanghj on 26/10/15.
 */
public class ImsWrapper {

    static IEvaluator evaluator;
    static IResultWriter writer;

    public static IEvaluator getEvaluator() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (evaluator == null) {
            String evaluatorName = CLibLinearEvaluator.class.getName();
            evaluator = (IEvaluator) Class.forName(evaluatorName).newInstance();
        }
        return evaluator;
    }

    public static IResultWriter getWriter() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (writer == null) {
            String saveDir = "resultDir";
            String writerName = CResultWriter.class.getName();

            IResultWriter writer = (IResultWriter) Class.forName(writerName)
                    .newInstance();
            writer.setOptions(new String[]{"-s", saveDir});
            ImsWrapper.writer = writer;
        }

        return writer;
    }
}
