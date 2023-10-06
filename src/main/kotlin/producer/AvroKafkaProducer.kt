package producer

import com.user.User
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object AvroKafkaProducer {

    private val log: Logger = LoggerFactory.getLogger(AvroKafkaProducer::class.java)

    fun kafkaProperties(): Properties {
        val props = Properties()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:29092"
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        props[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://localhost:8081"
        return props
    }

    fun createProducer(props: Properties): KafkaProducer<String, User> = KafkaProducer(props)

    fun produceMessages(producer: KafkaProducer<String, User>) {
        for (i in 0..99) {
            val user = User.newBuilder().setAge(i).setName("Message-$i").build()
            val record = ProducerRecord("my-topic-avro", i.toString(), user)
            producer.send(record) { metadata, exception ->
                if (exception != null) {
                    log.error("Failed kafka writing message: $exception")
                } else {
                    log.info("Record metadata: $metadata")
                }
            }.get()
        }

        producer.close()
    }
}

fun main() {
    AvroKafkaProducer.produceMessages(
        AvroKafkaProducer.createProducer(
            AvroKafkaProducer.kafkaProperties(),
        ),
    )
}
