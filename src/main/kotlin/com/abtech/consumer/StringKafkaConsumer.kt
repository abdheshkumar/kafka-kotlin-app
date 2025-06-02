package com.abtech.consumer

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

object StringKafkaConsumer {

    private fun kafkaProperties(): Properties = Properties().apply {
        put("bootstrap.servers", "localhost:29092")
        put("group.id", "test")
        put("enable.auto.commit", "true")
        put("auto.commit.interval.ms", "1000")
        put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
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
