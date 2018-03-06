/*******************************************************************************
* symbiote semantic Library
* Version: 0.1
* Date: 12/01/2018
* Author: Unidata
* Company: Unidata
*
* Semantic representation of symbiote resources
*
*
* Revision  Description
* ========  ===========
* 
* 0.1      Initial release
*******************************************************************************/
#include "semantic_resources.h"

Semantic::Semantic( String internalId, String name, String url, uint8_t capNum, Capability* cap, uint8_t obsNumber, Property* property)
{
	_capabilityNumber = capNum;
	_capability = cap;
	_obsPropertyNumber = obsNumber;
	_obsProperty = property;
	_internalId = internalId;
	_name = name;
	_url = url;
}

String Semantic::returnSemanticString()
{
	String tmpString = "";
	tmpString += "{\"@c\":\".SmartDevice\",\"connectedTo\":\"*tba*\",\"available\":\"True\",\"hasResource\":{\"@c\":\".Resource\",\"id\":\"";
	tmpString += _internalId;
	tmpString += "\",\"name\":\"";
	tmpString += _name;
	tmpString += "\",\"description\":\"NA\",\"labels\":\"NA\",\"interworkingServiceURL\":\"";
	tmpString += _url;
	tmpString += "\",\"locatedAt\":\"*tba*\",\"services\":null},\"capabilities\":[";
	for (uint8_t i = 0; i < _capabilityNumber-1; i++) {
			tmpString += _capability[i].returnSemanticString();
			tmpString += ",";
		}
	tmpString += _capability[_capabilityNumber-1].returnSemanticString();
	tmpString += "],\"observesProperty\":[";
	//tmpString += "\"observesProperty\":[";
	for (uint8_t i = 0; i < _obsPropertyNumber-1; i++) {
			tmpString += _obsProperty[i].returnSemanticString();
			tmpString += ",";
		}
	tmpString += _obsProperty[_obsPropertyNumber-1].returnSemanticString();
	tmpString += "]}";
	return tmpString;
}

Capability::Capability(String name, uint8_t param_num, Parameter* parameter)
{
	_name = name;
	_paramNum = param_num;
	_param = parameter;
}

String Capability::returnSemanticString()
{
	String tmpString = "";
	//tmpString += "\"capabilities\":[{\"name\":\"";
	tmpString += "{\"name\":\"";
	tmpString += _name;
	tmpString += "\",\"@c\":\".Capability\",\"inputParameter\":[";
	for (uint8_t i = 0; i < _paramNum-1; i++) {
			tmpString += _param[i].returnSemanticString();
			tmpString += ",";
		}
	tmpString += _param[_paramNum-1].returnSemanticString();
	tmpString += "],\"actsOn\":\"*tba*\",\"affects\":[\"";
	for (uint8_t i = 0; i < _paramNum-1; i++) {
			tmpString += _param[i].getName();
			tmpString += "\",\"";
		}
	tmpString += _param[_paramNum-1].getName();
	tmpString += "\"]}";
	return tmpString;
}

Parameter::Parameter(String name, String datatype, String restrictionMin, String restrictionMax)
{
	_name = name;
	_dataType = datatype;
	_restrictionMin = restrictionMin;
	_restrictionMax = restrictionMax;
}

String Parameter::returnSemanticString()
{
	String tmpString = "";
	tmpString = "{\"name\":\"";
	tmpString += _name;
	tmpString += "\",\"@c\":\".Parameter\",\"isArray\":false,\"datatype\":\"";
	tmpString += _dataType;
	tmpString += "\",\"mandatory\":true,\"restrictions\":[{\"min\":";
	tmpString += _restrictionMin;
	tmpString += ",\"max\":";
	tmpString += _restrictionMax;
	tmpString += "}]}";
	return tmpString;
}

String Parameter::getName()
{
	return _name;
}

uint8_t Parameter::getMinRestriction()
{
	return _restrictionMin.toInt();
}

uint8_t Parameter::getMaxRestriction()
{
	return _restrictionMax.toInt();
}

Property::Property(String name, String description)
{
	_name = name;
	_description = description;
}

String Property::returnSemanticString()
{
	String tmpString = "";
	tmpString = "{\"@c\":\".Property\",\"name\":\"" + _name + "\",\"description\":\"" + _description + "\"}";
	return tmpString;
}

String Property::getName()
{
	return _name;
}
