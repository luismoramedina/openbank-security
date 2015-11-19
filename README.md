# openbank-security


## Latch
Es necesario hacer el pareo de la aplicación de móvil con la aplicación cliente primero.
Hacer primero unpair y luego pair:

http://localhost/unpair

http://localhost/pair?token=xxxxx



NOTA:
Para que funcione la integración con Latch es necesario importar la CA que está en "\resources\latch\ca_latch.pem" en el almacén de certificados de la jdk que 
estemos usando, ej. "\jre\lib\security\cacerts"
