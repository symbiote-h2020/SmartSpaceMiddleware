curl -H 'Content-Type: application/json' -d '
{
  "sessionId":"P4xkLbq3",
  "payload":{
    "symId":"sym001"
  }
}
' -X POST -D - http://localhost:8080/innkeeper/unregistry
