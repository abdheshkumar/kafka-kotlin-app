https://github.com/conduktor/kafka-stack-docker-compose

docker-compose up

nc command to verify that both the servers are listening on the respective ports:

nc -z localhost 2181

nc -z localhost 29092

nc -z localhost 8081

curl --silent http://localhost:8081/subjects/                      
["my-topic-avro-value","my-topic-value"]                                                                                                                                     
curl --silent http://localhost:8081/subjects/my-topic-avro-value/versions/1/schema
{"type":"record","name":"User","namespace":"com.user","fields":[{"name":"name","type":"string"},{"name":"age","type":"int"}]}