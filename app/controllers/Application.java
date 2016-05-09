package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.didion.jwnl.JWNLException;
import play.libs.Json;
import play.mvc.*;

import play.mvc.Result;
import sg.edu.nus.comp.nlp.ims.classifiers.CLibLinearEvaluator;
import sg.edu.nus.comp.nlp.ims.feature.CAllWordsFeatureExtractorCombination;
import sg.edu.nus.comp.nlp.ims.io.CResultWriter;
import sg.edu.nus.comp.nlp.ims.lexelt.CResultInfo;
import sg.edu.nus.comp.nlp.ims.util.CJWNL;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPPOSTagger;
import sg.edu.nus.comp.nlp.ims.util.COpenNLPSentenceSplitter;

import util.ImsWrapper;
import views.html.*;

import org.w3c.dom.*;


import sg.edu.nus.comp.nlp.ims.implement.CTester;
import sg.edu.nus.comp.nlp.ims.classifiers.IEvaluator;
import sg.edu.nus.comp.nlp.ims.io.IResultWriter;

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


    public Result index() {
        return ok(index.render("WordNews!"));
    }

    private boolean isWordInDictionary(String word) throws Exception {
        try (
                Connection conn = play.db.DB.getConnection();
        ) {
            try (
                    Statement stmt = conn.createStatement()
            ) {
                stmt.executeQuery(
                        "SELECT id FROM english_words WHERE english_meaning = '" + word + "'"
                );
                ResultSet queryRes = stmt.getResultSet();

                return queryRes.next();
            }
        }
    }

    private ChinesePronunciationPair getChineseFromId(Long chineseId) throws SQLException {
        final int offset = 0; // set to non-zero if there are differences between the local db and db on heroku

        String sql = "SELECT chinese_meaning, pronunciation FROM chinese_words WHERE id = '" + (chineseId + offset) + "'";

        try (
                Connection conn = play.db.DB.getConnection()
        ) {
            try (
                    Statement stmt = conn.createStatement()
            )  {
                stmt.executeQuery(sql);
                ResultSet results = stmt.getResultSet();

                if (results.next()) {
                    return new ChinesePronunciationPair(
                            results.getString("chinese_meaning"),
                            results.getString("pronunciation")
                    );
                }
            }
        }

        return ChinesePronunciationPair.NONE;
    }

    private static class ChinesePronunciationPair {
        String symbol;
        String pronunciation;

        public ChinesePronunciationPair(String symbol, String pronunciation) {
            this.pronunciation = pronunciation;
            this.symbol = symbol;
        }

        public static ChinesePronunciationPair NONE = new ChinesePronunciationPair("", "");
    }


    public Result showTrainedDir() throws IOException {

        List<File> files = Arrays.asList(
                new File("trainedDir").listFiles()
        );

        try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        new GZIPInputStream(
                            new FileInputStream(
                                    files.get(0)
                            )
                        ), "ISO8859-1")
                )
        ) {

            int count = 0;
            while (reader.readLine() != null) {
                count++;
            }
        }

        return ok(
                index.render(
                        files.toString()
                )
        );
    }


    public Result obtainTranslation() throws Exception {

        long startTime = System.nanoTime();
        // extract request params
        final Map<String, String[]> values = request()
                .body()
                .asFormUrlEncoded();
        String textContent = values.get("text")[0];
        String name = values.get("name")[0];
        String url = values.get("url")[0];
        String num_words = values.get("num_words")[0];

        int numWords;
        try {
            numWords = Integer.parseInt(num_words);
        } catch (NumberFormatException | NullPointerException e) {
            numWords = 2;
        }

        ObjectNode result = Json.newObject();

        // find words to translate
        List<String> wordsThatCanBeTranslated = new ArrayList<>();
        String[] tokensInText = textContent.replaceAll("[^a-zA-Z- ]", " ").split("\\s+");

        for (String token : tokensInText ) {
            try {
                if (//!CSurroundingWordFilter.getInstance().filter(token.toLowerCase())
                       // &&
                        isWordInDictionary(token.toLowerCase())) {
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
                         new StreamResult(
                                 new FileOutputStream(testTempFileName)
                         )
            );

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

        try {
            CTester senseDisambiguator = new CTester();

            IEvaluator evaluator = ImsWrapper.getEvaluator();
            IResultWriter writer = ImsWrapper.getWriter();

            senseDisambiguator.setEvaluator(evaluator);
            senseDisambiguator.setWriter(writer);

            String featureExtractorName = CAllWordsFeatureExtractorCombination.class.getName();
            senseDisambiguator.setFeatureExtractorName(featureExtractorName);

            senseDisambiguator.test(testFileName);

            List<Object> results = senseDisambiguator.getResults();
            for (Object resultObj : results) {
                CResultInfo imsResult = (CResultInfo)resultObj;
                for (int instIdx = 0; instIdx < imsResult.size(); instIdx++) {
                    String docID = imsResult.getDocID(instIdx);
                    String instanceId = imsResult.getID(instIdx);
                    String id = imsResult.classes[imsResult.getAnswer(instIdx)];

                    try {
                        long senseId = Long.parseLong(id);
                        ChinesePronunciationPair chineseResult = getChineseFromId(senseId);

                        ObjectNode tokenNode =
                                Json.newObject()
                                        .put("wordId", senseId)
                                        .put("chinese", chineseResult.symbol)
                                        .put("pronunciation", chineseResult.pronunciation)
                                        .put("isTest", 0);

                        result.put(instanceId.split("\\.")[0], tokenNode);
                    } catch (NumberFormatException e) {
                        // silenced because it is U
                        assert id.equals("U");
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
                fileLen++;
                String[] tokensInResultsLine = lineFromResultFile.split(" ");
                long senseId = -1;
                try {
                    senseId = Long.parseLong(tokensInResultsLine[2]);

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

            fileInDirectory.delete();
        }
    }


}
