package nl.prv.veiligheidstoets;

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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import nl.prv.veiligheidstoets.request.RequestFactory;
import nl.prv.veiligheidstoets.request.VeiligheidtoetsRequest;


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
	
	/**
	 * initializes the servlet
	 */
	@Override
	public void init() {
		LOGGER.log(Level.INFO, "init servlet...");
	}
	
	/** 
	 * handles the post requests from the veiligheidstoets client
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		try(PrintWriter out = response.getWriter()) {
			Map<String, String> returnMessage = new HashMap<>();
			response.setContentType("application/json");
			
			@SuppressWarnings("unchecked")
			Map <String, String[]> params = request.getParameterMap();
			Map <String,String> props = new HashMap<>();
			Iterator<Entry<String, String[]>> it  = params.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry <String,String[]> pairs = it.next();
				props.put(pairs.getKey(), pairs.getValue()[0]);
			}
			
			VeiligheidtoetsRequest toetsRequest = RequestFactory.createVeiligheidtoetsRequest(props);
			if(toetsRequest == null) {
				returnMessage.put(ERROR, "\"Requesttype is missing or invalid\"");
				LOGGER.log(Level.WARN, "Missing or invalid requesttype!");
				out.println(createJsonObject(returnMessage));
				out.flush();
				return;
			}
			
			String propertiesNotPresentMessage = toetsRequest.setupProperties();
			if(propertiesNotPresentMessage != null) {
				returnMessage.put(ERROR, "\"" + propertiesNotPresentMessage + "\"");
				LOGGER.log(Level.WARN, propertiesNotPresentMessage);
				out.println(createJsonObject(returnMessage));
				out.flush();
				return;
			}
			
			out.println(createJsonObject(toetsRequest.getResponse()));
			out.flush();
		}			
		catch(IOException | JsonParseException e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
		}
	}
	
	/**
	 * Returns a JSON object with the error message or features as values
	 * @param responseMessage
	 * @return
	 */
	private JsonObject createJsonObject(Map<String, String> responseMessage) {
		JsonObject json = new JsonObject();
		JsonParser parser = new JsonParser();
		
		Iterator<String> iter = responseMessage.keySet().iterator();
		while(iter.hasNext()) {
			String key = iter.next();
			String value = responseMessage.get(key);
			json.add(key, parser.parse(value));
		}
		LOGGER.log(Level.DEBUG, "Returning JSON: " + json);
		
		return json;
	}
}
