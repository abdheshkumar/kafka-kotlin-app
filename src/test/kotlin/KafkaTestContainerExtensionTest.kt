
import io.kotest.extensions.testcontainers.kafka.createStringStringConsumer
import io.kotest.extensions.testcontainers.kafka.createStringStringProducer
import io.kotest.matchers.collections.shouldHaveSize
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration

class KafkaTestContainerExtensionTest : BaseIT() {
    init {
        "should setup kafka" {
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
