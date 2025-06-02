package com.abtech.support

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import org.slf4j.LoggerFactory
import java.lang.System.currentTimeMillis

private val logger = LoggerFactory.getLogger("com.abtech")

internal fun info(msg: String) = logger.info(msg)

/**
 * Provides a decorated version of [Resource] which prints it's three stages starting, allocated, ended.
 * @param label label to use in print statements.
 */
fun <A> Resource<A>.log(label: String): Resource<A> =
    resource {
        val time = currentTimeMillis()

        install(
            { info("$label.allocating") },
            { _, _ ->
                info("$label.release")
            }
        )

        bind().also {
            info("$label.allocated (${currentTimeMillis() - time} ms)")
        }
    }