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

//Semantic::Semantic(String name, String url, uint8_t capNum, Capability* cap, uint8_t obsNumber, Property* property)
Semantic::Semantic(String name, uint8_t capNum, Capability* cap, uint8_t obsNumber, Property* property)
{
	_capabilityNumber = capNum;
	_capability = cap;
	_obsPropertyNumber = obsNumber;
	_obsProperty = property;
	_symId = "";
	_name = name;
	//_url = url;
}

String Semantic::returnSensorSemanticString()
{
	String tmpString = "";
	tmpString += "{\"@c\":\".StationarySensor\",\"id\":\"\",\"name\":\"SENS-";
	tmpString += _name;
	tmpString += "\",\"description\":null,\"interworkingServiceURL\":\"";
	tmpString += _url;
	tmpString += "\",\"locatedAt\":null,\"services\":null,\"observesProperty\":";
	if (_obsPropertyNumber > 0) {
		// handle the case if no obsProperty available (e.g. is only an actuator)
		tmpString += "[";
		for (uint8_t i = 0; i < _obsPropertyNumber-1; i++) {
			tmpString += "\"";
			tmpString += _obsProperty[i].getName();
			tmpString += "\",";
		}
		tmpString += "\"";
		tmpString += _obsProperty[_obsPropertyNumber-1].getName();
		tmpString += "\"]";
	} else {
		tmpString += "null";
	}
	tmpString += "}";
	return tmpString;
}

String Semantic::returnActuatorSemanticString()
{
	String tmpString = "";
	tmpString += "{\"@c\":\".Actuator\",\"id\":\"\",\"name\":\"ACT-";
	tmpString += _name;
	tmpString += "\",\"description\":null,\"interworkingServiceURL\":\"";
	tmpString += _url;
	tmpString += "\",\"locatedAt\":null,\"services\":null,\"capabilities\":";
	if (_capabilityNumber > 0) {
		// handle the case if no obsProperty available (e.g. is only an actuator)
		tmpString += "[";
		for (uint8_t i = 0; i < _capabilityNumber-1; i++) {
			tmpString += _capability[i].returnSemanticString();
			tmpString += ",";
		}
		tmpString += _capability[_capabilityNumber-1].returnSemanticString();
		tmpString += "]";
	} else {
		tmpString += "null";
	}
	tmpString += "}";
	return tmpString;
}

String Semantic::getName()
{
	return _name;
}

String Semantic::getURL()
{
	return _url;
}

void Semantic::setSymId(String symId)
{
	_symId = symId;
}

String Semantic::getParamName(uint8_t capNum, uint8_t paramNum)
{
	if (capNum < _capabilityNumber && paramNum < getParamNum(capNum)) {
		return _capability[capNum].getParametersName(paramNum);
	}
}

uint8_t Semantic::getParamNum(uint8_t capNum)
{
	if (capNum < _capabilityNumber) {
		return _capability[capNum].getParametersNum();
	} 
}

uint8_t Semantic::getCapabilityNum()
{
	return _capabilityNumber;
}

String Semantic::getCapabilityName(uint8_t capNum)
{
	if (capNum<_capabilityNumber) {
		return _capability[capNum].getName();
	} else return "ERROR";
}

bool Semantic::actuateParameterOfCapability(uint8_t capNum, uint8_t paramNum, int in)
{

	if (capNum < _capabilityNumber && paramNum < _capability[capNum].getParametersNum()) {
		//check for overflow index
		return _capability[capNum].actuateCapability(_capability[capNum].getParametersName(paramNum), in);
	} else return false;

}

bool Semantic::actuateParameterOfCapability(uint8_t capNum, String paramName, int in)
{
	if (capNum < _capabilityNumber) {
		//check for overflow index
		return _capability[capNum].actuateCapability(paramName, in);
	} else return false;

}

bool Semantic::isASensor()
{
	if (_obsPropertyNumber > 0) return true;
	else return false;
}

bool Semantic::isAnActuator()
{
	bool retVal = false;
	for (uint8_t i = 0; i<_capabilityNumber; i++) {
		if (_capability[i].getParametersNum() > 0) retVal = true;
	}
	return retVal;
}

void Semantic::setURL(String url)
{
	_url = url;
}

String Semantic::getObsPropertyName(uint8_t propertyNumber)
{
	if (propertyNumber < _obsPropertyNumber) {
		return _obsProperty[propertyNumber].getName();
	} 
	else return "OutOfRange"; 
}

uint8_t Semantic::getObsPropertyNum()
{
	return _obsPropertyNumber;
}

String Semantic::getObsPropertyValue(uint8_t propertyNumber)
{
	return _obsProperty[propertyNumber].getValue();
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
	tmpString += "{\"name\":\"";
	tmpString += _name;
	tmpString += "\",\"parameters\":[";
	for (uint8_t i = 0; i < _paramNum-1; i++) {
			tmpString += _param[i].returnSemanticString();
			tmpString += ",";
		}
	tmpString += _param[_paramNum-1].returnSemanticString();
	tmpString += "],\"effects\":null}";
	return tmpString;
}

String Capability::getName()
{
	return _name;
}

String Capability::getParametersName(uint8_t paramNumber)
{
	if (paramNumber < _paramNum) return _param[paramNumber].getName();
	else return "OutOfRange";
}

uint8_t Capability::getParametersNum()
{
	return _paramNum;
}

bool Capability::actuateCapability(String paramName, int in)
{
	bool resp = false;
	for (uint8_t i = 0; i < _paramNum; i++) {
		if (getParametersName(i) == paramName) {
				resp = _param[i].actuateParameter(in);
		}
	}
	return resp;
}

Parameter::Parameter(String name, String datatype, String restrictionMin, String restrictionMax, bool (* actuate)(int))
{
	_name = name;
	_dataType = datatype;
	_restrictionMin = restrictionMin;
	_restrictionMax = restrictionMax;
	_actuate = actuate;
}

String Parameter::returnSemanticString()
{
	String tmpString = "";
	tmpString = "{\"name\":\"";
	tmpString += _name;
	tmpString += "\",\"datatype\":{\"@c\":\".PrimitiveDatatype\",\"baseDatatype\":\"";
	tmpString += _dataType;
	tmpString += "\",\"isArray\":";
	if (_isArray) tmpString += "true},";
	else tmpString += "false},";
	tmpString += "\"mandatory\":true,\"restrictions\":[{\"@c\":\".RangeRestriction\",\"min\":";
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

bool Parameter::actuateParameter(int in)
{
	return _actuate(in);
}

Property::Property(String name, String description, String (* function)())
{
	_name = name;
	_description = description;
	_function = function;
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

String Property::getValue()
{
	String tmpString = "";
	tmpString = _function();
	return tmpString;
}
