import json
import requests
import time;
data = {
"mti":"0x10",
"SDEVmac":"5c:cf:7f:ee:02:25",
"cp":"0x008C",
"kdf":"PBKDF2",
"nonce":"6a4470a0"
}

data_json = json.dumps(data)
headers = {'Content-type': 'application/json'}
url="http://localhost:8080/innkeeper/registry"

response = requests.post(url, data=data_json, headers=headers)
print response
data = response.json()
print data["mti"]


if data["mti"]=="0x20":
	print "GOT 0x20, send 0x30 authn message"
	time.sleep(1)

	data_0x30={ 
		"mti":"0x30",
		"nonce":"abcdef01",
		"sessionId":data["sessionId"],
		"sn":"0d4032",
		"authn":"9cbqYW8kl8tGiyNl9Dc2czxNYucSU62OrvLSt+L7fAk=",
		"sign":"f5c6ea616f2497cb468b2365f43736733c4d62e71253ad8eaef2d2b7e2fb7c09" 
	}

	print data_0x30
	data_json=json.dumps(data_0x30)
	response = requests.post(url, data=data_json, headers=headers)
	print response
	data = response.json()
	print data["mti"]
	
	

	
