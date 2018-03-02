curl -H 'Content-Type: application/json' -d '
{
"mti": "0x10",
"SDEVmac":"aa-bb-cc-dd-ee-ff",
"cp": "0x00a8, 0xccab, 0x008c,0xc004, 0xc005",
"kdf": "PBKDF2",
"nonce": "70501824",
"x509":
"MIIDXTCCAkWgAwIBAgIJAJC1HiIAZAiIMA0GCSqGSIb3DfBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVxaWRnaXRzIFB0eSBMdGQwHhcNMTExMjMxMDg1OTQ0WhcNMTAJjyzfN746vaInA1KxYEeI1Rx5KXY8zIdj6a7hhphpj2E04C3Fayua4DRHyZOLmlvQ6tIChY0ClXXuefbmVSDeUHwc8YuB7xxt8BVc69rLeHV15A0qyx77CLSj3tCx2IUXVqRs5mlSbvA=="  
}
' -X POST -D - http://localhost:8080/innkeeper/registry
