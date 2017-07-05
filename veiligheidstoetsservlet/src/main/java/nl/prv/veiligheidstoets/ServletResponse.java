package nl.prv.veiligheidstoets;

import java.util.Iterator;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

public class ServletResponse {

	private ServletResponse() {}
	
	/**
	 * Returns a JSON object with the error message or features as values
	 * @param responseMessage
	 * @return
	 */
	public static JsonObject convertToJson(Map<String, String> featureMap) {
		JsonObject json = new JsonObject();
		JsonParser parser = new JsonParser();
		
		Iterator<String> iter = featureMap.keySet().iterator();
		while(iter.hasNext()) {
			try {
				String key = iter.next();
				String value = featureMap.get(key);
				json.add(key, parser.parse(value));
			} catch(JsonParseException e) {
				json.add("error", parser.parse("\"" + e.getMessage() + "\""));
			}
		}
		return json;
	}
}
