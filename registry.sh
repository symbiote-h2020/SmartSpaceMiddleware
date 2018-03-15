curl -H 'Content-Type: application/json' -d '
{
  "symIdSDEV": "sym920",
  "pluginId": "5c:cf:7f:3a:6b:76",
  "pluginURL": "http://10.20.30.5:80",
  "dk1": "b15e3e8dab9f3badbce9a5444e58396",
  "hashField": "00000000000000000000"
}
' -X POST -D - http://localhost:8080/innkeeper/registry
