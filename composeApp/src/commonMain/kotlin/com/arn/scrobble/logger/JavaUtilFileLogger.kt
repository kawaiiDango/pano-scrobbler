package com.arn.scrobble.logger

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import java.io.OutputStream
import java.io.PrintStream
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class JavaUtilFileLogger(
    var isEnabled: Boolean,
    redirectStderr: Boolean,
    private val printToStd: Boolean,
    private val messageStringFormatter: MessageStringFormatter = DefaultFormatter
) : LogWriter() {
    private val originalErrStream = System.err

    private val fileLogFormatter = object : Formatter() {
        override fun format(record: LogRecord): String {
            return "[" + PanoTimeFormatter.short(record.millis) + "] " +
                    messageStringFormatter.formatMessage(
                        null,
                        null,
                        Message(record.message)
                    ) +
                    "\n"
        }
    }

    private val logger by lazy {
        Logger.getLogger("default")!!.apply {
            try {
                useParentHandlers = false // Disable parent handlers to avoid duplicate logs

                // Create a log file handler with rolling files
                val logsDir = PlatformStuff.logsDir

                val fileHandler = FileHandler(
                    logsDir.absolutePath + "/pano-scrobbler-%u-%g.log",
                    100 * 1024,
                    N_FILES,
                    true
                )

                // set this to a minimum of INFO level. The rest can be set in Kermit
                fileHandler.level = Level.INFO
                fileHandler.formatter = fileLogFormatter
                addHandler(fileHandler)

                // Redirect stderr to logger output at SEVERE level
                if (redirectStderr) {
                    val outputStream = LoggerOutputStream(this, Level.SEVERE)
                    System.setErr(PrintStream(outputStream))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun isLoggable(tag: String, severity: Severity): Boolean {
        return isEnabled
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
        if (printToStd) {
            if (severity == Severity.Error || severity == Severity.Assert) {
                originalErrStream.println(str)
                throwable?.printStackTrace(originalErrStream)
            } else {
                println(str)
                throwable?.printStackTrace(System.out)
            }
        }
    }

    companion object {
        private const val N_FILES = 2

        fun mergeLogFilesTo(dest: OutputStream) {
            val logFiles = PlatformStuff.logsDir.listFiles { file ->
                file.isFile && file.name.endsWith(".log")
            }
                ?.sortedByDescending { it.lastModified() }
                ?.take(N_FILES)
                ?: return

            dest.bufferedWriter().use { output ->
                logFiles.forEach { file ->
                    repeat(5) {
                        output.newLine()
                    }

                    file.bufferedReader().copyTo(output)
                }
            }
        }
    }
}