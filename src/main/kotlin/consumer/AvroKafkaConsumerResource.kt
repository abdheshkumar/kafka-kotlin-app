package consumer

import Config
import arrow.core.Either
import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import com.user.User
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import loadApplicationConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class AvroKafkaConsumerResource(private val cache: InMemoryCache) {
    private val log: Logger = LoggerFactory.getLogger(AvroKafkaConsumerResource::class.java)

    fun kafkaProperties(config: Config): Properties {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = config.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "test"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        props[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = "1000"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            io.confluent.kafka.serializers.KafkaAvroDeserializer::class.java
        props[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = config.schemaRegistryUrl
        return props
    }

    fun createConsumer(props: Properties): Resource<KafkaConsumer<String, User>> =
        resource({ KafkaConsumer(props) }) { consumer, _ -> consumer.close() }

    fun readMessages(consumer: KafkaConsumer<String, User>) {
        while (true) {
            val records: ConsumerRecords<String, User> = consumer.poll(Duration.ofMillis(100))
            for (record in records) {
                log.info("offset = ${record.offset()}, key = ${record.key()}, value = ${record.value()}")
                cache.add(record.value().toString())
            }
        }
    }
}

suspend fun startApp(env: String): Resource<Unit> = resource {
    when (val config: Either<Throwable, Config> = loadApplicationConfig(env)) {
        is Either.Left -> throw config.value
        is Either.Right -> {
            val cache = InMemoryCache()
            val service = AvroKafkaConsumerResource(cache)
            val consumer =
                service.createConsumer(service.kafkaProperties(config.value)).bind()
            consumer.subscribe(listOf("my-topic-avro"))
            service.readMessages(consumer)
        }
    }
}

suspend fun main() = startApp("dev").use { }
