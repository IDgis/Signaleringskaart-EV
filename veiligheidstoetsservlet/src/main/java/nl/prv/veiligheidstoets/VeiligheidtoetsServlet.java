package nl.prv.veiligheidstoets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

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
	private static final String PLANGEBIEDGML = "plangebiedgml";
	private static final String PLANGEBIEDWKT = "plangebiedWkt";
	private static final String REQUESTTYPE = "requesttype";
	private static final String SERVICENAME = "servicename";
	private static final String SERVICENAMEEV = "servicenameEV";
	private static final String SERVICENAMEKO = "servicenameKO";
	
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
				else if("getKOFeatures".equals(props.get(REQUESTTYPE))) {
					// get KwetsbareObjecten
					returnMessage = getKOFeatures(props);
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
			if(!props.containsKey(SERVICENAME)) {
				features.put(ERROR, "\"Servicename is missing!\"");
				return features;
			}
			String url = getServiceName(props.get(SERVICENAME));
			if(url == null) {
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
			String filter = props.get(FILTER);
			String template = templateHandler.getFilter(filter, props);
			LOGGER.log(Level.DEBUG, "TEMPLATE:\n" + template);
			SpatialQuery sq = new SpatialQuery(url, template);
			
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
	private String getServiceName(String serviceName) {
		switch(serviceName) {
		case "risicokaartWFS":
			return risicokaartWFSUrl;
		case "veiligheidstoetsWFS":
			return veiligheidstoetsWFSUrl;
		case "basisnetWFS":
			return basisnetWFSUrl;
		case "ruimtelijkeplannenWFS":
			return ruimtelijkeplannenWFSUrl;
		default:
			return null;	
		}
	}
	
	/**
	 * Adds the gml to the props if plangebiedWkt is present
	 * @param props - All properties given by the client
	 * @return false if an error occurred. True otherwise.
	 * @throws IOException 
	 */
	private boolean isPlangebiedWktPresent(Map<String, String> props) throws IOException {
		if(props.containsKey(PLANGEBIEDWKT)){
			String wktGeom = props.get(PLANGEBIEDWKT);
			
			String gml = null;
			try {
				gml = WKT2GMLParser.parse(wktGeom);
			} 
			catch (IOException e) {
				throw new IOException(e);
			} 
			props.put(PLANGEBIEDGML, gml);
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the Kwetsbare Objecten within plangebiedWkt entered in the request
	 * @param props - The postbody entered by the client
	 * @return A HashMap with the Kwetsbare Objecten or a message with no features found
	 */
	private Map<String, String> getKOFeatures(Map<String, String> props) {
		Map<String, String> features = new HashMap<>();
		try {
			// Check servicename
			String urlEV = null;
			String urlKO = null;
			if(!(props.containsKey(SERVICENAMEEV) && props.containsKey(SERVICENAMEKO))) {
				features.put(ERROR, "\"Servicename is missing!\"");
				return features;
			}
			urlEV = getServiceName(props.get(SERVICENAMEEV));
			urlKO = getServiceName(props.get(SERVICENAMEKO));
			if(urlEV == null || urlKO == null) {
				features.put(ERROR, "\"Servicename is invalid!\"");
				return features;
			}
			
			// Check plangebied-wkt
			boolean plangebiedPresent = isPlangebiedWktPresent(props);
			if(!plangebiedPresent) {
				features.put(ERROR, "\"Plangebied Wkt is missing!\"");
				return features;
			}
			
			features = getKOFeatureResult(props, urlEV, urlKO);
		}
		catch(IOException e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
			features.put(ERROR, "\"" + e.getMessage() + "\"");
			return features;
		}
		return features;
	}
	
	private Map<String, String> getKOFeatureResult(Map<String, String> props, String urlEV, String urlKO) throws IOException {
		Map<String, String> features = new HashMap<>();
		// Get new MultiPolygon Geometry from first template
		if(!(props.containsKey("filterEV") && props.containsKey("filterKO"))) {
			features.put(ERROR, "\"Template is missing! Please give up 2 templates!\"");
			return features;
		}
		String templateEV = templateHandler.getFilter(props.get("filterEV"), props);
		LOGGER.log(Level.DEBUG, "TEMPLATE_EV:\n" + templateEV);
		SpatialQuery sq = new SpatialQuery(urlEV, templateEV);
		Geometry geometry = sq.getRisicogebiedGeom(props.get(PLANGEBIEDWKT));
		if(geometry == null) {
			features.put("message", "NO_FEATURES_FOUND");
			return features;
		}
		
		WKTWriter writer = new WKTWriter();
		String geomWkt = writer.write(geometry);
		String gml = WKT2GMLParser.parse(geomWkt);
		props.put(PLANGEBIEDGML, gml);
		
		// Get Kwetsbare Objecten within the MultiPoint object
		String templateKO = templateHandler.getFilter(props.get("filterKO"), props);
		LOGGER.log(Level.DEBUG, "TEMPLATE_KO:\n" + templateKO);
		sq = new SpatialQuery(urlKO, templateKO);
		return sq.getKOFeatures();
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
