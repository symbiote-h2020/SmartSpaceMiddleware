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

/*

{
	"@c":".SmartDevice"
	"connectedTo":"*tba*",
    "available":"True",
    "agentType":"SDEV",    // SDEV, Platform
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


#endif // SYM_SEMANTICS_RESOURCES
