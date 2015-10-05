package controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.didion.jwnl.JWNLException;
import play.*;
import play.libs.Json;
import play.mvc.*;
import play.api.db.*;

import play.mvc.Result;
import sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator;
import sg.edu.nus.comp.nlp.ims.feature.CAllWordsFeatureExtractorCombinationWithSenna;
import sg.edu.nus.comp.nlp.ims.io.CResultWriter;
import sg.edu.nus.comp.nlp.ims.util.CJWNL;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPPOSTagger;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPSentenceSplitter;
import views.html.*;

import org.w3c.dom.*;


import sg.edu.nus.comp.nlp.ims.implement.CTester;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;
import sg.edu.nus.comp.nlp.ims.io.IResultWriter;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
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
        String sql = "SELECT id FROM english_words WHERE english_meaning = '" + word + "'";

        Connection conn = play.db.DB.getConnection();
        try {
            Statement stmt = conn.createStatement();
            try {
                stmt.executeQuery(sql);
                ResultSet queryRes = stmt.getResultSet();

                if (queryRes.next()) {
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

    public Result obtainTranslation() throws Exception {

        // extract request params
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        String textContent = values.get("text")[0];
        String name = values.get("name")[0];
        String url = values.get("url")[0];
        String num_words = values.get("num_words")[0];

        ObjectNode result = Json.newObject();
        result.put("message", "Hello " + name);


        // find words to translate
        List<String> wordsThatCanBeTranslated = new ArrayList<>();
        String[] tokensInText = textContent.split(" ");
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

        String testFlag =  "_____";

        for (String token : wordsThatCanBeTranslated) {
            Element lexelt = doc.createElement("lexelt");
            lexelt.setAttribute("item", token);
            rootElement.appendChild(lexelt);
            // lexelt.setAttribute("item", );

            Element instance = doc.createElement("instance");
            lexelt.appendChild(instance);

            String instanceId = token + ".0";
            instance.setAttribute("id", instanceId);
            instance.setAttribute("docsrc", "dummy");

            Element answer = doc.createElement("answer");
            instance.appendChild(answer);
            answer.setAttribute("instance", instanceId);
            answer.setAttribute("senseid", "dunno"); // because we are trying to find that out!!!

            Element context = doc.createElement("context");
            instance.appendChild(context);
            String amendedTextContent = textContent.replaceFirst(token, testFlag + token);
            context.setTextContent(amendedTextContent);
        }

        String testTempFileName = "temptestfile";
        // write to xml
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            // send DOM to file

            tr.transform(new DOMSource(doc),
                         new StreamResult(new FileOutputStream(testTempFileName)));

        } catch (TransformerException te) {
            System.out.println(te.getMessage());
            throw te;
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            throw ioe;
        }

        // try reading
        String testFileName = testTempFileName + "_test.xml" ;
        try (BufferedReader tempFileReader = new BufferedReader(new FileReader(testTempFileName))) {
            try (BufferedWriter testFileWriter = new BufferedWriter(new FileWriter(testFileName))) {
                String lineInFile;
                while ((lineInFile = tempFileReader.readLine()) != null) {

                    if (lineInFile.contains(testFlag)) {
                        StringBuilder updatedLine = new StringBuilder();

                        String[] lineInFileAsTokens = lineInFile.split(" ");
                        for (String tokenInFile : lineInFileAsTokens) {
                            if (tokenInFile.contains(testFlag)) {
                                String targetToken = tokenInFile.split(testFlag)[1];
                                updatedLine.append("<head>" + targetToken + "</head>");
                            } else {
                                updatedLine.append(tokenInFile);
                            }
                            updatedLine.append(' ');
                        }

                        testFileWriter.write(updatedLine.toString());
                    } else {
                        testFileWriter.write(lineInFile);
                    }

                }
            }
        }


        try (BufferedReader tempFileReader = new BufferedReader(new FileReader(testFileName))) {

            String lineInFile;
            while ((lineInFile = tempFileReader.readLine()) != null) {


                System.out.println(lineInFile);


            }

        }

        // key file.... doesn't matter


        // run tester

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

        COpenNLPSentenceSplitter.setDefaultModel("lib/EnglishSD.bin.gz");

        //List<File> testFiles = new ArrayList<>();

        tester.test(testFileName);


        // read from results dir
        File resultsDirectory = new File(saveDir);
        File[] filesInDirectory = resultsDirectory.listFiles();

        // there should only be one file
        for (File fileInDirectory : filesInDirectory) {
            BufferedReader bufferedReader = new BufferedReader(
                                                    new FileReader(fileInDirectory));

            String lineFromResultFile;
            int fileLen = 0;
            while ((lineFromResultFile = bufferedReader.readLine()) != null) {
                fileLen ++;
                String[] tokensInResultsLine = lineFromResultFile.split(" ");
                long senseId = Long.parseLong(tokensInResultsLine[2]);

                result.put("senseid", senseId);
            }

            System.out.println("len: " + fileLen);

            fileInDirectory.delete();
        }

        return ok(result);
    }
}
