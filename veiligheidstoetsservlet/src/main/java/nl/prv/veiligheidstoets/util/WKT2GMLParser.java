package nl.prv.veiligheidstoets.util;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.deegree.cs.exceptions.TransformationException;
import org.deegree.cs.exceptions.UnknownCRSException;
import org.deegree.cs.persistence.CRSManager;
import org.deegree.cs.refs.coordinatesystem.CRSRef;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.io.WKTReader;
import org.deegree.gml.GMLOutputFactory;
import org.deegree.gml.GMLStreamWriter;
import org.deegree.gml.GMLVersion;
import org.deegree.gml.geometry.GML3GeometryWriter;

import com.vividsolutions.jts.io.ParseException;

public class WKT2GMLParser {
	
	private WKT2GMLParser() {}
	
	public static String parse(String wktGeometry) throws IOException {
		try {
			CRSRef crs = CRSManager.getCRSRef("EPSG:28992");	
			WKTReader reader = new WKTReader(crs);
			Geometry geom = reader.read(wktGeometry);
			XMLOutputFactory xof = XMLOutputFactory.newInstance();
			StringWriter sw = new StringWriter();
	        XMLStreamWriter xtw = xof.createXMLStreamWriter(sw);
			GMLStreamWriter gtw = GMLOutputFactory.createGMLStreamWriter(GMLVersion.GML_32, xtw);
			GML3GeometryWriter ggw = new GML3GeometryWriter(gtw);
			ggw.export(geom);
			gtw.close();
			xtw.close();
			return sw.toString();
		} catch(ParseException | XMLStreamException | UnknownCRSException | TransformationException e) {
			throw new IOException(e);
		}
	}
}