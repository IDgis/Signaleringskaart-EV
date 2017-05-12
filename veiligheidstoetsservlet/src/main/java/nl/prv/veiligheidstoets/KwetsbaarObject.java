package nl.prv.veiligheidstoets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class KwetsbaarObject {

	private Point point;
	private Map<String, String> properties;
	
	public void setPoint(double x, double y) {
		GeometryFactory fac = new GeometryFactory();
		point = fac.createPoint(new Coordinate(x, y));
	}
	
	public void setPoint(String point) {
		String[] points = point.split(" ");
		setPoint(Double.parseDouble(points[0]), Double.parseDouble(points[1]));
	}
	
	public Point getPoint() {
		return point;
	}
	
	public String getPointWKT() {
		return "POINT(" + getCoordX() + " " + getCoordY() + ")";
	}
	
	public double getCoordX() {
		return point.getCoordinate().x;
	}
	
	public double getCoordY() {
		return point.getCoordinate().y;
	}
	
	public void setPosition(NodeList list) {
		for(int i = 0; i < list.getLength(); i++) {
			Element node = (Element)list.item(i);
			if(node.getNodeName().endsWith(":pos")) {
				setPoint(node.getTextContent().trim());
			}
		}
	}
	
	public void setProperties(NodeList nodeList, List<String> propertyNames) {
		properties = new HashMap<>();
		// Go through all tags in the nodeList
		for(int i = 0; i < nodeList.getLength(); i++) {
			Element node = (Element)nodeList.item(i);
			// Go through the propertyNames if they exist and add them to the map
			for(int j = 0; j < propertyNames.size(); j++) {
				if(node.getNodeName().toLowerCase().endsWith(":" + propertyNames.get(j)) ||
					node.getNodeName().toUpperCase().endsWith(":" + propertyNames.get(j))) {
						properties.put(propertyNames.get(j), node.getTextContent());
				}
			}
		}
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
}
