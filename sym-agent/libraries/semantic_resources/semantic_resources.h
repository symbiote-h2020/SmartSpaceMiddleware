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

#ifndef SYM_SEMANTICS_RESOURCES
#define SYM_SEMANTICS_RESOURCES

#include <Arduino.h>
#include <sym_agent.h>
#include <lsp.h>

/*

{
	"@c":".SmartDevice"
	"connectedTo":"*tba*",
    "available":"True",
    //"agentType":"SDEV",    // SDEV, Platform
    // SDEV one resource only
    "hasResource": 
        {
        	"@c":".Resource",
        	//MAPPED TO INTERNAL-ID
            "id":"###",
            "name":"###",
            "description":"###",
            // MAPPED TO TYPE
            "labels":"Light",
            "interworkingServiceURL":"*tba*",
            "locatedAt":"*tba*",
            "services":null
        },
	    "capabilities":
		[
			{
			    "name":"RGB",
			    "@c": ".Capability"
			    "inputParameter":
			    [
			        {
			            "name":"Red",
			            "@c":".Parameter"
			            "isArray":false,
			            "datatype":"xsd:unsignedByte",
			            "mandatory":true,
			            "restrictions":
			            [
			                {
			                    "min":0,
			                    "max":255
			                }
			            ]
			        },
			        {
			            "name":"Green",
			            "@c":".Parameter",
			            "isArray":false,
			            "datatype":"xsd:unsignedByte",
			            "mandatory":true,
			            "restrictions":
			            [
			                {
			                    "min":0,
			                    "max":255
			                }
			            ]
			        },
			        {
			            "name":"Blue",
			            "@c":".Parameter",
			            "isArray":false,
			            "datatype":"xsd:unsignedByte",
			            "mandatory":true,
			            "restrictions":
			            [
			                {
			                    "min":0,
			                    "max":255
			                }
			            ]
			        }
			    ],
			    "actsOn":"*tba*",
			    "affects":["R","G","B"]
			}
		]
	    "observesProperty":
	    	[
	    		{
	    			"@c":".Property"
	    			"name":"temperature",
	    			"description":""

	    		}
	    	]	
}

RAP RESPONSE
{
	"value":,
	"obsProperty":
	{
		"name":"temperature",
	    "description":""
	}
	"uom":
	{
		"symbol":"",
		"name":"",
		"description":
	}
}


*/

class Property {
  public:
    Property(String name, String description);
    String returnSemanticString();
    String getName();
  private:
    String _name;
    String _description;
};

class Parameter {
  public:
    Parameter(String name, String datatype, String _restrictionMin, String restrictionMax);
    String returnSemanticString();
    String getName();
    uint8_t getMinRestriction();
    uint8_t getMaxRestriction();
  private:
    
    String _name;
    bool _isArray = false;
    String _dataType;
    bool _mandatory = true;
    String _restrictionMin;
    String _restrictionMax;
};

class Capability {
  public:
    Capability(String name, uint8_t param_num, Parameter* parameter);
    String returnSemanticString();
    String getName();
    String getParametersName(uint8_t paramNumber);
  private:
    String _name;
    uint8_t _paramNum; 
    Parameter* _param;
};

class Semantic {
  public:
    Semantic( String internalId, String name, String url, uint8_t capNum, Capability* cap, uint8_t obsNumber, Property* property);
    String returnSemanticString();
    String getName();
    String getInternalId();
    String getURL();
    String getPropertyName(uint8_t propertyNumber);
  private:
  	String _name;
  	String _internalId;
  	String _url;
    uint8_t _capabilityNumber;
    Capability* _capability;
    uint8_t _obsPropertyNumber;
    Property* _obsProperty;
};




#endif // SYM_SEMANTICS_RESOURCES
