package nl.prv.veiligheidstoets;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class KwetsbaarObject {

	private Point point;
	private String gebruiksdoel;
	private String oppervlakte;
	
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
	
	public void setGebruiksDoel(String gebruiksdoel) {
		this.gebruiksdoel = gebruiksdoel;
	}
	
	public String getGebruiksDoel() {
		return gebruiksdoel;
	}
	
	public void setOppervlakte(String oppervlakte) {
		this.oppervlakte = oppervlakte;
	}
	
	public String getOppervlakte() {
		return oppervlakte;
	}
	
	public void setPosition(NodeList list) {
		for(int i = 0; i < list.getLength(); i++) {
			Element node = (Element)list.item(i);
			if(node.getNodeName().endsWith(":pos")) {
				setPoint(node.getTextContent().trim());
			}
		}
	}
	
	public void setAttributes(NodeList list) {
		for(int i = 0; i < list.getLength(); i++) {
			Element node = (Element)list.item(i);
			if(node.getNodeName().endsWith(":gebruiksdoel")) {
				setGebruiksDoel(node.getTextContent());
			}
			else if(node.getNodeName().endsWith(":oppervlakte")) {
				setOppervlakte(node.getTextContent());
			}
		}
	}
	
	public boolean isWithin(Geometry geom) {
		return point.coveredBy(geom);
	}
}
