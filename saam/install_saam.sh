#! /bin/bash
#1 REGISTER an SSP on portal: https://symbiote-dev.man.poznan.pl/administration
echo "step 1:"
echo "---------"
if [ -f "cert.properties" ]; then
	echo "cert.properties exists, generate saam-keystore.p12"
else
	echo "ERROR: cert.properties not exists, check your cert.properties first"
       	echo "REGISTR an SSP on portal: https://symbiote-dev.man.poznan.pl/administration"
	exit
fi
echo "step 2:"
echo "---------"
#2 copy this java security in /usr/lib/jvm/java-8-oracle/jre/lib/security/
JAVA_SECURITY="/usr/lib/jvm/java-8-oracle/jre/lib/security/java.security"
regex="org.bouncycastle.jce.provider.BouncyCastleProvider"
if grep -q "$regex" "${JAVA_SECURITY}"; then
	echo "entry $regex found in ${JAVA_SECURITY}, not copy"
else
	echo "entry $regex not found in ${JAVA_SECURITY}, copy"
	#cp java.security ${JAVA_SECURITY}
	if [ ! -f "${JAVA_SECURITY}.bak" ]; then
		   echo "File ${JAVA_SECURITY}.bak does not exist. save a backup copy"
		   #cp "${JAVA_SECURITY}" "${JAVA_SECURITY}.bak"
	fi

fi


echo "step 3:"
echo "---------"
#3 CHECK IF bcprov-ext-jdk15on-159.jar exists
if [ -f "/usr/lib/jvm/java-8-oracle/jre/lib/ext/bcprov-ext-jdk15on-159.jar" ]; then
	echo "bcprov-ext-jdk15on-159.jar exists, skip download"
else
	echo "bcprov-ext-jdk15on-159.jar not exists, download and copy"
	wget https://www.bouncycastle.org/download/bcprov-ext-jdk15on-159.jar
	cp bcprov-ext-jdk15on-159.jar /usr/lib/jvm/java-8-oracle/jre/lib/ext/
	
fi

echo "step 4:"
echo "---------"
#4 download SAM SymbioteSecuirty Helper
if [ -f "SymbIoTeSecurity-25.4.0-helper.jar" ]; then
	echo "SymbIoTeSecurity-25.4.0-helper.jar exists, skip download"
else
	echo "SymbIoTeSecurity-25.4.0-helper.jar not exists, download and copy"
	wget https://jitpack.io/com/github/symbiote-h2020/SymbIoTeSecurity/25.4.0/SymbIoTeSecurity-25.4.0-helper.jar 

fi

echo "step 5:"
echo "---------"
if [ -f "cert.properties" ]; then
	echo "cert.properties exists, generate saam-keystore.p12"
	java -jar SymbIoTeSecurity-25.4.0-helper.jar cert.properties
else
	echo "ERROR: cert.properties not exists, check your cert.properties first"
	exit
fi
	

echo "step 6:"
echo "---------"
#4 download SAM SymbioteSecuirty Helper
if [ -f "AuthenticationAuthorizationManager-3.1.1-run.jar" ]; then
	echo "AuthenticationAuthorizationManager-3.1.1-run.jar exists, skip download"
else
	echo "AuthenticationAuthorizationManager-3.1.1-run.jar not exists, download and copy"
	curl --location https://box.psnc.pl/f/8ae4708c36/?dl=1 2> /dev/null > AuthenticationAuthorizationManager-3.1.1-run.jar
fi



echo "DONE, SUCCESS!"
