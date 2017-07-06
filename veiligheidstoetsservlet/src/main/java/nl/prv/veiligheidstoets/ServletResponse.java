package nl.prv.veiligheidstoets;

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class ServletResponse {
	
	private static final Logger LOGGER = Logger.getLogger(ServletResponse.class.getName());

	private ServletResponse() {}
	
	/**
	 * Returns a JSON object with the error message or features as values
	 * @param responseMessage
	 * @return
	 */
	public static JsonObject convertToJson(Map<String, String> featureMap) {
		LOGGER.setLevel(Level.ALL);
		JsonObject json = new JsonObject();
		JsonParser parser = new JsonParser();
		
		Iterator<String> iter = featureMap.keySet().iterator();
		while(iter.hasNext()) {
			try {
				String key = iter.next();
				String value = featureMap.get(key);
				json.add(key, parser.parse(value));
			} catch(JsonParseException e) {
				LOGGER.log(Level.FATAL, e.getMessage(), e);
				json.add("error", parser.parse("\"" + e.getMessage() + "\""));
			}
		}
		return json;
	}
}
