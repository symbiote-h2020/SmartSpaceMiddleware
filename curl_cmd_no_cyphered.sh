curl -H 'Content-Type: application/json' -d '
{
  "sessionId":"",
  "payload":{
    "symId":"sym001",
    "dk1":null,
    "hashField":null,
    "semanticDescription":[
      {
        "internalId":"intern001",
        "pluginId":"5001plgIDPippo",
        "cloudMonitoringHost":null,
        "singleTokenAccessPolicy":null,
        "singleTokenFilteringPolicy":null,
        "resource":{
          "@c":".Sensor",
          "observesProperty":null,
          "id":"intern001@sym001",
          "name":"SensorDevice001",
          "description":null,
          "interworkingServiceURL":null,
          "locatedAt":null,
          "services":[
            {
              "@c":".Service",
              "id":null,
              "name":null,
              "description":null,
              "interworkingServiceURL":null,
              "resultType":null,
              "parameters":[
                {
                  "name":"param1",
                  "mandatory":false,
                  "restrictions":null,
                  "datatype":null
                }
              ]
            }
          ]
        },
        "params":null
      },
      {
        "internalId":"intern002",
        "pluginId":"5002plgID",
        "cloudMonitoringHost":null,
        "singleTokenAccessPolicy":null,
        "singleTokenFilteringPolicy":null,
        "resource":{
          "@c":".Actuator",
          "id":"intern002@sym001",
          "name":"Actuator001",
          "description":null,
          "interworkingServiceURL":null,
          "locatedAt":null,
          "services":null,
          "capabilites":null
        },
        "params":null
      }
    ],
    "connectedTo":null,
    "available":false,
    "agentType":"SDEV"
  }
}
' -X POST -D - http://localhost:8080/innkeeper/registry
