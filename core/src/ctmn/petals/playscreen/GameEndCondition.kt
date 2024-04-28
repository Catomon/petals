package ctmn.petals.playscreen

import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.gui.floatingLabel
import ctmn.petals.playscreen.listeners.TurnListener
import ctmn.petals.unit.isAlly
import ctmn.petals.tile.isOccupied
import ctmn.petals.unit.playerId
import ctmn.petals.unit.teamId
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnits
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.tile.isCapturable

abstract class GameEndCondition(val id: String) {

    companion object {
        fun get(id: String) : GameEndCondition {
            return when (id) {
                "endless" -> NoEnd()
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

    abstract fun check(playScreen: PlayScreen): Boolean

    open fun checkPlayerOutOfGame(player: Player, playScreen: PlayScreen): Boolean = false
}

class NoEnd : GameEndCondition("endless") {

    override fun check(playScreen: PlayScreen): Boolean {
        return false
    }
}

class EliminateEnemyUnits : GameEndCondition("eliminate_enemy_units") {

    private var mTeamStandId = Team.NONE
    val teamStandId get() = mTeamStandId

    override fun check(playScreen: PlayScreen): Boolean {
        val fUnit = playScreen.playStage.getUnits().firstOrNull()

        if (fUnit == null) {
            mTeamStandId = Team.NONE
            result = Result.DRAW
            winners.clear()
            return true
        }

        for (player in playScreen.turnManager.players) {
            if (!player.isOutOfGame) {
                for (player2 in playScreen.turnManager.players) {
                    if (player2 != player && !player2.isOutOfGame && !player.isAlly(player2.teamId)) {
                        return false
                    }
                }

                mTeamStandId = player.teamId
                result = Result.HAS_WINNER
                playScreen.turnManager.players.filter { it.teamId == mTeamStandId }.forEach {
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

    override fun checkPlayerOutOfGame(player: Player, playScreen: PlayScreen): Boolean {
        return playScreen.playStage.getUnitsOfPlayer(player).isEmpty
    }
}

class CaptureBases : GameEndCondition("capture_bases") {

    private var mTeamStandId = Team.NONE
    val teamStandId get() = mTeamStandId

    override fun check(playScreen: PlayScreen): Boolean {
        for (player in playScreen.turnManager.players) {
            if (!player.isOutOfGame) {
                for (player2 in playScreen.turnManager.players) {
                    if (player2 != player && !player2.isOutOfGame && !player.isAlly(player2.teamId))
                        return false
                }

                mTeamStandId = player.teamId
                result = Result.HAS_WINNER
                playScreen.turnManager.players.filter { it.teamId == mTeamStandId }.forEach {
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

    override fun checkPlayerOutOfGame(player: Player, playScreen: PlayScreen): Boolean {
        for (base in playScreen.playStage.getCapturablesOf(player)) {
            if (!base.isOccupied || playScreen.playStage.getUnit(base.tiledX, base.tiledY)!!.isAlly(player))
                return false
        }

        return true
    }
}

class ControlBasesWOvertime : GameEndCondition("base_control_overtime") {

    private var mTeamStandId = Team.NONE
    val teamStandId get() = mTeamStandId

    var overtimeTeamId = Team.NONE
        set(value) {
            field = value
            overtimeRoundsLeft = 3
        }
    var overtimeRoundsLeft = 3

    override fun check(playScreen: PlayScreen): Boolean {
        val fUnit = playScreen.playStage.getUnits().firstOrNull()

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
                playScreen.turnManager.players.filter { it.teamId == mTeamStandId }.forEach {
                    winners.add(it.id)
                }
                return true
            }
        }

        // if player has more than 75% bases
        for (player in playScreen.turnManager.players) {
            if (!player.isOutOfGame) {
                if (checkOvertime(player, playScreen)) {
                    if (overtimeTeamId != player.teamId) {
                        overtimeTeamId = player.teamId

                        //overtime turn listener
                        playScreen.playStage.addListener(TurnListener(player.id) { _, listener ->
                            if (overtimeTeamId == player.teamId && checkOvertime(player, playScreen)) {
                                overtimeRoundsLeft--

                                if (overtimeRoundsLeft <= 0) {
                                    playScreen.playStage.removeListener(listener)
                                }

                                playScreen.guiStage.floatingLabel("$overtimeTeamId Overtime: $overtimeRoundsLeft")
                            } else {
                                playScreen.playStage.removeListener(listener)

                                playScreen.guiStage.floatingLabel("$overtimeTeamId Overtime canceled")
                            }
                        })

                        playScreen.guiStage.floatingLabel("$overtimeTeamId Overtime triggered")
                    }
                } else {
                    if (overtimeTeamId == player.teamId) {
                        playScreen.guiStage.floatingLabel("$overtimeTeamId Overtime canceled")

                        overtimeTeamId = Team.NONE
                    }
                }
            }
        }

        // return true if one player left
        for (player in playScreen.turnManager.players) {
            if (!player.isOutOfGame) {
                for (player2 in playScreen.turnManager.players) {
                    if (player2 != player && !player2.isOutOfGame && !player.isAlly(player2.teamId))
                        return false
                }

                mTeamStandId = player.teamId
                result = Result.HAS_WINNER
                playScreen.turnManager.players.filter { it.teamId == mTeamStandId }.forEach {
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

    private fun checkOvertime(player: Player, playScreen: PlayScreen): Boolean {
        //todo get bases of team
        return playScreen.playStage.getCapturablesOf(player).size >= playScreen.playStage.getTiles()
            .filter { it.isCapturable }.size * 0.70f
    }

    override fun checkPlayerOutOfGame(player: Player, playScreen: PlayScreen): Boolean {
        if (!playScreen.playStage.getUnitsOfPlayer(player).isEmpty)
            return false

        for (base in playScreen.playStage.getCapturablesOf(player)) {
            if (!base.isOccupied || playScreen.playStage.getUnit(base.tiledX, base.tiledY)!!.isAlly(player))
                return false
        }

        return true
    }
}