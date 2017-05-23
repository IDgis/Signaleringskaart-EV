package nl.prv.veiligheidstoets.request;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;


public class WktValidRequest extends VeiligheidtoetsRequest {
	
	private String wktError;
	
	private static final Logger LOGGER = Logger.getLogger(WktValidRequest.class.getName());

	protected WktValidRequest(Map<String, String> props) {
		super(props);
		LOGGER.setLevel(Level.DEBUG);
	}
	
	@Override
	public String initProperties() {
		String result = super.initProperties();
		if(result != null) {
			return result;
		}
		
		wktError = getConfigProperty("wktError");
		
		return null;
	}
	
	@Override
	public Map<String, String> getResponse() {
		Map<String, String> checkResult = new HashMap<>();
		WKTReader reader = new WKTReader();
		try {
			Geometry geom = reader.read(plangebiedWkt);
			if(geom != null && !(geom.isValid())) {
				checkResult.put("isValid", "false");
				checkResult.put("error", "\"" + wktError + "\"");
				LOGGER.log(Level.WARN, wktError);
				return checkResult;
			}
			else {
				checkResult.put("isValid", "true");
				return checkResult;
			}
		} catch (ParseException e) {
			LOGGER.log(Level.FATAL, e.getMessage(), e);
			checkResult.put("error", "\"" + e.getMessage() + "\"");
			return checkResult;
		}
	}
}
