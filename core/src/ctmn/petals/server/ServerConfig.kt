package ctmn.petals.server

import ctmn.petals.Config

/**
 * Runtime options for a dedicated server. Values can be overridden from the command line,
 * see [fromArgs].
 */
class ServerConfig {

    var serverName = "Fae's Feisty Server"

    var port = Config.SERVER_PORT

    /** Maximum accepted connections. */
    var maxPlayers = 100

    /** Maximum concurrent lobbies plus running matches. */
    var maxSessions = 32

    /** Per turn time limit in seconds. 0 means no limit (the default). */
    var turnTimeLimitSeconds = 0

    /** Advertise the server on the local network with JmDNS. */
    var lanDiscovery = true

    /** Maps the quick match queue may pick from. */
    var mapPool = mutableListOf("2L_Blooming_Mountains")

    /** Debug helper: run a headless bot vs bot match and exit when it ends. */
    var botMatch = false

    var botDifficulty = "normal"

    /** Seconds a disconnected player keeps their match seat while reconnecting. */
    var reconnectGraceSeconds = 60

    /** Debug helper: connect this many fake clients and let them play for load testing. */
    var loadTestPlayers = 0

    var loadTestSeconds = 20f

    /**
     * Debug helper: host a real match over the network with bot seats, so server side bots can
     * be verified without two game windows. Pairs the queue against a human plus bots.
     */
    var onlineBotMatch = false

    /** Bots to seat in [onlineBotMatch]. */
    var onlineBotMatchBots = 1

    /** Per message tracing, so a live match can be followed in the console. */
    var verboseLog = false

    companion object {
        fun fromArgs(args: Array<String>): ServerConfig {
            val config = ServerConfig()

            var i = 0
            while (i < args.size) {
                when (val a = args[i].lowercase()) {
                    "--port", "port" -> {
                        i++
                        if (i < args.size) config.port = args[i].toIntOrNull() ?: config.port
                    }

                    "--name", "name" -> {
                        i++
                        if (i < args.size) config.serverName = args[i]
                    }

                    "--max-players" -> {
                        i++
                        if (i < args.size) config.maxPlayers = args[i].toIntOrNull() ?: config.maxPlayers
                    }

                    "--max-sessions" -> {
                        i++
                        if (i < args.size) config.maxSessions = args[i].toIntOrNull() ?: config.maxSessions
                    }

                    "--turn-timer" -> {
                        i++
                        if (i < args.size) config.turnTimeLimitSeconds = args[i].toIntOrNull() ?: config.turnTimeLimitSeconds
                    }

                    "--maps" -> {
                        i++
                        if (i < args.size) config.mapPool = args[i].split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                    }

                    "--no-lan" -> config.lanDiscovery = false

                    "verboselog", "--verbose-log" -> config.verboseLog = true

                    "botmatchonline", "botmatch-online" -> config.onlineBotMatch = true

                    "botmatch" -> {
                        config.botMatch = true
                        config.mapPool = mutableListOf("2L_Blooming_Mountains")
                    }

                    "--reconnect" -> {
                        i++
                        if (i < args.size) config.reconnectGraceSeconds = args[i].toIntOrNull() ?: 60
                    }

                    "--loadtest" -> {
                        i++
                        if (i < args.size) config.loadTestPlayers = args[i].toIntOrNull() ?: 4
                    }

                    "--duration" -> {
                        i++
                        if (i < args.size) config.loadTestSeconds = args[i].toFloatOrNull() ?: 20f
                    }

                    "easy", "normal", "hard", "insane" -> config.botDifficulty = a

                    else -> {}
                }
                i++
            }

            return config
        }
    }
}
