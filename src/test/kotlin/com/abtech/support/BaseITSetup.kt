package com.abtech.support

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import io.kotest.core.spec.style.StringSpec

abstract class BaseITSetup : StringSpec() {
    private val confluentPlatformSchemaRegistryImageVersion = "7.5.1"
    private val confluentPlatformKafkaImageVersion = "7.5.1"

    val testContainersResource: Resource<TestApp> = resource {
        val kafkaService = kafkaServiceResource(
            confluentPlatformKafkaImageVersion,
            confluentPlatformSchemaRegistryImageVersion
        ).bind()
        TestApp(kafkaService.serviceProperties.bootstrapServers, kafkaService.serviceProperties.schemaRegistryUrl)
    }
}

data class TestApp(val bootstrapServers: String, val schemaUrl: String)
