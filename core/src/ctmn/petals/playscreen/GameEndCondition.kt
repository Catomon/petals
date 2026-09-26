package ctmn.petals.playscreen

import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.sim.Battle
import ctmn.petals.unit.isAlly
import ctmn.petals.tile.isOccupied
import ctmn.petals.unit.playerId
import ctmn.petals.unit.teamId
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnits
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.tile.isBase
import ctmn.petals.tile.isOwnable

abstract class GameEndCondition(val id: String) {

    companion object {
        fun get(id: String) : GameEndCondition {
            return when (id) {
                "endless" -> NoEnd()
                "manual" -> Manual()
                "eliminate_enemy_units" -> EliminateEnemyUnits()
                "capture_bases" -> CaptureBases()
                "base_control_overtime" -> ControlBasesWOvertime()
                else -> throw IllegalArgumentException("GameEndCondition with id $id not found.")
            }
        }
    }

    enum class Result {
        NONE,
        WIN,
        LOSE,
        DRAW,
        HAS_WINNER,
    }

    var result: Result = Result.NONE

    var winners = mutableListOf<Int>()

    val ignorePlayers = mutableListOf<Int>()

    var end = false

    open fun check(battle: Battle): Boolean = end

    fun win(playerId: Int = 1) {
        winners.add(playerId)
        end = true
        result = Result.WIN
    }

    fun lose() {
        winners.add(-1)
        end = true
        result = Result.LOSE
    }

    fun draw() {
        winners.clear()
        end = true
        result = Result.DRAW
    }

    fun gotWinner(playerId: Int = 1) {
        winners.add(playerId)
        end = true
        result = Result.HAS_WINNER
    }

    open fun checkPlayerOutOfGame(player: Player, battle: Battle): Boolean = false
}

class NoEnd : GameEndCondition("endless") {
    override fun check(battle: Battle): Boolean {
        return false
    }
}

class Manual : GameEndCondition("manual")

class EliminateEnemyUnits : GameEndCondition("eliminate_enemy_units") {

    private var mTeamStandId = Team.NONE
    val teamStandId get() = mTeamStandId

    override fun check(battle: Battle): Boolean {
        val fUnit = battle.field.getUnits().firstOrNull()

        if (fUnit == null) {
            mTeamStandId = Team.NONE
            result = Result.DRAW
            winners.clear()
            return true
        }

        for (player in battle.turnManager.players) {
            if (!player.isOutOfGame) {
                for (player2 in battle.turnManager.players) {
                    if (player2 != player && !player2.isOutOfGame && !player.isAlly(player2.teamId)) {
                        return false
                    }
                }

                mTeamStandId = player.teamId
                result = Result.HAS_WINNER
                battle.turnManager.players.filter { it.teamId == mTeamStandId }.forEach {
                    winners.add(it.id)
                }
                return true
            }
        }


        mTeamStandId = Team.NONE
        result = Result.DRAW
        winners.clear()
        return true
    }

    override fun checkPlayerOutOfGame(player: Player, battle: Battle): Boolean {
        return battle.field.getUnitsOfPlayer(player).isEmpty
    }
}

class CaptureBases : GameEndCondition("capture_bases") {

    private var mTeamStandId = Team.NONE
    val teamStandId get() = mTeamStandId

    override fun check(battle: Battle): Boolean {
        val players = battle.turnManager.players.filter { player -> ignorePlayers.none { it == player.id } }
        for (player in players) {
            if (!player.isOutOfGame) {
                for (player2 in players) {
                    if (player2 != player && !player2.isOutOfGame && !player.isAlly(player2.teamId))
                        return false
                }

                mTeamStandId = player.teamId
                result = Result.HAS_WINNER
                players.filter { it.teamId == mTeamStandId }.forEach {
                    winners.add(it.id)
                }
                return true
            }
        }

        mTeamStandId = Team.NONE
        result = Result.DRAW
        winners.clear()
        return true
    }

    override fun checkPlayerOutOfGame(player: Player, battle: Battle): Boolean {
        if (ignorePlayers.any { it == player.id }) return false

        for (base in battle.field.getCapturablesOf(player).filter { it.isBase }) {
            //if (!base.isOccupied || battle.field.getUnit(base.tiledX, base.tiledY)!!.isAlly(player))
                return false
        }

        return true
    }
}

