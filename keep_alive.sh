curl -H 'Content-Type: application/json' -d '
{
  "sspId":"0"
}
' -X POST -D - http://localhost:8080/innkeeper/keep_alive
