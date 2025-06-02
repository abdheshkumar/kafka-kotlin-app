package com.abtech.support

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.autoCloseable
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

/**
 * Creates a [Resource] containing a running instance of a schema registry.
 * @param network Network used to connect to kafka instance.
 * @param kafkaBootstrapServer URL for kafka
 * @param version schema registry version
 */
fun schemaRegistryResource(
    network: Network,
    networkBootstrapServer: String,
    version: String
): Resource<SchemaRegistryContainer> =
    autoCloseable {
        schemaRegistryContainer(network, networkBootstrapServer, version)
            .apply {
                start()
            }
    }.log("schema registry")

fun schemaRegistryContainer(
    network: Network,
    networkBootstrapServer: String,
    version: String
): SchemaRegistryContainer =
    SchemaRegistryContainer(version).withKafka(network, networkBootstrapServer)

private const val EXPOSED_PORT = 8085


class SchemaRegistryContainer(version: String?) : GenericContainer<SchemaRegistryContainer>("confluentinc/cp-schema-registry:$version") {
    fun withKafka(
        network: Network,
        networkBootstrapServer: String
    ): SchemaRegistryContainer =
        withNetwork(network)
            .withExposedPorts(EXPOSED_PORT)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:$EXPOSED_PORT")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", networkBootstrapServer)
            .waitingFor(HostPortWaitStrategy())

    @Suppress("HttpUrlsUsage")
    fun externalSchemaRegistryUrl(): String = "http://$host:${getMappedPort(EXPOSED_PORT)}"
}