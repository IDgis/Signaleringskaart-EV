package nl.prv.veiligheidstoets.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import nl.prv.veiligheidstoets.SpatialQuery;
import nl.prv.veiligheidstoets.util.GMLParser;
import nl.prv.veiligheidstoets.util.TemplateHandler;

public class EVFeaturesRequest extends VeiligheidtoetsRequest {
	
	private String filter;
	private String url;
	private TemplateHandler templateHandler;
	
	private static final Logger LOGGER = Logger.getLogger(EVFeaturesRequest.class.getName());
	
	protected EVFeaturesRequest(Map<String, String> props) {
		super(props);
		LOGGER.setLevel(Level.INFO);
		templateHandler = new TemplateHandler();
	}
	
	@Override
	public String initProperties() {
		String result = super.initProperties();
		if(result != null) {
			return result;
		}
		
		if(!props.containsKey("servicename")) {
			return "Service name is missing!";
		}
		String servicename = props.get("servicename");
		url = getConfigProperty(servicename + "Url");
		
		if(url == null || "".equals(url)) {
			LOGGER.log(Level.WARN, "Service name is invalid: " + servicename);
			return "Service name is invalid: " + servicename;
		}
		
		if(!props.containsKey("filter")) {
			return "Filter is missing!";
		}
		filter = props.get("filter");
		
		return null;
	}
	
	public Map<String, String> getResponse() {
		Map<String, String> features = new HashMap<>();
		
		try {
			String gml = GMLParser.parseFromWKT(plangebiedWkt);
			props.put("plangebiedgml", gml);
		} catch (IOException e) {
			LOGGER.log(Level.FATAL, e.getMessage(), e);
			features.put(ERROR, "\"" + e.getMessage() + "\"");
			return features;
		}
		
		String template = templateHandler.getFilter(filter, props);
		if(template == null) {
			features.put(ERROR, "\"Filter is invalid!\"");
			return features;
		}
		LOGGER.log(Level.DEBUG, "TEMPLATE:\n" + template);
		LOGGER.log(Level.DEBUG, "URL: " + url);
		SpatialQuery sq = new SpatialQuery(url, template);
		String errorMessage = sq.processFilter();
		if(errorMessage != null) {
			features.put(ERROR, "\"" + errorMessage.replaceAll("\"", "\'") + "\"");
			return features;
		}
		LOGGER.log(Level.INFO, "Getting features...");
		RequestProcessor rp = new RequestProcessor();
		return rp.getFeatureResult(sq.getFeatureResult());
	}
}
