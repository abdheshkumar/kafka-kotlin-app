package consumer

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

object StringKafkaConsumer {

    private fun kafkaProperties(): Properties {
        val props = Properties()
        props.setProperty("bootstrap.servers", "localhost:29092")
        props.setProperty("group.id", "test")
        props.setProperty("enable.auto.commit", "true")
        props.setProperty("auto.commit.interval.ms", "1000")
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        return props
    }

    private fun createConsumer(props: Properties): KafkaConsumer<String, String> = KafkaConsumer(props)

    private fun readMessages(consumer: KafkaConsumer<String, String>) {
        while (true) {
            val records: ConsumerRecords<String, String> = consumer.poll(Duration.ofMillis(100))
            for (record in records)
                println("offset = ${record.offset()}, key = ${record.key()}, value = ${record.value()}")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val consumer = createConsumer(kafkaProperties())
        consumer.subscribe(listOf("my-topic-string"))
        readMessages(consumer)
    }
}
