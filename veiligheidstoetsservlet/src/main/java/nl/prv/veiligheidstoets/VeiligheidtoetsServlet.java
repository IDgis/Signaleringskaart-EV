package nl.prv.veiligheidstoets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import nl.prv.veiligheidstoets.util.TemplateHandler;
import nl.prv.veiligheidstoets.util.WKT2GMLParser;


/**
 * Servlet that handles spatial requests for veiligheidtoets
 *  
 * @author Linda Vels
 * @version 1.0 16-09-2013
 */

@WebServlet(urlPatterns = {"/toets"})
public class VeiligheidtoetsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOGGER = Logger.getLogger(VeiligheidtoetsServlet.class.getName());
	static {
		LOGGER.setLevel(Level.ALL);
	}
	
	private static final String ERROR = "error";
	private static final String FILTER = "filter";
	private static final String ISVALID = "isValid";
	private static final String REQUESTTYPE = "requesttype";
	private static final String SERVICENAME = "servicename";
	
	private String basisnetWFSUrl;
	private String risicokaartWFSUrl;
	private String veiligheidstoetsWFSUrl;
	private String ruimtelijkeplannenWFSUrl;
	private String wktError;
	
	private TemplateHandler templateHandler;
	
	/**
	 * initializes the servlet
	 * reads configuration
	 * 
	 */
	@Override
	public void init() {
		LOGGER.log(Level.INFO, "init servlet...");
		loadConfig();
	}


	/**
	 * Parses the configuration from the Veiligheidstoets.xml 
	 * 
	 */
	private void loadConfig(){
		
		try{
			String configdir = "/etc/veiligheidstoets";
			File configfile= new File(configdir + File.separator + "veiligheidstoets.xml");
			if(configfile.exists()){
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
				FileInputStream fis = new FileInputStream(configfile);
				Document configDoc = builder.parse(fis);
				basisnetWFSUrl = getConfigProperty(configDoc, "basisnetWFSUrl");
				risicokaartWFSUrl = getConfigProperty(configDoc,"risicokaartWFSUrl");
				veiligheidstoetsWFSUrl  = getConfigProperty(configDoc,"veiligheidstoetsWFSUrl");
				ruimtelijkeplannenWFSUrl = getConfigProperty(configDoc, "ruimtelijkeplannenWFSUrl");
				wktError = getConfigProperty(configDoc, "wktError");
				templateHandler = new TemplateHandler();
				fis.close();
			} 
			else {
				LOGGER.log(Level.FATAL, String.format("Config file missing %s%sveiligheidstoets.xml", configdir, File.separator));
			}
		}
		catch(Exception e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
		}
	}
	
	/** 
	 * handles the post requests from the veiligheidstoets client
	 * 
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> returnMessage = new HashMap<>();
		
		try(PrintWriter out = response.getWriter()) {
			
			response.setContentType("application/json");
			
			Map <String, String[]> params = request.getParameterMap();
			Map <String,String> props = new HashMap<>();
			Iterator<Entry<String, String[]>> it  = params.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry <String,String[]> pairs = it.next();
				props.put(pairs.getKey(), pairs.getValue()[0]);
			}
			
			// Check request type
			if(props.containsKey(REQUESTTYPE)) {
				if("polygonIsValid".equals(props.get(REQUESTTYPE))) {
					// Check wktIsValid
					returnMessage = getWktValidMessage(props, "\"" + wktError + "\"");
				}
				else if("getEVFeatures".equals(props.get(REQUESTTYPE))) {
					// getEVFeatures
					returnMessage = getEVFeatures(props);
				}
				else {
					returnMessage.put(ERROR, "\"Request type is invalid: " + props.get(REQUESTTYPE) + "\"");
				}
			}
			else {
				returnMessage.put(ERROR, "\"Request type is missing!\"");
			}
			
			JsonObject json = new JsonObject();
			JsonParser parser = new JsonParser();
			
			Iterator<String> iter = returnMessage.keySet().iterator();
			while(iter.hasNext()) {
				String key = iter.next();
				String value = returnMessage.get(key);
				json.add(key, parser.parse(value));
			}
			
			LOGGER.log(Level.DEBUG, "Returning JSON: " + json);
			out.println(json);
			out.flush();
		}			
		catch(IOException | JsonParseException e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
		}
	}
	
	/**
	 * Checks if the client entered a valid shape. Returns a Map with the key isValid with
	 * the value true/false.
	 * @param props - All properties given by the client
	 * @param wktError - The error message if the wkt is invalid
	 * @return isValid with a value true/false. If false it also returns an error message
	 */
	private Map<String, String> getWktValidMessage(Map<String, String> props, String wktError) {
		Map<String, String> checkResult = new HashMap<>();
		if(props.containsKey("wkt")) {
			String wktGeom = props.get("wkt");
			WKTReader reader = new WKTReader();
			try {
				Polygon poly = (Polygon) reader.read(wktGeom);
				if(poly != null && !(poly.isValid())) {
					checkResult.put(ISVALID, "false");
					checkResult.put(ERROR, wktError);
					return checkResult;
				}
				else {
					checkResult.put(ISVALID, "true");
					return checkResult;
				}
			} catch (ParseException e) {
				LOGGER.log(Level.FATAL, e.toString(), e);
				checkResult.put(ISVALID, "false");
				checkResult.put(ERROR, "\"an error occurred: " + e.getMessage() + "\"");
				return checkResult;
			}
		}
		checkResult.put(ERROR, "\"Wkt is missing!\"");
		return checkResult;
	}
	
	/**
	 * Gets the number of features or the values for the given properties in
	 * named in the postbody.
	 * @param props - All properties given by the client
	 */
	private Map<String, String> getEVFeatures(Map<String, String> props) {
		Map<String, String> features = new HashMap<>();
		try {
			// Check servicename
			String url = getServiceName(props, 0);
			if(url == null) {
				features.put(ERROR, "\"Servicename is missing!\"");
				return features;
			}
			else if("INVALID".equals(url)) {
				features.put(ERROR, "\"Servicename is invalid: " + props.get(SERVICENAME) + "\"");
				return features;
			}
			
			// Check plangebied-wkt
			boolean plangebiedPresent = isPlangebiedWktPresent(props);
			if(!plangebiedPresent) {
				features.put(ERROR, "\"Plangebied Wkt is missing!\"");
				return features;
			}
			
			// Check templates
			if(!props.containsKey(FILTER)) {
				features.put(ERROR, "\"Template is missing!\"");
				return features;
			}
			String[] templates = props.get(FILTER).split("x");
			String template = templateHandler.getFilter(templates[0], props);
			LOGGER.log(Level.DEBUG, "TEMPLATE:\n" + template);
			SpatialQuery sq = new SpatialQuery(url, template);
			
			// Check for second template
			if(templates.length > 1) {
				LOGGER.log(Level.INFO, "Second template found...");
				List<KwetsbaarObject> kwObjects = sq.getKwetsbareObjecten();
				LOGGER.log(Level.DEBUG, "Number of kwetsbare objecten returned: " + kwObjects.size());
				url = getServiceName(props, 1);
				List<KwetsbaarObject> kwObjectsInBuffer = processSecondTemplate(kwObjects, url, props);
				return sq.getFeatureResult(kwObjectsInBuffer);
			}
			
			// Check properties
			LOGGER.log(Level.INFO, "Getting features...");
			features = sq.getFeatureResult();
		}
		catch(Exception e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
			features.put(ERROR, "\"" + e.getMessage() + "\"");
			return features;
		}
		return features;
	}
	
	/**
	 * 
	 * @param props - All properties given by the client
	 * @param index the index of the servicename if more are given. 0 for the default servicename,
	 * 1 for the second filter if present.
	 * @return The url for the given servicename, INVALID if the servicename is invalid.
	 */
	private String getServiceName(Map<String, String> props, int index) {
		if(props.containsKey(SERVICENAME)){
			String[] urls = props.get(SERVICENAME).split("x");
			if(index == 1 && urls.length != 2) {
				return null;
			}
			if("risicokaartWFS".equals(urls[index])) {
				return risicokaartWFSUrl;
			} 
			else if( "veiligheidstoetsWFS".equals(urls[index])) {
				return veiligheidstoetsWFSUrl;
			}
			else if("basisnetWFS".equals(urls[index])) {
				return basisnetWFSUrl;
			}
			else if("ruimtelijkeplannenWFS".equals(urls[index])) {
				return ruimtelijkeplannenWFSUrl;
			}
			return "INVALID";
		}
		return null;
	}
	
	/**
	 * Adds the gml to the props if plangebiedWkt is present
	 * @param props - All properties given by the client
	 * @return false if an error occurred. True otherwise.
	 * @throws IOException 
	 */
	private boolean isPlangebiedWktPresent(Map<String, String> props) throws IOException {
		if(props.containsKey("plangebiedWkt")){
			String wktGeom = props.get("plangebiedWkt");
			
			String gml = null;
			try {
				gml = WKT2GMLParser.parse(wktGeom);
			} 
			catch (IOException e) {
				throw new IOException(e);
			} 
			props.put("plangebiedgml", gml);
			return true;
		}
		return false;
	}
	
	/**
	 * Returns a List of Kwetsbare Objecten that are inside the buffer.
	 * @param kwObjects - A List of all KwetsbareObjecten found by the first template
	 * @param url - The URL for the service
	 * @param props - All properties given by the client
	 */
	private List<KwetsbaarObject> processSecondTemplate(List<KwetsbaarObject> kwObjects, String url, Map<String, String> props) {
		if(kwObjects.isEmpty()) {
			return new ArrayList<>();
		}
		LOGGER.log(Level.DEBUG, "Kwetsbare objecten found for second template: " + kwObjects.size());
		List<KwetsbaarObject> kwObjectsInBuffer = new ArrayList<>();
		for(int i = 0; i < kwObjects.size(); i++) {
			String pointGeom = kwObjects.get(i).getPointWKT();
			String gml = null;
			try {
				gml = WKT2GMLParser.parse(pointGeom);
			} 
			catch (IOException e) {
				LOGGER.log(Level.FATAL, e.toString(), e);
			} 
			props.put("plangebiedgml", gml);
			
			String[] templates = props.get(FILTER).split("x");
			String template = templateHandler.getFilter(templates[1], props);
			
			try {
				SpatialQuery sq2 = new SpatialQuery(url, template);
				LOGGER.log(Level.DEBUG, "Processing: " + (i + 1) + " of " + kwObjects.size());
				KwetsbaarObject bufferResult = sq2.getKwetsbaarObjectInBuffer(kwObjects.get(i));
				if(bufferResult != null) {
					kwObjectsInBuffer.add(bufferResult);
				}
			}
			catch(Exception e) {
				LOGGER.log(Level.FATAL, e.toString(), e);
			}
		}
		LOGGER.log(Level.DEBUG, "Number of kwObjectsInBuffer: " + kwObjectsInBuffer.size());
		return kwObjectsInBuffer;
	}

	/**
	 * parses the value of a property with a given name from the configuration Document
	 * 
	 * @param doc 
	 * @param propName
	 * @return the value of a given property
	 */
	private String getConfigProperty(Document doc, String propName){
		Element root = doc.getDocumentElement();
		return root.getAttribute(propName);
	}
}
