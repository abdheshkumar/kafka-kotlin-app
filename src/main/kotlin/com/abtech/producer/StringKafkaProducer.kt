package com.abtech.producer

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

object StringKafkaProducer {

    private fun kafkaProperties(): Properties {
        val props = Properties()
        props["bootstrap.servers"] = "localhost:29092"
        props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        return props
    }

    private fun createProducer(props: Properties): KafkaProducer<String, String> = KafkaProducer(props)

    private fun produceMessages(producer: KafkaProducer<String, String>) {
        for (i in 0..99) {
            val record = ProducerRecord("my-topic-string", i.toString(), "Message-$i")
            producer.send(record) { metadata, exception ->
                if (exception != null) {
                    println("Failed kafka writing message: $exception")
                } else {
                    println("Record metadata: $metadata")
                }
            }.get()
        }

        producer.close()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        produceMessages(
            createProducer(
                kafkaProperties(),
            ),
        )
    }
}
