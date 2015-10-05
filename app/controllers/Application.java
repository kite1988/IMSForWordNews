package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.didion.jwnl.JWNLException;
import play.*;
import play.libs.Json;
import play.mvc.*;

import views.html.*;

import org.w3c.dom.*;

import sg.edu.nus.comp.nlp.ims.implement.CTester;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;
import sg.edu.nus.comp.nlp.ims.io.IResultWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;


public class Application extends Controller {

    public Result index() {
        CTester tester = new CTester();
        String type = "file";
        File testPath = new File("generated_ims_format_text.xml");
        String modelDir = "trainedDir";
        String statDir = "trainedDir";
        String saveDir = "resultDir";
        String evaluatorName = sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator.class.getName();
        String writerName = sg.edu.nus.comp.nlp.ims.io.CResultWriter.class.getName();
        String lexeltFile = null;

        return ok(index.render("Your new application is ready.!!"));
    }

    private boolean isWordInDictionary(String word) {
        boolean result = false;



        return result;
    }

    public Result obtainTranslation() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, JWNLException, ParserConfigurationException {

        // extract request params
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        String text = values.get("text")[0];
        String name = values.get("name")[0];
        String url = values.get("url")[0];
        String num_words = values.get("num_words")[0];

        ObjectNode result = Json.newObject();
        result.put("message", "Hello " + name);


        // find words to translate




        // write to files expected by ims
        // xml file

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("corpus");
        doc.appendChild(rootElement);
        rootElement.setAttribute("lang", "english");

        Element lexelt = doc.createElement("lexelt");
        rootElement.appendChild(lexelt);
       // lexelt.setAttribute("item", );





        // key file



        CTester tester = new CTester();
        String type = "file";
        File testPath = new File("generated_ims_format_text.xml");
        String modelDir = "trainedDir";
        String statDir = "trainedDir";
        String saveDir = "resultDir";
        String evaluatorName = sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator.class.getName();
        String writerName = sg.edu.nus.comp.nlp.ims.io.CResultWriter.class.getName();
        String lexeltFile = null;

        // initial JWordNet
        try {
            sg.edu.nus.comp.nlp.ims.util.CJWNL.initial(new FileInputStream("lib/prop.xml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw e;
        } catch (JWNLException e) {
            e.printStackTrace();
            throw e;
        }


        sg.edu.nus.comp.nlp.ims.util.COpenNLPPOSTagger.setDefaultModel("lib/tag.bin.gz");
        sg.edu.nus.comp.nlp.ims.util.COpenNLPPOSTagger.setDefaultPOSDictionary("lib/tagdict.txt");




        IEvaluator evaluator = (sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator) Class.forName(evaluatorName)
                .newInstance();

        evaluator.setOptions(new String[]{"-m", modelDir, "-s", statDir});


        // set result writer
        writerName = sg.edu.nus.comp.nlp.ims.io.CResultWriter.class.getName();

        IResultWriter writer = (sg.edu.nus.comp.nlp.ims.io.IResultWriter) Class.forName(writerName)
                .newInstance();
        writer.setOptions(new String[]{"-s", saveDir});

        tester.setEvaluator(evaluator);
        tester.setWriter(writer);

        String featureExtractorName = sg.edu.nus.comp.nlp.ims.feature.CAllWordsFeatureExtractorCombinationWithSenna.class.getName();
        tester.setFeatureExtractorName(featureExtractorName);





        return ok(result);

    }
}
