<?xml version="1.0" encoding="UTF-8"?>
<wfs:GetFeature xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"
 xmlns:ogc="http://www.opengis.net/ogc"
 xmlns:gml="http://www.opengis.net/gml"
 xmlns:wfs="http://www.opengis.net/wfs"
 service="WFS"
 version="1.1.0"
 outputFormat="text/xml; subtype=gml/3.2.1">
    <wfs:Query typeName="verblijfsobject_ov">
        <ogc:PropertyName>gebruiksdoel</ogc:PropertyName>
        <ogc:PropertyName>oppervlakte</ogc:PropertyName>
        <ogc:Filter>
	        <ogc:Within>
	            <ogc:PropertyName>geom</ogc:PropertyName>
	            	$plangebiedgml$                            
	        </ogc:Within>
        </ogc:Filter>
    </wfs:Query>
</wfs:GetFeature>