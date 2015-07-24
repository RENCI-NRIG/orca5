To generate a new self-signed certificate and a private key:

openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout file.key -out file.crt


