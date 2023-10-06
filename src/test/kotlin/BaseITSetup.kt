import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import io.kotest.core.spec.style.StringSpec
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

abstract class BaseITSetup : StringSpec() {
    private val confluentPlatformSchemaRegistryImage = "confluentinc/cp-schema-registry:7.2.2"
    private val confluentPlatformKafkaImage = "confluentinc/cp-kafka:7.2.2"
    private val schemaRegistryPort = 8085

    private val kafkaContainerResource: Resource<KafkaContainer> =
        resource({
            val container = KafkaContainer(DockerImageName.parse(confluentPlatformKafkaImage))
                .withNetwork(Network.SHARED)
                .waitingFor(HostPortWaitStrategy())
            container.start()
            container
        }) { container, _ ->
            container.close()
        }

    private fun schemaRegistryContainerResource(kafkaContainer: KafkaContainer) = resource({
        val container = createSchemaRegistryContainer(kafkaContainer)
        container.start()
        container
    }) { container, _ -> container.close() }

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

    private fun schemaUrl(schemaRegistryContainer: GenericContainer<*>): String =
        "http://${schemaRegistryContainer.host}:${schemaRegistryContainer.getMappedPort(schemaRegistryPort)}"

    val testContainersResource: Resource<TestApp> = resource {
        val kafkaContainer = kafkaContainerResource.bind()
        val schemaRegistryContainer = schemaRegistryContainerResource(kafkaContainer).bind()
        TestApp(kafkaContainer.bootstrapServers, schemaUrl(schemaRegistryContainer))
    }
}

data class TestApp(val bootstrapServers: String, val schemaUrl: String)
