package ru.yuksanbo.common.launcher

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import com.typesafe.config.ConfigSyntax
import com.typesafe.config.ConfigUtil
import com.typesafe.config.ConfigValueFactory
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.yuksanbo.common.misc.Misc
import java.io.File
import java.io.PrintWriter
import ch.qos.logback.classic.Logger as LogbackLogger

class Launcher {

    companion object {
        private val OPT_debug = "d"
        private val OPT_help = "h"
        private val OPT_config = "c"

        private val builtinOptions = listOf(
                CliOption(OPT_debug, "debug", false, "Enable debug"),
                CliOption(OPT_help, "help", false, "Print this help")
        )
    }

    private var cliArgs: List<String> = emptyList()
    private var customCliOptions: List<CliOption> = emptyList()
    private var params: List<String> = emptyList()
    private var customLoggers: Map<String, String> = emptyMap()
    private var noConfig = false

    fun launch(appName: String): Context {

        val cliParsed = parseCommandLine(appName)

        val con = Console(cliParsed.hasOption(OPT_debug))

        con.debug("A. Loading configuration")
        val config = loadConfiguration(con, cliParsed, appName)

        con.debug("B. Configuring logging")
        configureLogging(config.getConfig("logging"), con.debug, appName)

        con.debug("Z. Launcher finished")

        return Context(
                appName,
                cliParsed,
                config.getBoolean("launcher.debug"),
                config,
                con,
                LoggerFactory.getLogger(appName)
        )
    }

    fun noConfig(): Launcher {
        this.noConfig = true
        return this;
    }

    fun withLoggers(vararg customLoggers: Pair<String, String>): Launcher {
        this.customLoggers = mapOf(*customLoggers)
        return this
    }

    fun withArgs(args: Array<String>): Launcher {
        this.cliArgs = args.toList()
        return this
    }

    fun withCliOptions(vararg options: CliOption): Launcher {
        this.customCliOptions = options.toList()
        return this
    }

    fun withParams(vararg params: String): Launcher {
        this.params = params.toList()
        return this
    }

    private fun parseCommandLine(appName: String): CommandLine {
        val cliOptions = Options()
        (builtinOptions + customCliOptions).forEach {
            cliOptions.addOption(Option(it.shortName, it.longName, it.hasArg, it.description))
        }
        val cliParsed = try {
            DefaultParser().parse(cliOptions, cliArgs.toTypedArray())
        } catch (e: ParseException) {
            throw Misc.systemExit("command-line parsing failed: ${e.message}")
        }

        if (cliParsed.hasOption(OPT_help)) {
            val formatter = HelpFormatter()
            val writer = PrintWriter(System.out)
            formatter.printHelp(
                    writer,
                    80,
                    "\n" + params.map { "${appName} [options] ${it}" }.joinToString("\n"),
                    null,
                    cliOptions,
                    2,
                    4,
                    null
            )
            writer.flush()
            throw Misc.systemExit(code = 0)
        }

        return cliParsed
    }

