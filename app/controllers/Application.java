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
import sg.edu.nus.comp.nlp.ims.feature.CAllWordsFeatureExtractorCombination;
import sg.edu.nus.comp.nlp.ims.feature.CAllWordsFeatureExtractorCombinationWithSenna;
import sg.edu.nus.comp.nlp.ims.feature.SennaWordEmbeddings;
import sg.edu.nus.comp.nlp.ims.io.CResultWriter;
import sg.edu.nus.comp.nlp.ims.lexelt.CResultInfo;
import sg.edu.nus.comp.nlp.ims.util.CJWNL;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPPOSTagger;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPSentenceSplitter;
import sg.edu.nus.comp.nlp.ims.util.CSurroundingWordFilter;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;


public class Application extends Controller {


    static SennaWordEmbeddings embeddings = SennaWordEmbeddings.instance();

    public Result index() {
        CTester tester = new CTester();
        String type = "file";
       // File testPath = new File("generated_ims_format_text.xml");
        String modelDir = "trainedDir";
        String statDir = "trainedDir";
        String saveDir = "resultDir";
        String evaluatorName = sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator.class.getName();
        String writerName = sg.edu.nus.comp.nlp.ims.io.CResultWriter.class.getName();
        String lexeltFile = null;

        return ok(index.render("Your new application is ready.!!"));
    }

    private boolean isWordInDictionary(String word) throws SQLException {
        try {
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
        } catch (Exception e) {
            //pass
            // just return false
        }

        return false;
    }

    private ChinesePronunciationPair getChineseFromId(Long chineseId) throws SQLException {
        final int chineseIdOffset = 0; // because there of differences between the local db and db on heroku

        System.out.println(chineseId);
        String sql = "SELECT chinese_meaning, pronunciation FROM chinese_words WHERE id = '" + (chineseId + chineseIdOffset) + "'";

        Connection conn = play.db.DB.getConnection();
        try {
            Statement stmt = conn.createStatement();
            try {
                stmt.executeQuery(sql);
                ResultSet queryRes = stmt.getResultSet();

                if (queryRes.next()) {

                    ChinesePronunciationPair result = new ChinesePronunciationPair();

                    result.symbol = queryRes.getString("chinese_meaning");
                    result.pronunciation = queryRes.getString("pronunciation");
                    return result;
                }

            } finally {
                stmt.close();
            }
        } finally {
            conn.close();
        }

        return ChinesePronunciationPair.NONE;
    }

    private static class ChinesePronunciationPair {
        String symbol;
        String pronunciation;

        public static ChinesePronunciationPair NONE = new ChinesePronunciationPair();
    }


