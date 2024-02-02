package ctmn.petals.ai

import com.badlogic.gdx.utils.Array
import ctmn.petals.player.Player
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnitsOfEnemyOf
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.playstage.tiledDst
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitActor
import kotlin.math.max

/** google "minimax" algorithm with "alpha-beta pruning" heuristic-based
1. Avoid unnecessary object creation: In the `getAvailableCommands()` function, you're creating a new `Array<Command>`
instance for each call. Instead, consider preallocating the `Array` with an initial capacity based on the expected
number of available commands. This avoids unnecessary memory allocations.

2. Minimize object access: In the `evaluateHeuristics()` function, you're repeatedly accessing the
`simulationPlayScreen.playStage` and `unit` properties. Consider caching these values locally to avoid repeated
lookups, which can improve performance.

3. Optimize distance calculation: In the `evaluateHeuristics()` function, you're calculating the distance between
units multiple times using the `tiledDst()` function. This calculation can be computationally expensive, especially
if the number of units is large. Consider caching the distances between units to avoid redundant calculations.
*/

class ScoreAI(
    val aiPlayer: Player,
    val playScreen: PlayScreen,
    ) {

    val simulationPlayScreen = PlayScreen()

    fun makeCommand(): Command? {
        // Assess the game state
        val availableCommands = getAvailableCommands()
        evaluateGameState() // Assess the current game state and update AI's internal knowledge

        // Determine AI's objectives and goals
        val objectives = defineObjectives()

        // Choose an command based on objectives and available options
        var selectedCommand: Command? = null
        var bestScore = -99999.0

        for (command in availableCommands) {
            if (command == null) continue

            val score = evaluateCommand(command, objectives)

            if (score > bestScore) {
                bestScore = score
                selectedCommand = command
            }
        }

        val trash = 0

        return selectedCommand
    }

    private fun evaluateGameState() {

    }

    private fun defineObjectives(): Array<Objective> {
        return Array()
    }

    private fun getAvailableCommands(): Array<Command> {
        val commands = Array<Command>(2000) //todo define max units per player and calculate commands amount

        for (unit in playScreen.playStage.getUnitsOfPlayer(aiPlayer)) {
            for (tile in playScreen.playStage.getTiles())
                if (unit.canMove(tile))
                    commands.add(MoveUnitCommand(unit, tile.tiledX, tile.tiledY))

            for (eUnit in playScreen.playStage.getUnitsOfEnemyOf(aiPlayer)) {
                if (unit.canAttack(eUnit))
                    commands.add(AttackCommand(unit, eUnit))
            }
        }

        for (i in 0 until commands.size) {
            if (!commands[i].canExecute(playScreen)) {
                println("removing cz can't ${commands[i]}")

                commands[i] = null
            }
        }

        println(commands.size.toString())

        return commands
    }

    private fun evaluateCommand(command: Command, objectives: Array<Objective>): Double {
        var score = 0.0

        // Apply the command to a simulated game state
        simulateCommand(command)

        // Evaluate the resulting game state based on objectives and heuristics
        score += evaluateObjectives(command, objectives)
        score += evaluateHeuristics()

        return score
    }

    private fun evaluateHeuristics(): Float {
        var score = 0f

        for (unit in simulationPlayScreen.playStage.getUnitsOfPlayer(aiPlayer)) {
            score += 1 + max(0, unit.health)  * 0.01f
            if (unit.isLeader)
                score += 1 + max(0, unit.health) * 0.01f

            var nearestEnemy: UnitActor? = null

            for (eUnit in simulationPlayScreen.playStage.getUnitsOfEnemyOf(aiPlayer)) {
                if (nearestEnemy == null) {
                    nearestEnemy = eUnit
                    continue
                }

                if (tiledDst(unit.tiledX, unit.tiledY, eUnit.tiledX, eUnit.tiledY) <
                    tiledDst(unit.tiledX, unit.tiledY, nearestEnemy.tiledX, nearestEnemy.tiledY)
                )
                    nearestEnemy = eUnit
            }

            if (nearestEnemy != null) {
                val dist = tiledDst(unit.tiledX, unit.tiledY, nearestEnemy.tiledX, nearestEnemy.tiledY)
                score -= if (dist > unit.attackRange) dist else 0
            }
        }

        for (unit in simulationPlayScreen.playStage.getUnitsOfEnemyOf(aiPlayer)) {
            score -= 1 + max(0, unit.health)  * 0.01f
            if (unit.isLeader)
                score -= 1 + max(0, unit.health) * 0.01f
        }

        return score
    }

    private fun simulateCommand(command: Command) {
        val gameState = GameState(playScreen)
        createPlayScreenFromGameState(gameState, simulationPlayScreen)
        simulationPlayScreen.commandManager.execute(command)
    }

    // Other supporting functions...

    private fun evaluateObjectives(command: Command, objectives: Array<Objective>): Double {
        var score = 0.0

        for (objective in objectives) {
            val objectiveScore = evaluateObjective(command, objective)
            val weightedScore = objectiveScore * objective.weight

            score += weightedScore
        }

        return score
    }

    private fun evaluateObjective(command: Command, objective: Objective): Double {
        var objectiveScore = 0.0

        // Assess how well the command aligns with the objective criteria
        // Use game-specific logic and criteria to determine the objective score

        return objectiveScore
    }

    // Other supporting functions...

    private fun createPlayScreenFromGameState(
        gameState: GameState,
        playScreen: PlayScreen = PlayScreen(),
    ): PlayScreen {

        playScreen.initView = false
        playScreen.playStage.initView = false

        playScreen.turnManager.players.clear()
        playScreen.aiManager.aiPlayers.clear()

        playScreen.playStage.clearGameActors()

        playScreen.gameStateId = gameState.gameStateId

        playScreen.localPlayer = gameState.localPlayer

        playScreen.turnManager.players.addAll(gameState.players)
        playScreen.turnManager.turn = gameState.turn
        playScreen.friendlyFire = gameState.friendlyFire
        gameState.aiPlayers.forEach { playScreen.aiManager.add(EasyAiDuelBot(playScreen.turnManager.getPlayerById(it) ?: return@forEach, playScreen)) }

        playScreen.playStage.idCounter = gameState.idCounter

        playScreen.randomSeed = gameState.randomSeed
        playScreen.randomCount = gameState.randomCount

        with (playScreen) {
            for (tile in gameState.tiles) {
                playStage.addActor(tile)
            }

            for (unit in gameState.units) {
                playStage.addActor(unit)
            }

            //playScreen.guiStage = PlayGUIStage(playScreen)
        }

        return playScreen
    }

    inner class AttackObjective() : Objective {
        override val name: String = "attack"

        override var weight: Float = 1f

        override fun evaluate(): Float {
            var score = 0



            return score * weight
        }
    }
}

//data class Command(/* Command properties */)

interface Objective {
    val name: String
    var weight: Float

    fun evaluate() : Float {
        return 1f
    }
}