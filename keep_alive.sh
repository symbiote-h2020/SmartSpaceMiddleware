curl -H 'Content-Type: application/json' -d '
{
  "sspIdParent":"0"
}
' -X POST -D - http://localhost:8080/innkeeper/keep_alive
