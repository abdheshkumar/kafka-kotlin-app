import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.addMapSource
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.fp.Validated
import java.io.File

inline fun <reified A : Any> loadApplicationConfig(
    environment: String,
    mapSource: Map<String, Any> = emptyMap(),
): Either<Throwable, A> {
    return ConfigLoaderBuilder.default()
        .addMapSource(mapSource)
        .addFileSource(File("../vault/secrets/secrets.yaml"), optional = true)
        .addResourceSource("/env/$environment.yml", allowEmpty = true)
        .addResourceSource("/env/default.yml", allowEmpty = true)
        .build()
        .loadConfig<A>().attempt()
}

fun <A : Any> ConfigResult<A>.attempt(): Either<Throwable, A> = when (this) {
    is Validated.Valid -> this.value.right()
    is Validated.Invalid -> {
        println("Failed to load" + this.error.description())
        Throwable(this.error.description())
            .left()
    }
}
