package nl.prv.veiligheidstoets;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class KwetsbaarObject {

	private Point point;
	private String name;
	private double id;
	
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
	
	public double getCoordX() {
		return point.getCoordinate().x;
	}
	
	public double getCoordY() {
		return point.getCoordinate().y;
	}
	
	public void setId(double id) {
		this.id = id;
	}
	
	public double getId() {
		return id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setAttributes(NodeList list) {
		for(int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if(node.getNodeName().endsWith(":pos")) {
				setPoint(node.getTextContent().trim());
			}
			else if(node.getNodeName().endsWith(":AOBJECTID")) {
				setId(Integer.parseInt(node.getTextContent().trim()));
			}
			else if(node.getNodeName().endsWith(":NAAM")) {
				setName(node.getTextContent().replaceAll("\"", "\'").trim());
			}
		}
	}
	
	public boolean isWithin(Geometry geom) {
		return point.coveredBy(geom);
	}
}
