<?xml version="1.0" encoding="UTF-8"?>
<wfs:GetFeature  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"
    xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml"
    xmlns:wfs="http://www.opengis.net/wfs" service="WFS" version="1.1.0"
    outputFormat="text/xml; subtype=gml/3.2.1"
    resultType="hits">
    <wfs:Query typeName="Besluitvlak_P">
        <ogc:Filter>
            <ogc:And>
                <ogc:PropertyIsEqualTo>
                    <ogc:PropertyName>plangebied</ogc:PropertyName>
                    <ogc:Literal>NL.IMRO.9923.VerordeningOv01-va01</ogc:Literal>
                </ogc:PropertyIsEqualTo>
                <ogc:PropertyIsEqualTo>
                    <ogc:PropertyName>identificatie</ogc:PropertyName>
                    <ogc:Literal>NL.IMRO.9923PRIMA201200000004564</ogc:Literal>
                </ogc:PropertyIsEqualTo>
                <ogc:Intersects>
                    <ogc:PropertyName>geometrie</ogc:PropertyName>
                    $plangebiedgml$         
                </ogc:Intersects>   
            </ogc:And>    
        </ogc:Filter>
    </wfs:Query>
</wfs:GetFeature>
                    