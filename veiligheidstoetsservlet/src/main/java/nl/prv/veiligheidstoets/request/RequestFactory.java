package nl.prv.veiligheidstoets.request;

import java.util.Map;

public class RequestFactory {

	private RequestFactory() {}
	
	public static VeiligheidtoetsRequest createVeiligheidtoetsRequest(Map<String, String> props) {		
		if(!props.containsKey("requesttype")) {
			return null;
		}
		String requesttype = props.get("requesttype");
		switch(requesttype) {
		case "polygonIsValid":
			return new WktValidRequest(props);
		case "getEVFeatures":
			return new EVFeaturesRequest(props);
		case "getKOFeatures":
			return new KOFeaturesRequest(props);
		default:
			return null;
		}
	}
}
