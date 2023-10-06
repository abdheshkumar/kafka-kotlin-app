import arrow.fx.coroutines.continuations.resource
import io.kotest.core.extensions.install
import producer.startApp
import io.kotest.assertions.arrow.fx.coroutines.ResourceExtension
class AvroKafkaProducerResourceTest : BaseITSetup() {
    private val appStart = resource {
        val testApp = testContainersResource.bind()
        startApp(
            "dev",
            mapOf(
                "bootstrapServers" to testApp.bootstrapServers,
                "schemaRegistryUrl" to testApp.schemaUrl,
            ),
        )
        testApp
    }

    init {
        val app = install(ResourceExtension(appStart))
    }
}
