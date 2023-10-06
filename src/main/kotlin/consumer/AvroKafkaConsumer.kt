package consumer

import com.user.User
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

object AvroKafkaConsumer {
    private val log: Logger = LoggerFactory.getLogger(AvroKafkaConsumer::class.java)

    fun kafkaProperties(): Properties {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:29092"
        props[ConsumerConfig.GROUP_ID_CONFIG] = "test"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        props[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = "1000"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
        props[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://localhost:8081"
        return props
    }

    fun createConsumer(props: Properties): KafkaConsumer<String, User> = KafkaConsumer(props)

    fun readMessages(consumer: KafkaConsumer<String, User>) {
        while (true) {
            val records: ConsumerRecords<String, User> = consumer.poll(Duration.ofMillis(100))
            for (record in records)
                log.info("offset = ${record.offset()}, key = ${record.key()}, value = ${record.value()}")
        }
    }
}

fun main() {
    val consumer = AvroKafkaConsumer.createConsumer(AvroKafkaConsumer.kafkaProperties())
    consumer.subscribe(listOf("my-topic-avro"))
    AvroKafkaConsumer.readMessages(consumer)
}
