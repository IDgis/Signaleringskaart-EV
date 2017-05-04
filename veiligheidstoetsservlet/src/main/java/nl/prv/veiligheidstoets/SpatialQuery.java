package nl.prv.veiligheidstoets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class SpatialQuery {
	private String urlstr;
	private String filter;

	public SpatialQuery(String urlstr, String filter) {
		this.urlstr = urlstr;
		this.filter = filter;
	}

	private String getResult() throws IOException {
		return postDataToService();
	}

	/**
	 * 
	 * @return Gets an xml with the data given in the postbody
	 * @throws IOException
	 */
	private String postDataToService() throws IOException {
		 URL url = new URL(urlstr);
		 HttpURLConnection hpcon = null;
		 try{
			hpcon = (HttpURLConnection) url.openConnection();   
			hpcon.setRequestMethod("POST");
			hpcon.setRequestProperty("Content-Length", "" + Integer.toString(filter.getBytes().length));      
			hpcon.setRequestProperty("Content-Type", "xml/text");
			hpcon.setUseCaches (false);
			hpcon.setDoInput(true);
			hpcon.setDoOutput(true);
			DataOutputStream printout = new DataOutputStream (hpcon.getOutputStream ());
			printout.writeBytes (filter);
			printout.flush ();
			printout.close ();		
			BufferedReader in = new BufferedReader(new InputStreamReader(hpcon.getInputStream()));
			String input;
			StringBuilder response = new StringBuilder(256);
			while((input = in.readLine()) != null) {
				response.append(input + "\r");
			}
			if (response.toString().indexOf("ExceptionReport") > 0){
				System.out.println("fout in request naar " + urlstr + " met filter " + filter + " response: " + response.toString());
			}
			return response.toString();	
			
			
		} catch(Exception e){
			System.out.println("fout in request naar " + urlstr + " met filter " + filter);
			throw new IOException(e);	
		} finally {
			if(hpcon != null){
				hpcon.disconnect();	
			}
		} 
	}
	
	/**
	 * Returns the number of properties found or a json of the features with the properties specified in the filter.
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getPropertyResult() throws Exception {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			
			Document filterDoc = builder.parse(new InputSource(new ByteArrayInputStream(filter.getBytes())));
			String resultType = filterDoc.getDocumentElement().getAttribute("resultType");
			if(resultType.equals("hits")) {
				return getNumFeatures(doc);
			}
			
			// Getting properties to filter
			List<String> filteredFeatures = new ArrayList<>();
			NodeList elementList = filterDoc.getElementsByTagName("ogc:PropertyName");
			for(int i = 0; i < elementList.getLength(); i++) {
				filteredFeatures.add(elementList.item(i).getTextContent());
			}
			
			// Getting names of the results
			List<String> resultNames = new ArrayList<>();
			NodeList featureList = doc.getElementsByTagName("gml:featureMember");
			for(int i = 0; i < featureList.getLength(); i++) {
				Node node = featureList.item(i);
				Element element = (Element)node;
				Element nameElement = (Element)element.getElementsByTagName("*").item(0);
				resultNames.add(nameElement.getAttribute("fid"));
			}
			return getProperties(filteredFeatures, resultNames, doc.getElementsByTagName("gml:featureMember"));
		} 
		catch (IOException | ParserConfigurationException | SAXException e) {
			throw new Exception(e);
		}
	}
	
	/**
	 * Returns the number of features found in the given document
	 * @param doc
	 * @return
	 */
	private Map<String, String> getNumFeatures(Document doc) {
		Map<String, String> numFeatures = new HashMap<>();
		
		Element element = doc.getDocumentElement();
		int result = Integer.parseInt(element.getAttribute("numberOfFeatures"));
		numFeatures.put("numberOfFeaturesFound", Integer.toString(result));
		
		return numFeatures;
	}
	
	/**
	 * Returns a Map with the name and json array string of the features found
	 * @param filteredFeatures
	 * @param resultNames
	 * @param elementList
	 * @return
	 */
	private Map<String, String> getProperties(List<String> filteredFeatures, List<String> resultNames, NodeList elementList) {
		Map<String, String> features = new HashMap<>();
		
		List<String> propertyList = new ArrayList<>();
		for(int i = 0; i < elementList.getLength(); i++) {
			Node featureMemberNode = elementList.item(i);
			Element featureMemberElement = (Element)featureMemberNode;
			NodeList featureMemberList = featureMemberElement.getElementsByTagName("*");
			for(int j = 0; j < featureMemberList.getLength(); j++) {
				Node featureMemberValue = featureMemberList.item(j);
				
				for(int k = 0; k < filteredFeatures.size(); k++) {
					if(filteredFeatures.get(k).equals("the_geom") || filteredFeatures.get(k).equals("geometrie")) {
						filteredFeatures.remove(k);
						continue;
					}
					if(featureMemberValue.getNodeName().endsWith(":" + filteredFeatures.get(k))) {
						propertyList.add(featureMemberValue.getTextContent());
					}
				}
			}
			
		}
		String[] propertyArray = propertyList.toArray(new String[propertyList.size()]);
		String[] filterArray = filteredFeatures.toArray(new String[filteredFeatures.size()]);
		String[] valueStringArray = parseToSingleStringArray(filterArray, propertyArray);
		
		for(int i = 0; i < valueStringArray.length; i++) {
			features.put(resultNames.get(i), valueStringArray[i]);
		}
		
		return features;
	}
	
	/**
	 * Gets an array of Strings converted to json from 2 other String[] combined
	 * @param filterArray the smallest array
	 * @param propertyArray the longest array
	 * @return
	 */
	private String[] parseToSingleStringArray(String[] filterArray, String[] propertyArray) {
		List<String> list = new ArrayList<>();
		int index = 0;
		int numIters = propertyArray.length / filterArray.length;
		
		for(int i = 0; i < numIters; i++) { // 2x
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for(int j = 0; j < filterArray.length - 1; j++) {
				sb.append("\"" + filterArray[j] + "\":\"" + propertyArray[index++] + "\",");
			}
			sb.append("\"" + filterArray[filterArray.length - 1] + "\":\"" + propertyArray[index++] + "\"}");
			list.add(sb.toString());
		}
		return list.toArray(new String[list.size()]);
	}
	
	/**
	 * Returns a KwetsbaarObject[] for the given filter.
	 * @throws Exception
	 */
	public KwetsbaarObject[] getKwetsbareObjecten() throws Exception {
		List<KwetsbaarObject> kwetsbaarObjList = new ArrayList<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			NodeList elementList = doc.getElementsByTagName("gml:featureMember");
			if(elementList.getLength() == 0) {
				return new KwetsbaarObject[0];
			}
			
			for(int i = 0; i < elementList.getLength(); i++) {
				Node node = elementList.item(i);
				Element element = (Element)node;
				NodeList childList = element.getElementsByTagName("*");
				KwetsbaarObject obj = new KwetsbaarObject();
				obj.setAttributes(childList);
				kwetsbaarObjList.add(obj);
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new Exception(e);
		}
		
		return kwetsbaarObjList.toArray(new KwetsbaarObject[kwetsbaarObjList.size()]);
	}
}
