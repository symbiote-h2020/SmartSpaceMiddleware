curl -H 'Content-Type: application/json' -d '
{
        "sessionId":'$1',
	"payload":"CYP PAYLOAD"
}' -X POST -D - http://localhost:8080/innkeeper/registry