    private fun loadConfiguration(con: Console, cliParsed: CommandLine, appName: String): Config {
        val parseOptions = ConfigParseOptions
                .defaults()
                .setAllowMissing(false)
                .setSyntax(ConfigSyntax.CONF)

        val resolveOptions = ConfigResolveOptions
                .defaults()
                .setAllowUnresolved(false)

        val placeholders = ConfigFactory.empty().withValue("appName", ConfigValueFactory.fromAnyRef(appName))

        val configFile = File(cliParsed.getOptionValue(OPT_config, "${appName}.conf"))

        con.debug("Loading internal default launcher configuration")
        val defaultConfig = ConfigFactory
                .parseResources("yvu/libs/launcher/launcher.conf", parseOptions)
                .resolveWith(placeholders, resolveOptions)

        val userConfig = if (noConfig) {
            con.debug("Skipping user configuration due to noConfig option")
            ConfigFactory.empty()
        } else {
            con.debug("Loading user configuration from ${configFile.absolutePath}")
            ConfigFactory
                    .parseFile(configFile, parseOptions)
                    .resolveWith(placeholders, resolveOptions)
        }

        con.debug("Merging command-line/builder parameters into final configuration")

        var config = ConfigFactory.empty()

        if (!con.debug && userConfig.withFallback(defaultConfig).getBoolean("launcher.debug")) {
            con.debug = true
        }
        if (con.debug) {
            config = config.withValue("launcher.debug", ConfigValueFactory.fromAnyRef(true))
            config = config.withValue("logging.console.per-package.${appName}", ConfigValueFactory.fromAnyRef("debug"))
        }

        for ((pkg, level) in customLoggers) {
            val level0 = ConfigValueFactory.fromAnyRef(level)
            config = config.withValue(ConfigUtil.joinPath("logging.file.per-package", pkg), level0)
            config = config.withValue(ConfigUtil.joinPath("logging.console.per-package", pkg), level0)
        }

        config = config.withFallback(userConfig.withFallback(defaultConfig))

        return config
    }

    private fun configureLogging(config: Config, consoleDebug: Boolean, appName: String) {
        val logback = LoggerFactory.getILoggerFactory() as LoggerContext

        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as LogbackLogger
        rootLogger.detachAndStopAllAppenders()
        rootLogger.level = Level.toLevel(config.getString("default-level"))

        fun addAppenderToLogger(appender: Appender<ILoggingEvent>, logger: String, level: String?) {
            val logger0 = LoggerFactory.getLogger(logger) as LogbackLogger
            logger0.addAppender(appender)
            level?.let {
                logger0.level = Level.toLevel(level)
                logger0.isAdditive = false
            }
        }

        fun setPerPackageLoggers(appender: Appender<ILoggingEvent>, perPackageConfig: Config) {
            rootLogger.addAppender(appender)
            for ((pkg, level) in perPackageConfig.root()) {
                addAppenderToLogger(appender, pkg, level.unwrapped() as String)
            }
        }

        fun createConsoleAppender(config: Config): Appender<ILoggingEvent>? {
            if (!config.getBoolean("enabled")) {
                return null
            }

            return ConsoleAppender<ILoggingEvent>().apply {
                context = logback
                encoder = PatternLayoutEncoder().apply {
                    context = logback
                    pattern = "%.-1level %msg%n"
                    start()
                }
                target = "System.err"
                isWithJansi = true
                start()
            }
        }

        fun createFileAppender(config: Config): Appender<ILoggingEvent>? {
            if (!config.getBoolean("enabled")) {
                return null
            }

            return FileAppender<ILoggingEvent>().apply {
                context = logback
                encoder = PatternLayoutEncoder().apply {
                    context = logback
                    pattern = "%date %level [%thread] %logger \\(%file:%line\\) %msg%n"
                    start()
                }
                file = config.getString("filepath")
                start()
            }
        }

        val consoleConfig = config.getConfig("console")
        val fileConfig = config.getConfig("file")

        createConsoleAppender(consoleConfig)?.let {
            setPerPackageLoggers(it, consoleConfig.getConfig("per-package"))
        }

        createFileAppender(fileConfig)?.let {
            setPerPackageLoggers(it, fileConfig.getConfig("per-package"))
        }

    }

    data class Context(
            val appName: String,
            val parsedCli: CommandLine,
            val debug: Boolean,
            val config: Config,
            val console: Console,
            val logger: Logger
    )

    class Console(var debug: Boolean = false) {
        inline fun debug(messageSupplier: () -> String) {
            debug(messageSupplier.invoke())
        }
        fun debug(vararg messageParts: String) {
            if (!debug) return
            System.err.print("D ")
            for (message in messageParts) {
                System.err.print(message)
            }
            System.err.print("\n")
        }

        fun out(message: String) {
            System.out.println(message)
            System.out.flush()
        }

        fun error(message: String) {
            System.err.println(message)
        }
    }
}