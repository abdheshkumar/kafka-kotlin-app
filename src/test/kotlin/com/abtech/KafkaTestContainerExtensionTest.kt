package com.abtech

import io.kotest.assertions.arrow.fx.coroutines.ResourceExtension
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.extensions.install
import io.kotest.matchers.shouldBe
import com.abtech.producer.startApp
import com.abtech.support.BaseIT
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class KafkaTestContainerExtensionTest : BaseIT() {
    init {
        val app = install(ResourceExtension(startApp("dev")))
        "should setup kafka" {
            val consumer = kafkaConsumer()
            eventually(10.seconds) {
                val records = consumer.poll(Duration.ofMillis(1000))
                records.count() shouldBe 100
            }
        }
    }
}
