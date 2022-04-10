package com.github.sszuev.ontconverter

import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("Main.kt")

fun main(argsArray: Array<String>) {
    val args = parseArgs(argsArray)
    // configure logger:
    org.apache.log4j.Logger.getRootLogger().level = if (args.verbose)
        org.apache.log4j.Level.DEBUG else org.apache.log4j.Level.FATAL
    logger.debug(args.printString())
}

