# 1. 创建CA根证书
openssl genrsa -out ca-key.pem 4096
openssl req -new -x509 -days 3650 -key ca-key.pem -out ca-cert.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=YourCompany/CN=Internal CA"

# 2. 生成服务端证书(auth-service)
openssl genrsa -out auth-key.pem 2048
openssl req -new -key auth-key.pem -out auth-csr.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=YourCompany/CN=auth-service"

# 签名服务端证书
openssl x509 -req -days 365 -in auth-csr.pem \
  -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out auth-cert.pem

# 3. 生成客户端证书(gateway-service)
openssl genrsa -out gateway-key.pem 2048
openssl req -new -key gateway-key.pem -out gateway-csr.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=YourCompany/CN=gateway-service"

openssl x509 -req -days 365 -in gateway-csr.pem \
  -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out gateway-cert.pem

# 4. 转换为PKCS12格式(Java使用)
openssl pkcs12 -export -in auth-cert.pem -inkey auth-key.pem \
  -out auth-keystore.p12 -name auth-service -passout pass:changeit

openssl pkcs12 -export -in gateway-cert.pem -inkey gateway-key.pem \
  -out gateway-keystore.p12 -name gateway-service -passout pass:changeit

# 5. 创建TrustStore
keytool -import -trustcacerts -alias ca -file ca-cert.pem \
  -keystore truststore.p12 -storepass changeit -noprompt

echo "证书生成完成!"
echo "auth-keystore.p12: 服务端证书"
echo "gateway-keystore.p12: 客户端证书"
echo "truststore.p12: 信任库"