curl -H 'Content-Type: application/json' -d '
{
"sessionId":"J1GTn7v3",
"mti": "0x30",
"aad": "aa-bb-cc-dd-ee-ff;12792518056",
"authn":" IAkgIAkNCsKFCS4Jw6oJfwkuCS4JwrQJwpwJw6EJXgkuCcK1CTYJdAlrCcK6DQouCSMJKgkjCSYJNQkuCcKWCcOmCcKICWAJw7EJLgkuCUIJUA=="
}
' -X POST -D - http://localhost:8080/innkeeper/registry
