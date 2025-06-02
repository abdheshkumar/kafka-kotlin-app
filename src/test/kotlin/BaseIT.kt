import com.user.User
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

abstract class BaseIT : StringSpec() {
    private val confluentPlatformKafkaImage = "confluentinc/cp-kafka:7.5.1"
    private val confluentPlatformSchemaRegistryImage = "confluentinc/cp-schema-registry:7.5.1"
    private val schemaRegistryPort = 8085

    private val container = KafkaContainer(DockerImageName.parse(confluentPlatformKafkaImage))
        .withNetwork(Network.SHARED)
        .waitingFor(HostPortWaitStrategy())

    private val kafkaContainer: KafkaContainer = install(ContainerExtension(container))

    private val schemaRegistryContainer =
        install(ContainerExtension(createSchemaRegistryContainer(kafkaContainer)))

    private fun createSchemaRegistryContainer(
        kafkaContainer: KafkaContainer,
    ): GenericContainer<*> {
        return GenericContainer(DockerImageName.parse(confluentPlatformSchemaRegistryImage))
            .withNetwork(kafkaContainer.network)
            .withEnv(
                mapOf(
                    "SCHEMA_REGISTRY_HOST_NAME" to "schema-registry",
                    "SCHEMA_REGISTRY_LISTENERS" to "http://0.0.0.0:$schemaRegistryPort",
                    "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS" to "PLAINTEXT://${kafkaContainer.networkAliases[0]}:9092",
                ),
            )
            .waitingFor(HostPortWaitStrategy())
            .withExposedPorts(schemaRegistryPort)
    }

    fun schemaUrl(): String =
        "http://${schemaRegistryContainer.host}:${schemaRegistryContainer.getMappedPort(schemaRegistryPort)}"

    fun kafkaConsumer(): KafkaConsumer<String, User> {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaContainer.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "test"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
        props[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = schemaUrl()
        val consumer = KafkaConsumer<String, User>(props)
        consumer.subscribe(listOf("my-topic-avro"))
        return consumer
    }

    init {
        System.setProperty("config.override.bootstrapServers", kafkaContainer.bootstrapServers)
        System.setProperty("config.override.schemaRegistryUrl", schemaUrl())
    }
}
