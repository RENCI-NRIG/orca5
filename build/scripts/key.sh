#JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home
echo |openssl s_client -connect geni-orca.renci.org:443 2>&1 |sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' | sudo $JAVA_HOME/bin/keytool -import -trustcacerts -alias geni-orca -keystore  $JAVA_HOME/jre/lib/security/cacerts -storepass changeit -noprompt
