package com.abtech.support

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.kafka.ConfluentKafkaContainer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * A [Resource] containing a running instance of kafka and the avro schema registry.
 */
fun kafkaServiceResource(
    kafkaVersion: String,
    registryVersion: String
): Resource<KafkaService> =
    resource {
        val network = autoCloseable { Network.newNetwork() }
        val (kafkaContainer, kafkaContainerProperties) = kafkaContainerResource(network, kafkaVersion).bind()
        val schemaRegistryContainer =
            schemaRegistryResource(network, kafkaContainerProperties.address, registryVersion).bind()

        val kafkaServiceProperties =
            KafkaServiceProperties(
                kafkaContainerProperties.bootstrapServers,
                schemaRegistryContainer.externalSchemaRegistryUrl()
            )
        install({
            KafkaService(kafkaContainer, kafkaServiceProperties)
        }, { _, _ ->
            deleteTempKafkaFiles()
        })
    }

fun kafkaContainer(
    network: Network,
    version: String
): ConfluentKafkaContainer {
    return ConfluentKafkaContainer("confluentinc/cp-kafka:$version")
        .withListener("kafka:19092")
        .withNetwork(network)
        .waitingFor(HostPortWaitStrategy())
        // add support for Apple Silicon/ARM
        .withCreateContainerCmdModifier { it.withPlatform("linux/amd64") }
}

private fun kafkaContainerResource(
    network: Network,
    version: String
) = resource {
    val kafkaContainer =
        autoCloseable {
            kafkaContainer(network, version)
                .apply {
                    start()
                }
        }

    Pair(
        kafkaContainer,
        KafkaContainerProperties("kafka:19092", kafkaContainer.bootstrapServers)
    )
}.log("kafka")

/**
 * Description of running kafka service.
 * @param container docker container
 * @param serviceProperties kafka service properties
 */
data class KafkaService(
    val container: ConfluentKafkaContainer,
    val serviceProperties: KafkaServiceProperties
)

/**
 * Description of running kafka service.
 * @param address address of the kafka service
 * @param bootstrapServers kafka bootstrap URL
 */
data class KafkaContainerProperties(
    val address: String,
    val bootstrapServers: String
)

/**
 * Description of running kafka service.
 * @param bootstrapServers kafka bootstrap URL
 * @param schemaRegistryUrl schema registry URL
 */
data class KafkaServiceProperties(
    val bootstrapServers: String,
    val schemaRegistryUrl: String
)

private suspend fun deleteTempKafkaFiles() {
    Paths
        .get(System.getProperty("java.io.tmpdir"))
        .resolve("kafka-streams")
        .takeIf { it.exists() }
        ?.let { streamsTmp ->
            withContext(Dispatchers.IO) {
                Files
                    .walk(streamsTmp)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete)
            }
        }
}