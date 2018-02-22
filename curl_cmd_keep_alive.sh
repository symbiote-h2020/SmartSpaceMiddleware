curl -H 'Content-Type: application/json' -d '
{
  "sessionId":"P09jQ3Xh",
  "payload":{
    "symId":"sym001"
  }
}
' -X POST -D - http://localhost:8080/innkeeper/keep_alive
