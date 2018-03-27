curl -H 'Content-Type: application/json' -d '
{
"symId":"sym607","sspId":"0",
"internalIdResource":"InternalIDSensor001",
  "semanticDescription":{
         "@c":".Actuator",
          "id":"",
          "name":"Actuator001",
          "description":null,
          "interworkingServiceURL":null, 
          "locatedAt":null,
          "services":null,
          "capabilites":null    
      }
}
' -X POST -D - http://localhost:8080/innkeeper/join
