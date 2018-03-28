curl -H 'Content-Type: application/json' -d '
{
  "sspId":"2"
}
' -X POST -D - http://localhost:8080/innkeeper/unregistry
