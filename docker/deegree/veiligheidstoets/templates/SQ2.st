<?xml version="1.0" encoding="UTF-8"?>
<wfs:GetFeature  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"
    xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml"
    xmlns:wfs="http://www.opengis.net/wfs" service="WFS" version="1.1.0"
    outputFormat="text/xml; subtype=gml/3.2.1">
    <wfs:Query typeName="basisnet_spoor_ov_pr10_6">
    	<ogc:PropertyName>PR10_6</ogc:PropertyName>
        <ogc:PropertyName>PR10_7</ogc:PropertyName>
        <ogc:PropertyName>PR10_8</ogc:PropertyName>
        <ogc:PropertyName>PAG</ogc:PropertyName>
        <ogc:PropertyName>A</ogc:PropertyName>
        <ogc:PropertyName>B2</ogc:PropertyName>
        <ogc:PropertyName>B3</ogc:PropertyName>
        <ogc:PropertyName>C3</ogc:PropertyName>
        <ogc:PropertyName>D3</ogc:PropertyName>
        <ogc:PropertyName>D4</ogc:PropertyName>
        <ogc:PropertyName>AWK</ogc:PropertyName>
        <ogc:PropertyName>B2WK</ogc:PropertyName>
        <ogc:PropertyName>Breedte</ogc:PropertyName>
        <ogc:Filter>
	        <ogc:Intersects>
	            <ogc:PropertyName>the_geom</ogc:PropertyName>
	            $plangebiedgml$                         
	        </ogc:Intersects>
        </ogc:Filter>
    </wfs:Query>
</wfs:GetFeature>
