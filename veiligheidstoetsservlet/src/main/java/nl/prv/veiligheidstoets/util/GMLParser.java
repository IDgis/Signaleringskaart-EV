package nl.prv.veiligheidstoets.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.deegree.commons.xml.XMLParsingException;
import org.deegree.cs.exceptions.TransformationException;
import org.deegree.cs.exceptions.UnknownCRSException;
import org.deegree.cs.persistence.CRSManager;
import org.deegree.cs.refs.coordinatesystem.CRSRef;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.io.WKTReader;
import org.deegree.gml.GMLInputFactory;
import org.deegree.gml.GMLOutputFactory;
import org.deegree.gml.GMLStreamReader;
import org.deegree.gml.GMLStreamWriter;
import org.deegree.gml.GMLVersion;
import org.deegree.gml.geometry.GML3GeometryWriter;

import com.vividsolutions.jts.io.ParseException;

public class GMLParser {

	private GMLParser() {}
	
	public static Geometry parseToGeometry(String inputDoc) throws IOException {
		try {
			StringReader sr = new StringReader(inputDoc);
			XMLInputFactory xif = XMLInputFactory.newInstance();
			XMLStreamReader xsr = xif.createXMLStreamReader(sr);
			GMLStreamReader gsr = GMLInputFactory.createGMLStreamReader(GMLVersion.GML_30, xsr);
			return gsr.readGeometryOrEnvelope();
		}
		catch(XMLStreamException | XMLParsingException | UnknownCRSException e) {
			throw new IOException(e);
		}
	}
	
	public static String parseFromGeometry(Geometry geom) throws IOException {
		try {
			StringWriter sw = new StringWriter();
			XMLOutputFactory xof = XMLOutputFactory.newInstance();
	        XMLStreamWriter xtw = xof.createXMLStreamWriter(sw);
			GMLStreamWriter gtw = GMLOutputFactory.createGMLStreamWriter(GMLVersion.GML_30, xtw);
			GML3GeometryWriter ggw = new GML3GeometryWriter(gtw);
			ggw.export(geom);
			gtw.close();
			xtw.close();
			
			return sw.toString();
		}
		catch(XMLStreamException | UnknownCRSException | TransformationException e) {
			throw new IOException(e);
		}
	}
	
	public static String parseFromWKT(String wktGeometry) throws IOException {
		try {
			CRSRef crs = CRSManager.getCRSRef("EPSG:28992");	
			WKTReader reader = new WKTReader(crs);
			Geometry geom = reader.read(wktGeometry);
			return GMLParser.parseFromGeometry(geom);
		} catch(ParseException e) {
			throw new IOException(e);
		}
	}
}