    public Result showTrainedDir() throws IOException {
        File dir = new File("trainedDir");
        File[] a = dir.listFiles();
        List<File> heh = Arrays.asList(a);
        System.out.println(heh.get(0).getAbsolutePath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(heh.get(0))), "ISO8859-1"));

        String line ;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            count ++;
        }
        System.out.println("  :: " + count);

        reader.close();

        return ok(index.render(heh.toString()));
    }


    public Result obtainTranslation() throws SQLException, ParserConfigurationException, TransformerException, IOException, JWNLException {

        long startTime = System.currentTimeMillis();
        // extract request params
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        String textContent = values.get("text")[0];
        String name = values.get("name")[0];
        String url = values.get("url")[0];
        String num_words = values.get("num_words")[0];
        System.out.println("start with ... " + textContent + " to find " + num_words + " words");

        int numWords;
        try {
            numWords = Integer.parseInt(num_words);
        } catch (NumberFormatException | NullPointerException e) {
            numWords = 2;
        }

        ObjectNode result = Json.newObject();

        // find words to translate
        List<String> wordsThatCanBeTranslated = new ArrayList<>();
        String[] tokensInText = textContent.replaceAll("[^a-zA-Z ]", "").split("\\s+");
        for (String token : tokensInText ) {
            try {
                if (!CSurroundingWordFilter.getInstance().filter(token.toLowerCase()) && isWordInDictionary(token.toLowerCase())) {
                    System.out.print(token + " , ");
                    wordsThatCanBeTranslated.add(token);
                    if (wordsThatCanBeTranslated.size() >= numWords) {
                        break;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
        System.out.println();

        // write to files expected by ims
        // xml file

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw e;
        }
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
            context.setTextContent(" " + amendedTextContent + " ");
        }

        int randomNumber = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        String testTempFileName = "temptestfile" + randomNumber;
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

        // todo use filelock
        // write to format expected by ims
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


    /*    try (BufferedReader tempFileReader = new BufferedReader(new FileReader(testFileName))) {

            String lineInFile;
            while ((lineInFile = tempFileReader.readLine()) != null) {


                System.out.println(lineInFile);


            }
            System.out.println(" in file : " + testFileName);
            System.out.println("         : " + new File(testFileName).getAbsolutePath());
        }*/

        // key file.... doesn't matter

        System.out.println(System.currentTimeMillis() - startTime);
        System.out.println("before preparing tester");
        System.out.println(System.currentTimeMillis() - startTime);

        // run tester

        CTester tester = new CTester();
        String type = "file";
        //File testPath = new File("generated_ims_format_text.xml");
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



        try {
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


            tester.test(testFileName);

            List<Object> results = (List<Object>)tester.getResults();
            for (Object thing : results) {
                CResultInfo imsResult = (CResultInfo)thing;
                for (int instIdx = 0; instIdx < imsResult.size(); instIdx++) {
                    String docID = imsResult.getDocID(instIdx);
                    String instanceId = imsResult.getID(instIdx);
                    String id = imsResult.classes[imsResult.getAnswer(instIdx)];

                    try {
                        long senseId = Long.parseLong(id);
                        ChinesePronunciationPair chineseResult = getChineseFromId(senseId);

                        ObjectNode tokenNode = Json.newObject();
                        tokenNode.put("wordId", senseId);
                        tokenNode.put("chinese", chineseResult.symbol);
                        tokenNode.put("pronunciation", chineseResult.pronunciation);
                        tokenNode.put("isTest", 0);

                        result.put(instanceId.split("\\.")[0], tokenNode);
                    } catch (NumberFormatException e) {
                        // silenced because it is U
                        assert id.equals("U");
                    }
                }

            }


            // read from results dir
            //File resultsDirectory = new File(saveDir);
            //File[] filesInDirectory = resultsDirectory.listFiles();
            //handleResultsDir(result, filesInDirectory);


        } catch (Exception e) {
            System.out.println("Ctester problem!");
            throw new RuntimeException(e);

        }

        System.out.println("bef return");
        System.out.println(System.currentTimeMillis() - startTime);
        return ok(result);
    }

    private void handleResultsDir(ObjectNode result, File[] filesInDirectory) throws IOException, SQLException {
        // there should only be one file
        for (File fileInDirectory : filesInDirectory) {
            System.out.println(fileInDirectory.getAbsolutePath());
            if (fileInDirectory.getName().equals("aaa")) {
                continue; // skip dummy file
            }
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(fileInDirectory));

            String lineFromResultFile;
            int fileLen = 0;
            while ((lineFromResultFile = bufferedReader.readLine()) != null) {
                System.out.println(lineFromResultFile);
                System.out.println("=====================================");


                fileLen++;
                String[] tokensInResultsLine = lineFromResultFile.split(" ");
                long senseId = -1;
                try {
                    senseId = Long.parseLong(tokensInResultsLine[2]);
                 //   result.put("senseid", senseId);

                    ChinesePronunciationPair chineseResult = getChineseFromId(senseId);

                    ObjectNode tokenNode = Json.newObject();
                    tokenNode.put("wordId", senseId);
                    tokenNode.put("chinese", chineseResult.symbol);
                    tokenNode.put("pronunciation", chineseResult.pronunciation);
                    tokenNode.put("isTest", 0);

                    result.put(tokensInResultsLine[1].split("\\.")[0], tokenNode);
                } catch (NumberFormatException e) {
                    // silenced because it is U
                    assert tokensInResultsLine[2].equals("U");
                }
            }

            System.out.println("len: " + fileLen);

            fileInDirectory.delete();
        }
    }


}
