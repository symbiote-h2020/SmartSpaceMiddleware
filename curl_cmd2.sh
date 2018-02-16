curl -H 'Content-Type: application/json' -d '
{
        "symId":2,
        "dk1":"test",
	"connectedTo":"test",
	"available":true,
	"agentType":"test",
	"semanticDescription":[]
}' -X POST -D - http://localhost:8080/innkeeper/registry
