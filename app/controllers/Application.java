package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import play.libs.Json;
import play.libs.Json.*;                        
import play.mvc.*;
import play.mvc.BodyParser;                     


import play.mvc.Result;
import sg.edu.nus.comp.nlp.ims.feature.CAllWordsFeatureExtractorCombination;
import sg.edu.nus.comp.nlp.ims.lexelt.CResultInfo;

import util.ImsWrapper;
import views.html.*;

import org.w3c.dom.*;


import sg.edu.nus.comp.nlp.ims.implement.CTester;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;
import java.util.HashMap;


public class Application extends Controller {
    private static HashMap<String, String> idWord;
    private String chineseDictFile = "dump" + File.separator + "chinese_words.csv";
	private static String testFlag = "_____";

    
    public Result index() {
        return ok(index.render("WordNews!"));
    }


    public Result test() {
        response().setHeader("Access-Control-Allow-Origin", "*"); 

        System.out.println("test");
        ObjectNode result = Json.newObject();
        result.put("msg", "ok");
        result.put("translation", "中文");
        return ok(result);
    }


    public Result translateWord() {
        response().setHeader("Access-Control-Allow-Origin", "*"); 

        long startTime = System.nanoTime();
        // extract request params
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
		
		System.out.println(request().body());
				
        if( values==null || values.get("word") == null || values.get("sentence") == null ) {
             ObjectNode bad_request_result = Json.newObject();
             bad_request_result.put("msg", "Invalid parameters");
             //return badRequest(bad_request_result);
             return ok(bad_request_result);
         }

        if(values==null) {
            ObjectNode bad_request_result = Json.newObject();
            bad_request_result.put("msg", "Invalid parameters");
            return ok(bad_request_result);
        }
        
        String word = values.get("word")[0];
        String sentence = values.get("sentence")[0];
     
        ObjectNode result = Json.newObject();
        String translated_text = null;
        try {
		    String imsFormat = generateXML(word, sentence);
            translated_text = getIMSTranslation(imsFormat);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("msg", "no-result");
        }
		
		if (translated_text!=null) {
			result.put("msg", "OK");
			result.put("translation", translated_text);
		} 

		long endTime = System.nanoTime();
		System.out.println("translating " + word + " used " + (endTime-startTime)/1000000 + "ms");
        return ok(result);
    }


	// TODO: Change the inteface of IMS to accept sentence directly.
    // write to files expected by ims
    private String generateXML(String token, String sentence) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
			return null;
        }
		
        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("corpus");
        doc.appendChild(rootElement);
        rootElement.setAttribute("lang", "english");

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
        context.setTextContent(" " + sentence + " ");

        // write to xml
        String xmlFormat = "";
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            StreamResult xmlStringResult = new StreamResult(new StringWriter());
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            // send DOM to file

            tr.transform(new DOMSource(doc), xmlStringResult);
            xmlFormat = xmlStringResult.getWriter().toString();

        } catch (TransformerException te) {
            System.out.println(te.getMessage());
			return null;
        }

        // write to format expected by ims
        StringBuilder imsFormat = new StringBuilder();
        for (String lineInFile : xmlFormat.split("\n")) {
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
                imsFormat.append(updatedLine.toString());
            } else {
                imsFormat.append(lineInFile);
            }
        }
		return imsFormat.toString();
    }


    private String getIMSTranslation(String imsFormat) {
		try {
			CTester senseDisambiguator = getSenseDisambiguator();

			//senseDisambiguator.test(testFileName);
			senseDisambiguator.testWithXmlString(imsFormat.toString(), null);

			List<Object> results = senseDisambiguator.getResults();
			if (results.size()==1) {
				Object resultObj = results.get(0);
			
				CResultInfo imsResult = (CResultInfo)resultObj;
				if (imsResult.size()==1) {
				
					String docID = imsResult.getDocID(0);
					String instanceId = imsResult.getID(0);
					String senseId = imsResult.classes[imsResult.getAnswer(0)];
					
					//System.out.println("sense ID " + senseId);

					if (StringUtils.isNumeric(senseId)) {
						return getChineseWord(senseId);
					}
				}
			}        
		}catch (Exception e) {
			System.err.println("Error disambiguating ");
			e.printStackTrace();
		}
		return null;
    }
	


    private CTester getSenseDisambiguator() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        CTester senseDisambiguator = new CTester();

        senseDisambiguator.setEvaluator(ImsWrapper.getEvaluator());
        senseDisambiguator.setWriter(ImsWrapper.getWriter());
        senseDisambiguator.setFeatureExtractorName(
                CAllWordsFeatureExtractorCombination.class.getName()
        );

        return senseDisambiguator;
    }


    private String getChineseWord(String id) {
		if (idWord==null) {
            loadChineseDictionary();
        }

        return idWord.get(id); 
    }

    // Load Chinese word and its ID into memeory.
    private void loadChineseDictionary() {
        idWord = new HashMap<String, String>();

        try {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(chineseDictFile), "UTF-8"));
				
        String line = null;
        while ((line=br.readLine())!=null) {
            line = line.trim();
            if (!line.isEmpty()) {
                String[] items = line.split(",");
                idWord.put(items[0], items[1]);
				//System.out.println(items[1]);
            }
        }
        br.close();
		} catch (Exception e) {
			System.err.println("Error in loading " + chineseDictFile);
			e.printStackTrace();
		}
		System.out.println("size of dict " + idWord.size());
    }

}
