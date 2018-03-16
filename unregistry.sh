curl -H 'Content-Type: application/json' -d '
{
  "symId": "sym920"
}
' -X POST -D - http://localhost:8080/innkeeper/unregistry
