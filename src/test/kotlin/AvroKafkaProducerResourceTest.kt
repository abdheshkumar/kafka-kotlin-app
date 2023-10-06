import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import com.user.User
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.kotest.assertions.arrow.fx.coroutines.ResourceExtension
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.extensions.install
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import producer.startApp
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.seconds

class AvroKafkaProducerResourceTest : BaseITSetup() {
    private val kafkaConsumerResource = resource {
        val testApp = testContainersResource.bind()
        startApp(
            "dev",
            mapOf(
                "bootstrapServers" to testApp.bootstrapServers,
                "schemaRegistryUrl" to testApp.schemaUrl,
            ),
        ).bind()
        kafkaConsumer(testApp).bind()
    }

    private val app = install(ResourceExtension(kafkaConsumerResource))

    init {
        "App should producer kafka records" {
            val consumer = app.get()
            eventually(10.seconds) {
                val records = consumer.poll(Duration.ofMillis(1000))
                records.count() shouldBe 100
            }
        }
    }
}

fun kafkaConsumer(app: TestApp): Resource<KafkaConsumer<String, User>> = resource({
    val props = Properties()
    props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = app.bootstrapServers
    props[ConsumerConfig.GROUP_ID_CONFIG] = "test"
    props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
    props[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = app.schemaUrl
    val consumer = KafkaConsumer<String, User>(props)
    consumer.subscribe(listOf("my-topic-avro"))
    consumer
}) { consumer, _ -> consumer.close() }
