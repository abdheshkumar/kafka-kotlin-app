package com.abtech.producer

import com.abtech.Config
import arrow.core.Either
import com.abtech.loadApplicationConfig
import com.user.User
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

class ReactiveKafka {
    private val log: Logger = LoggerFactory.getLogger(javaClass::class.java)
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS z dd MMM yyyy")
    private fun kafkaProperties(config: Config): SenderOptions<String, User> {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, config.schemaRegistryUrl)
        }
        return SenderOptions.create(props)
    }

    private fun createProducer(senderOptions: SenderOptions<String, User>): KafkaSender<String, User> =
        KafkaSender.create(senderOptions)

    private fun produceMessages(kafkaSender: KafkaSender<String, User>): Disposable {
        val data = Flux.range(0, 99).map {
            val user = User.newBuilder().setAge(it).setName("Message-$it").build()
            val record = ProducerRecord("my-topic-avro", it.toString(), user)
            SenderRecord.create(record, it)
        }
        return kafkaSender.send(data)
            .doOnError { e -> log.error("Send failed", e) }
            .subscribe { r ->
                val metadata = r.recordMetadata()
                val timestamp = Instant.ofEpochMilli(metadata.timestamp())
                val message = String.format(
                    "Message %d sent successfully, topic-partition=%s-%d offset=%d timestamp=%s\n",
                    r.correlationMetadata(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    dateFormat.format(timestamp),
                )
                println(message)
            }
    }

    fun startApp(env: String, mapSource: Map<String, Any> = emptyMap()): Disposable {
        return when (val config: Either<Throwable, Config> = loadApplicationConfig(env, mapSource)) {
            is Either.Left -> throw config.value
            is Either.Right -> {
                val producer = createProducer(kafkaProperties(config.value))
                produceMessages(producer)
            }
        }
    }
}

fun main() {
    ReactiveKafka().startApp("dev")
}
