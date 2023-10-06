import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

abstract class BaseIT : StringSpec() {
    private val confluentPlatformSchemaRegistryImage = "confluentinc/cp-schema-registry:7.2.2"
    private val confluentPlatformKafkaImage = "confluentinc/cp-kafka:7.2.2"
    private val schemaRegistryPort = 8085
    val kafkaContainer: KafkaContainer =
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

    fun schemaUrl(): String =
        "http://${schemaRegistryContainer.host}:${schemaRegistryContainer.getMappedPort(schemaRegistryPort)}"

    init {
        System.setProperty("egsp.discovery-url", "http://localhost:8084/")
        System.setProperty("trips-config.deadlineMs", "1000")
    }
}
