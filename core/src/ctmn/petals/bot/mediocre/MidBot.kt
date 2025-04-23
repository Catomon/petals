package ctmn.petals.bot.mediocre

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Logger
import ctmn.petals.bot.Bot
import ctmn.petals.bot.BotAction
import ctmn.petals.player.Player
import ctmn.petals.player.Species
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getBases
import ctmn.petals.playstage.getUnitsOfEnemyOf
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.logErr

class MidBot(
    player: Player, playScreen: PlayScreen,
    private val speciesUnits: Array<UnitActor> = Array<UnitActor>().apply {
        Species.getSpeciesUnits(player.species).forEach {
            add(it.unitActor)
        }
    },
) : Bot(player, playScreen) {

    //constants
    private val turnEndIdleThreshold = 2f
    private val afterActionIdle = 0.75f
    private val preActionIdle = 1f
    private val moveCamera = false

    //state variables
    private var waitTime = 0f
    private var idleTime = 0f
    private var processingTime = 0f
    private var noActions = false

    val enemyPlayers = mutableMapOf<Int, Player>()
    val enemyUnits = mutableMapOf<String, UnitActor>()
    val botUnits = mutableMapOf<String, UnitActor>()

    private val possibleActions = mutableListOf<BotAction>()

    //    @Volatile
    var isProcessing: Boolean = false
        private set
    private var processingThread: Thread? = null
    private fun createProcessingThread() = Thread {
        isProcessing = true
        try {
            process()
        } catch (e: Exception) {
            logErr("Cannot execute command.")
            e.printStackTrace()
        }
        isProcessing = false
    }

    private val log = Logger("MidBot", Logger.DEBUG)

    private var updateLogTimer = 0f

    override fun update(delta: Float) {
        updateLogTimer += delta
        if (updateLogTimer > 1) {
            updateLogTimer = 0f

            log.debug("update: waiting for action: ${playScreen.actionManager.hasActions}, waitTime: $waitTime, isProcessing: $isProcessing, processingTime: $processingTime, idleTime: $idleTime")
        }

        if (playScreen.actionManager.hasActions) {
            return
        }

        if (waitTime > 0) {
            waitTime -= delta
            return
        }

        if (isProcessing) {
            idleTime = 0f
            processingTime += delta
        } else {
            processingTime = 0f
            idleTime += delta

            if (!noActions) {
                val thinkingThread = processingThread
                val isThinkingThreadAlive = thinkingThread != null && thinkingThread.isAlive
                if (!isThinkingThreadAlive)
                    this.processingThread = createProcessingThread().apply { start() }
            }
        }

        if (idleTime > turnEndIdleThreshold) {
            log.debug("done.")

            processingThread?.interrupt()
            done()
        }
    }

    override fun levelCreated(playStage: PlayStage) {
        super.levelCreated(playStage)
    }

    override fun onStart() {
        log.debug("onStart()")

        super.onStart()

        isProcessing = false
        idleTime = 0f
        waitTime = 0f
        processingTime = 0f
        noActions = false

        enemyPlayers.putAll(playScreen.turnManager.players.filter { !player.allies.contains(it.id) }
            .associateBy { it.id })
        enemyUnits.putAll(playScreen.playStage.getUnitsOfEnemyOf(player).associateBy { it.name })
        botUnits.putAll(playScreen.playStage.getUnitsOfPlayer(player).associateBy { it.name })
    }

    override fun onEnd() {
        log.debug("onEnd()")

        super.onEnd()

        enemyPlayers.clear()
        enemyUnits.clear()
        botUnits.clear()
    }

    private fun process() {
        log.debug("process()")

        val atkA = botUnits.values.mapNotNull { unit ->
            val a = attackAction(unit)
            if (a.evaluate() > 0) a else null
        }
        possibleActions.addAll(atkA)

        val mvA = botUnits.values.mapNotNull { unit ->
            val a = moveAction(unit)
            if (a.evaluate() > 0) a else null
        }
        possibleActions.addAll(mvA)

        val buyA = playScreen.playStage.getBases(player).mapNotNull { base ->
            val a = buyAction(base)
            if (a.evaluate() > 0) a else null
        }
        possibleActions.addAll(buyA)

        val useAbA = botUnits.values.mapNotNull { unit ->
            val a = useAbilityAction(unit)
            if (a.evaluate() > 0) a else null
        }
        possibleActions.addAll(useAbA)

        log.debug("process(): possibleActions: " + possibleActions.joinToString { it::class.simpleName ?: "null" }
            .ifBlank { "Empty" })

        val empty = possibleActions.randomOrNull()?.let { action ->
            val success = action.execute()
            if (success)
                waitTime = afterActionIdle
            possibleActions.clear()
        } == null

        if (empty) {
            log.debug("process(): No actions.")
            noActions = true
        }
    }

    private fun attackAction(unitActor: UnitActor): AttackAction {
        return AttackAction(unitActor, this@MidBot, playScreen)
    }

    private fun moveAction(unitActor: UnitActor): MoveAction {
        return MoveAction(unitActor, this@MidBot, playScreen)
    }

    private fun buyAction(base: TileActor): BuyAction {
        return BuyAction(base, this@MidBot, playScreen, speciesUnits = speciesUnits)
    }

    private fun useAbilityAction(unitActor: UnitActor): UseAbilityAction {
        return UseAbilityAction(unitActor, this@MidBot, playScreen)
    }
}