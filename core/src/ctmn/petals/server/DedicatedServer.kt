package ctmn.petals.server

import com.badlogic.gdx.Gdx
import ctmn.petals.bot.v2.BotFactory
import ctmn.petals.bot.v2.Difficulty
import ctmn.petals.map.loadMap
import ctmn.petals.net.FakeClient
import ctmn.petals.net.ServerCore
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.GameType
import ctmn.petals.sim.BattleSetup
import ctmn.petals.sim.ServerBattle
import ctmn.petals.utils.GameLog

/**
 * Headless render free server loop.
 *
 * In normal mode it starts the netty [ServerCore] which owns lobbies, the quick match queue
 * and running matches. `botmatch` runs a self contained bot vs bot match with no network at
 * all, and `--loadtest N` connects N fake clients against this same server for capacity
 * testing (dozens of matches, around 100 players).
 */
class DedicatedServer(val config: ServerConfig) {

    @Volatile
    private var running = false

    private var core: ServerCore? = null

    private var botMatch: ServerBattle? = null
    private var botElapsed = 0f

    private val fakeClients = ArrayList<FakeClient>()
    private var loadElapsed = 0f
    private var lastReport = 0f
    private var finishedMatches = 0

    fun start() {
        running = true
        GameLog.info("DedicatedServer", "Starting '${config.serverName}' on port ${config.port}")

        if (config.botMatch) {
            startBotMatch()
            return
        }

        val serverCore = ServerCore(config)
        serverCore.start()
        core = serverCore

        if (config.loadTestPlayers > 0) {
            repeat(config.loadTestPlayers) { index ->
                fakeClients.add(
                    FakeClient(index, "127.0.0.1", config.port) { finishedMatches++ }
                )
            }
            GameLog.info("DedicatedServer", "Load test: ${config.loadTestPlayers} fake clients connected")
        }
    }

    private fun startBotMatch() {
        val mapName = config.mapPool.firstOrNull() ?: "2L_Blooming_Mountains"
        val map = loadMap(mapName)
        val blue = newBluePlayer.copy()
        val red = newRedPlayer.copy()

        val difficulty = when (config.botDifficulty.lowercase()) {
            "easy" -> Difficulty.EASY
            "hard" -> Difficulty.HARD
            "insane" -> Difficulty.INSANE
            else -> Difficulty.NORMAL
        }

        val battle = BattleSetup.build(
            map = map,
            players = listOf(blue, red),
            gameType = GameType.MULTIPLAYER,
        )

        battle.botManager.add(BotFactory.create(blue, battle, difficulty))
        battle.botManager.add(BotFactory.create(red, battle, difficulty))

        botMatch = battle
        GameLog.info("DedicatedServer", "Headless bot match started on '$mapName'")
    }

    fun update(delta: Float) {
        if (!running) return

        if (config.botMatch) {
            updateBotMatch(delta)
            return
        }

        val serverCore = core ?: return
        if (config.loadTestPlayers > 0) {
            loadElapsed += delta

            if (loadElapsed - lastReport >= 5f) {
                lastReport = loadElapsed
                GameLog.info(
                    "DedicatedServer",
                    "Load test t=${loadElapsed.toInt()}s clients=${serverCore.connectedCount} " +
                        "matches=${serverCore.activeMatchCount} finished=$finishedMatches"
                )
            }

            if (loadElapsed >= config.loadTestSeconds) {
                GameLog.info(
                    "DedicatedServer",
                    "Load test done: clients=${serverCore.connectedCount} " +
                        "matches=${serverCore.activeMatchCount} finished=$finishedMatches"
                )
                running = false
                fakeClients.forEach { it.close() }
                serverCore.stop()

                // The harness keeps netty client groups, so end the process instead of
                // waiting for those threads to wind down.
                Gdx.app?.exit()
                Runtime.getRuntime().halt(0)
            }
        }
    }

    private fun updateBotMatch(delta: Float) {
        val battle = botMatch ?: return

        botElapsed += delta
        battle.update(delta)

        if (battle.isGameOver) {
            GameLog.info(
                "DedicatedServer",
                "Headless bot match finished in ${botElapsed.toInt()}s. Winners: ${battle.gameEndCondition.winners}"
            )
            running = false
            Gdx.app?.exit()
        }
    }

    fun stop() {
        running = false
        core?.stop()
        fakeClients.forEach { it.close() }
    }
}
