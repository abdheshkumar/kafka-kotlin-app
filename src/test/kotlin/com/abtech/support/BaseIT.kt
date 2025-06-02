package com.abtech.support

import com.user.User
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.testcontainers.containers.Network
import org.testcontainers.kafka.ConfluentKafkaContainer
import java.util.*

abstract class BaseIT : StringSpec() {
    private val confluentPlatformKafkaImageVersion = "7.5.1"
    private val confluentPlatformSchemaRegistryImageVersion = "7.5.1"
    val network: Network = Network.newNetwork()


    private val kafkaContainer: ConfluentKafkaContainer =
        install(ContainerExtension(kafkaContainer(network, confluentPlatformKafkaImageVersion))) {}

    private val schemaRegistryContainer: SchemaRegistryContainer =
        install(
            ContainerExtension(
                schemaRegistryContainer(
                    network,
                    "kafka:19092",
                    confluentPlatformSchemaRegistryImageVersion
                )
            )
        )

    fun kafkaConsumer(): KafkaConsumer<String, User> {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaContainer.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "test"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
        props[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] =
            schemaRegistryContainer.externalSchemaRegistryUrl()
        val consumer = KafkaConsumer<String, User>(props)
        consumer.subscribe(listOf("my-topic-avro"))
        return consumer
    }

    init {
        System.setProperty("config.override.bootstrapServers", kafkaContainer.bootstrapServers)
        System.setProperty("config.override.schemaRegistryUrl", schemaRegistryContainer.externalSchemaRegistryUrl())
    }
}
