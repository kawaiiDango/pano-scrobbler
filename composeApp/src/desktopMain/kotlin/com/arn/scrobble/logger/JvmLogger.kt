package com.arn.scrobble.logger

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import com.arn.scrobble.utils.PlatformStuff
import java.io.File
import java.io.PrintStream
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

class JvmLogger(
    logToFile: Boolean,
    redirectStderr: Boolean,
    private val messageStringFormatter: MessageStringFormatter = DefaultFormatter
) : LogWriter() {
    private val originalErrStream = System.err

    private val logger = Logger.getLogger("default")!!.apply {
        try {
            useParentHandlers = false // Disable parent handlers to avoid duplicate logs

            if (logToFile) {
                // Create a log file handler with rolling files (100K per file and max of 3 files)
                val logsDir = File(PlatformStuff.filesDir, "logs").also { it.mkdirs() }

                val fileHandler = FileHandler(
                    logsDir.absolutePath + "/pano-scrobbler-%u-%g.log",
                    100 * 1024,
                    3,
                    true
                )

                // set this to a minimum of INFO level. The rest can be set in Kermit
                fileHandler.level = Level.INFO

                System.setProperty(
                    "java.util.logging.SimpleFormatter.format",
                    "[%1\$tc] %4\$s: %5\$s%6\$s%n"
                )

                fileHandler.formatter = SimpleFormatter()
                addHandler(fileHandler)
            }
            // Redirect stderr to logger output at SEVERE level
            if (redirectStderr) {
                val outputStream = LoggerOutputStream(this, Level.SEVERE)
                System.setErr(PrintStream(outputStream))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val str = messageStringFormatter.formatMessage(severity, Tag(tag), Message(message))
        val logLevel = when (severity) {
            Severity.Verbose -> Level.FINEST
            Severity.Debug -> Level.FINE
            Severity.Info -> Level.INFO
            Severity.Warn -> Level.WARNING
            Severity.Error,
            Severity.Assert -> Level.SEVERE
        }
        if (throwable != null)
            logger.log(logLevel, str, throwable)
        else
            logger.log(logLevel, str)

        // also print to standard outputs:
        if (severity == Severity.Error || severity == Severity.Assert) {
            originalErrStream.println(str)
            throwable?.printStackTrace(originalErrStream)
        } else {
            println(str)
            throwable?.printStackTrace(System.out)
        }
    }
}