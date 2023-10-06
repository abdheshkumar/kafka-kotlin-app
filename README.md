https://github.com/conduktor/kafka-stack-docker-compose
#### Start Kafka containers
```shell
docker-compose up
```

nc command to verify that both the servers are listening to the respective ports:

```shell
nc -z localhost 2181
```

```shell
nc -z localhost 29092
```
```shell
nc -z localhost 8081
```
```shell
curl --silent http://localhost:8081/subjects/                      
["my-topic-avro-value","my-topic-value"]   
```
```shell
                                                                                                                                  
curl --silent http://localhost:8081/subjects/my-topic-avro-value/versions/1/schema
{"type":"record","name":"User","namespace":"com.user","fields":[{"name":"name","type":"string"},{"name":"age","type":"int"}]}
```

FOR RESOURCE SAFETY: https://arrow-kt.io/learn/quickstart/
FOR CONFIG: https://github.com/sksamuel/hoplite