class ControlBasesWOvertime : GameEndCondition("base_control_overtime") {

    private var mTeamStandId = Team.NONE
    val teamStandId get() = mTeamStandId

    private var lastTurnSeen = -1
    private var lastRoundSeen = -1

    var overtimeTeamId = Team.NONE
        set(value) {
            field = value
            overtimeRoundsLeft = 3
        }
    var overtimeRoundsLeft = 3

    override fun check(battle: Battle): Boolean {
        val players = battle.turnManager.players.filter { player -> ignorePlayers.none { it == player.id } }

//        if (fUnit == null) {
//            mTeamStandId = Team.NONE
//            result = Result.DRAW
//            return true
//        }

        //overtime
        if (overtimeTeamId != Team.NONE) {
            if (overtimeRoundsLeft <= 0) {
                mTeamStandId = overtimeTeamId
                result = Result.HAS_WINNER
                players.filter { it.teamId == mTeamStandId }.forEach {
                    winners.add(it.id)
                }
                return true
            }

            //count a round down only once per turn, without any scene2d listener
            val turnChanged = battle.turnManager.turn != lastTurnSeen || battle.turnManager.round != lastRoundSeen
            if (turnChanged) {
                lastTurnSeen = battle.turnManager.turn
                lastRoundSeen = battle.turnManager.round

                val overtimePlayer = players.firstOrNull { it.teamId == overtimeTeamId && !it.isOutOfGame }
                if (overtimePlayer != null && checkOvertime(overtimePlayer, battle)) {
                    overtimeRoundsLeft--

                    if (overtimeRoundsLeft <= 0) {
                        mTeamStandId = overtimeTeamId
                        result = Result.HAS_WINNER
                        players.filter { it.teamId == mTeamStandId }.forEach {
                            winners.add(it.id)
                        }
                        return true
                    }

                    battle.showFloatingLabel("$overtimeTeamId Overtime: $overtimeRoundsLeft")
                } else {
                    battle.showFloatingLabel("$overtimeTeamId Overtime canceled")

                    overtimeTeamId = Team.NONE
                }
            }
        }

        // if player has more than 75% bases
        for (player in players) {
            if (!player.isOutOfGame) {
                if (checkOvertime(player, battle)) {
                    if (overtimeTeamId != player.teamId) {
                        overtimeTeamId = player.teamId
                        lastTurnSeen = battle.turnManager.turn
                        lastRoundSeen = battle.turnManager.round

                        battle.showFloatingLabel("$overtimeTeamId Overtime triggered")
                    }
                } else {
                    if (overtimeTeamId == player.teamId) {
                        battle.showFloatingLabel("$overtimeTeamId Overtime canceled")

                        overtimeTeamId = Team.NONE
                    }
                }
            }
        }

        // return true if one player left
        for (player in players) {
            if (!player.isOutOfGame) {
                for (player2 in players) {
                    if (player2 != player && !player2.isOutOfGame && !player.isAlly(player2.teamId))
                        return false
                }

                mTeamStandId = player.teamId
                result = Result.HAS_WINNER
                players.filter { it.teamId == mTeamStandId }.forEach {
                    winners.add(it.id)
                }
                return true
            }
        }

        mTeamStandId = Team.NONE
        result = Result.NONE
        winners.clear()
        return true
    }

    private fun checkOvertime(player: Player, battle: Battle): Boolean {
        //todo get bases of team
        return battle.field.getCapturablesOf(player).size >= battle.field.getTiles()
            .filter { it.isOwnable }.size * 0.70f
    }

    override fun checkPlayerOutOfGame(player: Player, battle: Battle): Boolean {
        if (ignorePlayers.any { it == player.id }) return false

        if (!battle.field.getUnitsOfPlayer(player).isEmpty)
            return false

        for (base in battle.field.getCapturablesOf(player)) {
            if (!base.isOccupied || battle.field.getUnit(base.tiledX, base.tiledY)!!.isAlly(player))
                return false
        }

        return true
    }
}
