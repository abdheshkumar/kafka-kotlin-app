import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.extensions.testcontainers.kafka.createStringStringConsumer
import io.kotest.extensions.testcontainers.kafka.createStringStringProducer
import io.kotest.matchers.collections.shouldHaveSize
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.time.Duration

class KafkaTestContainerExtensionTest : FunSpec() {

    private val confluentPlatformSchemaRegistryImage = "confluentinc/cp-schema-registry:7.2.2"
    private val confluentPlatformKafkaImage = "confluentinc/cp-kafka:7.2.2"
    private val schemaRegistryPort = 8085
    private val kafkaContainer: KafkaContainer =
        install(ContainerExtension(KafkaContainer(DockerImageName.parse(confluentPlatformKafkaImage)))) {
            withNetwork(Network.SHARED)
            waitingFor(HostPortWaitStrategy())
        }
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

    private fun schemaUrl(): String =
        "http://${schemaRegistryContainer.host}:${schemaRegistryContainer.getMappedPort(schemaRegistryPort)}"

    init {

        test("should setup kafka") {
            println(schemaUrl())
            val producer = kafkaContainer.createStringStringProducer()
            producer.send(ProducerRecord("foo", "key", "bubble bobble"))
            producer.flush()
            producer.close()

            val consumer = kafkaContainer.createStringStringConsumer {
                this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
                this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            }

            consumer.subscribe(listOf("foo"))
            val records = consumer.poll(Duration.ofSeconds(15))
            records.shouldHaveSize(1)
            consumer.close()
        }
    }
}
