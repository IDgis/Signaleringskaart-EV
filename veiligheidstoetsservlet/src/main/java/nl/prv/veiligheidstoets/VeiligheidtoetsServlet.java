package nl.prv.veiligheidstoets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;

import org.deegree.cs.exceptions.TransformationException;
import org.deegree.cs.exceptions.UnknownCRSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import nl.prv.veiligheidstoets.util.LogStream;
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
	
	private String risicokaartWFSUrl;
	private String risicokaartUserName;
	private String risicokaartPassword;
	private String wktError;
	
	private String veiligheidstoetsWFSUrl;
	private TemplateHandler filterHandler;
	
	/**
	 * initializes the servlet
	 * reads configuration
	 * 
	 */
	@Override
	public void init() {
		System.out.println("init servlet");
		loadConfig();
	}


	/**
	 * Parses the configuration from the Veiligheidstoets.xml 
	 * 
	 */
	private void loadConfig(){
		
		try{
			//String configdir = "C:/Users/Kevin/Signaleringskaart/git/veiligheidstoetsservlet/config";
			String configdir = "/etc/veiligheidstoets";
			File configfile= new File(configdir + File.separator + "veiligheidstoets.xml");
			if(configfile.exists()){
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
				FileInputStream fis = new FileInputStream(configfile);
				Document configDoc = builder.parse(fis);
				risicokaartWFSUrl = getConfigProperty(configDoc,"risicokaartWFSUrl");
				risicokaartUserName = getConfigProperty(configDoc,"risicokaartUserName");
				risicokaartPassword = getConfigProperty(configDoc,"risicokaartPassword");
				veiligheidstoetsWFSUrl  = getConfigProperty(configDoc,"veiligheidstoetsWFSUrl");
				wktError = getConfigProperty(configDoc, "wktError");
				filterHandler = new TemplateHandler();
				fis.close();
			} 
			else {
				System.out.println("Config file missing " + configdir + File.separator + "veiligheidstoets.xml");
			}
		}
		catch(Exception e) {	
			System.out.println(e);
		}
	}
	
	/** 
	 * handles the post requests from the veiligheidstoets client
	 * 
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LogStream logStream = new LogStream(new ByteArrayOutputStream());
		Map<String, String> returnMessage = new HashMap<>();
		
		try(PrintWriter out = response.getWriter()) {
			
			//////////////////// Cross-Origin stuff /////////////////////////////////////
					
			List<String> incomingURLs = Arrays.asList(getServletContext().getInitParameter("incomingURLs").trim().split(","));
			String clientOrigin = request.getHeader("origin");
			
			String ipAddress = request.getHeader("x-forwarded-for");
			if(ipAddress == null) {
				ipAddress = request.getRemoteAddr();
			}
			
			response.setContentType("application/json");
			response.setHeader("Cache-control", "no-cache, no-store");
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Expires", "-1");
			
			int myIndex = incomingURLs.indexOf(clientOrigin);
			if(myIndex != -1) {
				response.addHeader("Access-Control-Allow-Origin", clientOrigin);
				response.setHeader("Access-Control-Allow-Methods", "POST");
				response.setHeader("Access-Control-Allow-Headers", "Content-Type");
				response.setHeader("Access-Control-Max-Age", "86400");
			}
			
			/////////////////////////////////////////////////////////////////////////////////
			
			Map <String, String[]> params = request.getParameterMap();
			Map <String,String> props = new HashMap<>();
			Iterator<Entry<String, String[]>> it  = params.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry <String,String[]> pairs = it.next();
				props.put(pairs.getKey(), pairs.getValue()[0]);
			}
			
			// Check request type
			if(props.containsKey("requesttype")) {
				if(props.get("requesttype").equals("polygonIsValid")) {
					// Check wktIsValid
					returnMessage = checkWktValid(props, "\"" + wktError + "\"");
				}
				else if(props.get("requesttype").equals("getEVFeatures")) {
					// getEVFeatures
					returnMessage = getEVFeatures(props);
				}
				else {
					returnMessage.put("error", "\"Request type is invalid!\"");
				}
			}
			else {
				returnMessage.put("error", "\"Request type is missing!\"");
			}
			
			JsonObject json = new JsonObject();
			JsonParser parser = new JsonParser();
			
			Iterator<String> iter = returnMessage.keySet().iterator();
			while(iter.hasNext()) {
				String key = iter.next();
				String value = returnMessage.get(key);
				json.add(key, parser.parse(value));
			}
			out.println(json);
			out.flush();
		}			
		catch(Exception e) {
			logStream.write(e.toString());
			throw new ServletException(e);
		}
	}
	
	/**
	 * Checks if the client entered a valid shape. Returns a Map with the key isValid with
	 * the value true/false.
	 * @param props
	 * @param wktError
	 * @return
	 */
	private Map<String, String> checkWktValid(Map<String, String> props, String wktError) {
		Map<String, String> checkResult = new HashMap<>();
		if(props.containsKey("wkt")) {
			String wktGeom = props.get("wkt");
			WKTReader reader = new WKTReader();
			Polygon poly;
			try {
				poly = (Polygon) reader.read(wktGeom);
				if(poly != null && !(poly.isValid())) {
					checkResult.put("isValid", "false");
					checkResult.put("error", wktError);
					return checkResult;
				}
				else {
					checkResult.put("isValid", "true");
					return checkResult;
				}
			} catch (ParseException e) {
				checkResult.put("isValid", "false");
				checkResult.put("error", "\"an error occurred: " + e.getMessage() + "\"");
			}
		}
		checkResult.put("error", "\"Wkt is missing!\"");
		return checkResult;
	}
	
	/**
	 * Gets the number of features or the values for the given properties in
	 * named in the postbody.
	 * @param props
	 * @throws Exception 
	 */
	private Map<String, String> getEVFeatures(Map<String, String> props) throws Exception {
		Map<String, String> features = new HashMap<>();
		// Check servicename
		String url = getServiceName(props);
		if(url == null) {
			features.put("error", "\"Servicename is missing!\"");
			return features;
		}
		else if(url.equals("INVALID")) {
			features.put("error", "\"Servicename is invalid!\"");
			return features;
		}
		
		// Check plangebied-wkt
		boolean plangebiedPresent = getPlangebiedWkt(props);
		if(!plangebiedPresent) {
			features.put("error", "\"Plangebied Wkt is missing!\"");
			return features;
		}
		
		// Check filter
		String filter = getTemplateFilter(props);
		if(filter == null) {
			features.put("error", "\"Filter is missing!\"");
			return features;
		}
		
		// Check properties
		SpatialQuery sq = new SpatialQuery(url, filter, risicokaartUserName, risicokaartPassword);
		return sq.getPropertyResult();
	}
	
	/**
	 * 
	 * @param props
	 * @return The url for the given servicename, INVALID if the servicename is invalid.
	 */
	private String getServiceName(Map<String, String> props) {
		if(props.containsKey("servicename")){
			if(props.get("servicename").equals("risicokaartWFS")) {
				return risicokaartWFSUrl;
			} 
			else if(props.get("servicename").equals("veiligheidstoetsWFS")) {
				return veiligheidstoetsWFSUrl;
			}
			return "INVALID";
		}
		return null;
	}
	
	/**
	 * Adds the gml to the props if plangebiedWkt is present
	 * @param props
	 * @return false if an error occurred. True otherwise.
	 * @throws Exception 
	 */
	private boolean getPlangebiedWkt(Map<String, String> props) throws Exception {
		if(props.containsKey("plangebiedWkt")){
			String wktGeom = props.get("plangebiedWkt");
			String gml = null;
			try {
				gml = WKT2GMLParser.parse(wktGeom);
			} 
			catch (ParseException | XMLStreamException | UnknownCRSException | TransformationException e) {
				throw new Exception(e);
			} 
			props.put("plangebiedgml", gml);
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the template filter
	 * @param props
	 * @return
	 */
	private String getTemplateFilter(Map<String, String> props) {
		if(props.containsKey("filter")) {
			String fn = props.get("filter");
			return filterHandler.getFilter(fn, props);
		}
		return null;
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
