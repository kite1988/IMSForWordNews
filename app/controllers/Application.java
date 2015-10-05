package controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.didion.jwnl.JWNLException;
import play.*;
import play.libs.Json;
import play.mvc.*;
import play.api.db.*;

import sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator;
import sg.edu.nus.comp.nlp.ims.feature.CAllWordsFeatureExtractorCombinationWithSenna;
import sg.edu.nus.comp.nlp.ims.io.CResultWriter;
import sg.edu.nus.comp.nlp.ims.util.CJWNL;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPPOSTagger;
import views.html.*;

import org.w3c.dom.*;


import sg.edu.nus.comp.nlp.ims.implement.CTester;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;
import sg.edu.nus.comp.nlp.ims.io.IResultWriter;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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

    private boolean isWordInDictionary(String word) throws SQLException {
        String sql = "SELECT BOOLEAN FROM english_words;";

        Connection conn = play.db.DB.getConnection();
        try {
            Statement stmt = conn.createStatement();
            try {
                stmt.executeQuery(sql);
                ResultSet queryRes = stmt.getResultSet();
                System.out.println(queryRes);
                System.out.println(queryRes);
                System.out.println(queryRes);
                if (queryRes.getBoolean("BOOLEAN")) {
                    return true;
                }

            } finally {
                stmt.close();
            }
        } finally {
            conn.close();
        }

        return false;
    }

    public Result obtainTranslation() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, JWNLException, ParserConfigurationException, SQLException {

        // extract request params
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        String text = values.get("text")[0];
        String name = values.get("name")[0];
        String url = values.get("url")[0];
        String num_words = values.get("num_words")[0];

        ObjectNode result = Json.newObject();
        result.put("message", "Hello " + name);


        // find words to translate
        List<String> wordsThatCanBeTranslated = new ArrayList<>();
        String[] tokensInText = text.split(" ");
        for (String token : tokensInText ) {
            try {
                if (isWordInDictionary(token)) {
                    wordsThatCanBeTranslated.add(token);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }


        result.put("heheheh", wordsThatCanBeTranslated.toString());


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
        String evaluatorName = CLibLinearEvaluator.class.getName();
        String writerName = CResultWriter.class.getName();
        String lexeltFile = null;

        // initial JWordNet
        try {
            CJWNL.initial(new FileInputStream("lib/prop.xml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw e;
        } catch (JWNLException e) {
            e.printStackTrace();
            throw e;
        }


        COpenNLPPOSTagger.setDefaultModel("lib/tag.bin.gz");
        COpenNLPPOSTagger.setDefaultPOSDictionary("lib/tagdict.txt");




        IEvaluator evaluator = (IEvaluator) Class.forName(evaluatorName)
                .newInstance();

        evaluator.setOptions(new String[]{"-m", modelDir, "-s", statDir});


        // set result writer
        writerName = CResultWriter.class.getName();

        IResultWriter writer = (IResultWriter) Class.forName(writerName)
                .newInstance();
        writer.setOptions(new String[]{"-s", saveDir});

        tester.setEvaluator(evaluator);
        tester.setWriter(writer);

        String featureExtractorName = CAllWordsFeatureExtractorCombinationWithSenna.class.getName();
        tester.setFeatureExtractorName(featureExtractorName);





        return ok(result);

    }
}
