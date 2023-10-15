package producer

import Config
import arrow.core.Either
import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import com.user.User
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import loadApplicationConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class AvroKafkaProducerResource {

    private val log: Logger = LoggerFactory.getLogger(AvroKafkaProducerResource::class.java)

    fun kafkaProperties(config: Config): Properties = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            io.confluent.kafka.serializers.KafkaAvroSerializer::class.java,
        )
        put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, config.schemaRegistryUrl)
    }

    fun createProducer(props: Properties): Resource<KafkaProducer<String, User>> =
        resource({ KafkaProducer(props) }) { producer, _ -> producer.close() }

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

fun startApp(env: String, mapSource: Map<String, Any> = emptyMap()): Resource<Unit> = resource {
    when (val config: Either<Throwable, Config> = loadApplicationConfig(env, mapSource)) {
        is Either.Left -> throw config.value
        is Either.Right -> {
            val service = AvroKafkaProducerResource()
            val producer = service.createProducer(service.kafkaProperties(config.value)).bind()
            service.produceMessages(producer)
        }
    }
}

suspend fun main() = startApp("dev").use { }
