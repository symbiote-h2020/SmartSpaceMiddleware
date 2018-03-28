curl -H 'Content-Type: application/json' -d '
{
  "symId":"sym161",
  "pluginId": "5c:cf:7f:3a:6b:32",
  "pluginURL": "http://10.20.30.5:21",
  "dk1": "b15e3e8dab9f3badbce9a5444e58398",
  "hashField": "00000000000000000000"
}
' -X POST -D - http://localhost:8080/innkeeper/registry
