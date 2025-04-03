package ctmn.petals.bot

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const
import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.playscreen.commands.UseAbilityCommand
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playstage.*
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.creatures.BigLivingTree
import ctmn.petals.unit.actors.creatures.BunnySlimeHuge
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.RoamingPosition
import ctmn.petals.utils.*
import ctmn.petals.utils.tiledX

class SimpleCommandProcessor(
    val aiPlayer: Player,
    val playScreen: PlayScreen,
) {

    private val playStage = playScreen.playStage

    /** followers will move in leaderRange + exceedLeaderRange **/
    private val exceedLeaderRange = 1

    private val playerID get() = aiPlayer.id

    /** if true always go for enemies on the map; look for enemies in [agroRange] if false. **/
    var permaAgro = true
    var agroRange = 6
    /** take random move if not enemies spotted **/
    var roamingIfNoAgro = true
    /** max dist from roaming start point **/
    var roamingMaxRange = 3

    private var lastUnitTimes = 0
    private var lastUnitCommand: UnitActor? = null

    fun makeCommand() : Command? {
        for (unit in playStage.getUnitsOfPlayer(aiPlayer).sortedBy { if (it.isLeader) it.leaderID else it.followerID }) {
            if (unit.actionPoints < 1) continue

            // skip unit if command has been returned but not executed 5 times
            if (lastUnitTimes > 5) {
                Gdx.app.error(javaClass.simpleName, "Can't do shit for unit ${unit.name}")

                unit.actionPoints = 0
                lastUnitTimes = 0

                continue
            }

            // agro
            if (!permaAgro) {
                if (!unit.isEnemyInAgroRange()) {
                    if (roamingIfNoAgro && unit.actionPoints >= Const.ACTION_POINTS_MOVE_MIN) {
                        val command = unit.roam(1)
                        if (command != null) { ///
                            lastUnitTimes++
                            if (lastUnitCommand != unit) {
                                lastUnitTimes = 0
                                lastUnitCommand = unit
                            }

                            return command
                        }
                    } else {
                        continue
                    }
                }
            }

            if (unit.isLeader) {
                val command = leader(unit, playStage.getUnitsForLeader(unit.leaderID, true))
                if (command != null) { ///
                    lastUnitTimes++
                    if (lastUnitCommand != unit) {
                        lastUnitTimes = 0
                        lastUnitCommand = unit
                    }

                    return command
                }
            }

            if (unit.isFollower) {
                val leader = playStage.getLeadUnit(unit.followerID)
                if (leader != null) {
                    val command = follower(unit, leader)
                    if (command != null) { ///
                        lastUnitTimes++
                        if (lastUnitCommand != unit) {
                            lastUnitTimes = 0
                            lastUnitCommand = unit
                        }

                        return command
                    }
                }
            }

            if (!unit.isLeader && playStage.getLeadUnit(unit.followerID) == null) {
                val command = single(unit)
                if (command != null) { ///
                    lastUnitTimes++
                    if (lastUnitCommand != unit) {
                        lastUnitTimes = 0
                        lastUnitCommand = unit
                    }

                    return command
                }
            }
        }

        return null
    }

    /** returns command for unit with not leader **/
    private fun single(unit: UnitActor) : Command? {
        if (unit.canAttackNow()) {
            val enemyUnit = playStage.getUnitsOfEnemyOf(aiPlayer).firstOrNull { unit.canAttackNow(it) }
            if ((enemyUnit != null)) {
                val command = AttackCommand(unit, enemyUnit)
                if (command.canExecute(playScreen)) return command
            }
        }

        if (unit.canMove()) {
            val closestOffenseTile = playStage.getOffenseFlanks(unit)
                .minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }
            if (closestOffenseTile != null) {
                val command = unit.moveTowardsCommand(closestOffenseTile.tiledX, closestOffenseTile.tiledY)
                if (command != null) return command
            }
        }

        return null
    }

    /** returns command for unit with a leader **/
    private fun follower(unit: UnitActor, leader: UnitActor) : Command? {
        if (unit.canMove()) {
            //if leader has endangered flanks
            // if unit is already on an endangered tile
//            if (leader.getEndangeredFlanks().firstOrNull { unit.tiledX == it.tiledX && unit.tiledY == it.tiledY } != null) {
//                unit.actionPoints = 1
//                return null
//            }
            // if not
            val leaderEndangerTile = leader.getEndangeredFlanks(true).minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }
            if (leaderEndangerTile != null) {
                val command = unit.moveTowardsCommand(leaderEndangerTile.tiledX, leaderEndangerTile.tiledY)
                if (command != null) return command
            }

            //no endangered flanks
            // if there are on offensive tile in leader range
            val offensiveTileInLeaderRange = playStage.getOffenseFlanks(unit)
                .filter { leader.isInRange(it.tiledX, it.tiledY, leader.cLeader?.leaderRange ?: 2) }
                .minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }
            if (offensiveTileInLeaderRange != null) {
                val command = unit.moveTowardsCommand(offensiveTileInLeaderRange.tiledX, offensiveTileInLeaderRange.tiledY, leader)
                if (command != null) {
                    if (tiledDst(unit.tiledX, unit.tiledY, command.tileX, command.tileY) <=
                        tiledDst(unit.tiledX, unit.tiledY, offensiveTileInLeaderRange.tiledX, offensiveTileInLeaderRange.tiledY))
                        return command
                }
            }


            // attack if can
            if (unit.canAttackNow()) {
                val enemyUnit = playStage.getUnitsOfEnemyOf(aiPlayer).firstOrNull { unit.canAttackNow(it) }
                if ((enemyUnit != null)) {
                    val command = AttackCommand(unit, enemyUnit)
                    if (command.canExecute(playScreen)) return command
                }
            }

            // move to offense flanks
            val closestOffenseTile = playStage.getOffenseFlanks(unit).minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }
            if (closestOffenseTile != null) {
                val command = unit.moveTowardsCommand(closestOffenseTile.tiledX, closestOffenseTile.tiledY, leader)
                if (command != null) {
                    if (tiledDst(unit.tiledX, unit.tiledY, command.tileX, command.tileY) <=
                        tiledDst(unit.tiledX, unit.tiledY, closestOffenseTile.tiledX, closestOffenseTile.tiledY))
                        return command
                }
            }

            unit.actionPoints = 1
        }

        //if unit can attack
        if (unit.canAttackNow()) {
            val enemyUnit = playStage.getUnitsOfEnemyOf(aiPlayer).firstOrNull { unit.canAttackNow(it) }
            if ((enemyUnit != null)) {
                val command = AttackCommand(unit, enemyUnit)
                if (command.canExecute(playScreen)) return command
            }
        }

        return null
    }

    /** returns command for leader **/
    private fun leader(unit: UnitActor, followers: Array<UnitActor>) : Command? {
        val useAbilityCommand = abilityUsage(unit)
        if (useAbilityCommand != null) return useAbilityCommand

        //attack if unit will have at least 15 health
        // and enemyDamage will not be higher than leaderDamage by more than 15
        // or if enemy dies
        if (unit.canAttackNow()) {
            val enemyUnit = playStage.getUnitsOfEnemyOf(aiPlayer).firstOrNull { unit.canAttackNow(it) }
            if (enemyUnit != null) {
                val command = AttackCommand(unit, enemyUnit)
                if (command.canExecute(playScreen)) {
                    val leaderDamage = playScreen.calculateDmgDef(unit, enemyUnit).first
                    val enemyDamage = playScreen.calculateDmgDef(enemyUnit, unit).first

                    val willNotDie = unit.health - enemyDamage > 0
                    val willDealSomeDamage = leaderDamage.toFloat() / enemyDamage >= 0.75f //not really outdamage tho
                    val willOutdamage = leaderDamage.toFloat() / enemyDamage >= 1f //not really outdamage tho
                    val willEndUpWithMoreHealth = unit.health - enemyDamage > enemyUnit.health - leaderDamage
                    val isNotLowOnHealth = unit.health - enemyDamage > 15
                    val hasThreeOrMoreFollowers = followers.size >= 3
                    val wllKillEnemyUnit = enemyUnit.health - leaderDamage <= 0

                    if (willNotDie && (wllKillEnemyUnit || ((!hasThreeOrMoreFollowers || enemyDamage < 9 || (enemyUnit.isLeader && willEndUpWithMoreHealth)) && isNotLowOnHealth && willDealSomeDamage))) {
                        return command
                    }
                }
            }
        }

        val closestEnemyUnit = playStage.getUnitsOfEnemyOf(aiPlayer).minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }
        if (closestEnemyUnit != null) {

            var xOff = if (closestEnemyUnit.tiledX < unit.tiledX) 2 else -2
            var yOff = if (closestEnemyUnit.tiledY < unit.tiledY) 2 else -2
            if (xOff == yOff) {
                xOff--
                yOff--
            }

            val halfHp = unit.health < unit.unitComponent.baseHealth / 2

            val command =
                if (halfHp)
                    unit.moveTowardsCommand(closestEnemyUnit.tiledX - xOff, closestEnemyUnit.tiledY - yOff, null, true)
                else
                    unit.moveTowardsCommand(closestEnemyUnit.tiledX, closestEnemyUnit.tiledY)

            if (command != null) {

                val tooCloseToEnemyUnit =
                    tiledDst(command.tileX, command.tileY, closestEnemyUnit.tiledX, closestEnemyUnit.tiledY) == 1
                val newPosNotTooClose = tiledDst(unit.tiledX, unit.tiledY, command.tileX, command.tileY) <=
                        tiledDst(unit.tiledX, unit.tiledY, closestEnemyUnit.tiledX, closestEnemyUnit.tiledY)

                if (newPosNotTooClose && !tooCloseToEnemyUnit)
                    return command

                if (!halfHp)
                    return command
            }
        }

        return null
    }

    /** returns move command in -[range] to [range] around unit
     * returns move command to move to leader if unit is a follower **/
    private fun UnitActor.roam(range: Int = 1) : MoveUnitCommand? {
        if (actionPoints < Const.ACTION_POINTS_MOVE_MIN) return null

        val roamPos = get(RoamingPosition::class.java) ?: add(RoamingPosition(tiledX, tiledY)) as RoamingPosition

        val leader = playStage.getLeadUnit(followerID)
        return if (isFollower && leader != null) {
            add(RoamingPosition(tiledX, tiledY))
            moveTowardsCommand(leader.tiledX, leader.tiledY)
        } else {
            //random tile in roaming range
            val tile = playStage.getTilesInRange(roamPos.tiledX, roamPos.tiledY, roamingMaxRange).random()
            if (tile.tiledX == tiledX && tile.tiledY == tiledY) {
                actionPoints = 0

                return null
            }

            moveTowardsCommand(tile.tiledX, tile.tiledY)
        }
    }

    /** returns true if an enemy is in [agroRange] **/
    private fun UnitActor.isEnemyInAgroRange() : Boolean {
        for (eUnit in playStage.getUnitsOfEnemyOf(aiPlayer)) {
            if (isInRange(eUnit.tiledX, eUnit.tiledY, agroRange)) {
                return true
            }
        }

        return false
    }

    // util methods

    /** returns tiles where [unit] will be able to attack an enemy unit in attackRange **/
    private fun PlayStage.getOffenseFlanks(unit: UnitActor) : Array<TileActor> {
        val offenseTiles = Array<TileActor>()
        for (eUnit in getUnitsOfEnemyOf(aiPlayer)) {
            getTilesInRange(eUnit.tiledX, eUnit.tiledY, unit.attackRange, true).forEach { tile ->
                offenseTiles.add(tile)
            }
        }

        return offenseTiles
    }

    /** returns [isPassableAndFree] tiles around unit in a range of 1 **/
    private fun UnitActor.getEndangeredFlanks(isPassableAndFree: Boolean = false) : Array<TileActor> {
        val freeFlanks = Array<TileActor>()

        playStageOrNull ?: return freeFlanks

        val freeTiles = playStage.getSurroundingTiles(tiledX, tiledY, isPassableAndFree)

//        for (unit in playStage.getUnitsOfEnemyOf(aiPlayer)) {
//            for (tile in freeTiles) {
//                //if (unit.canMove(tile.tiledX, tile.tiledY) || tiledX == tile.tiledX && tiledY == tile.tiledY)
//                    freeFlanks.add(tile)
//            }
//        }

        freeFlanks.addAll(freeTiles)

        return freeFlanks
    }

    /** returns move command to a tile that is reachable and closest to the destination
     * if [leader] != null, follower will move within its leaders leaderRange + [exceedLeaderRange] **/
    private fun UnitActor.moveTowardsCommand(destX: Int, destY: Int, leader: UnitActor? = null, minDist: Boolean = false) : MoveUnitCommand? {
        playStageOrNull ?: return null

        val closestTile =
            if (leader == null)
                getClosestTileInMoveRange(destX, destY)
            else {
                val closest = playStage.getClosestTileInRange(
                    leader.tiledX, leader.tiledY, destX, destY,
                    (leader.cLeader?.leaderRange ?: 1) + exceedLeaderRange,
                    increaseRange = true) ?: return null
                getClosestTileInMoveRange(closest.tiledX, closest.tiledY)
            }


        if (closestTile != null) return MoveUnitCommand(this, closestTile.tiledX, closestTile.tiledY)

        return null
    }

    /** todo **/
    private fun UnitActor.getKillableUnitFlank() : TileActor? {
        playStageOrNull ?: return null

        var closestFlank: TileActor? = null
        for (eUnit in playStage.getUnitsOfEnemyOf(aiPlayer)) {
            var eHealth = eUnit.health - playScreen.calculateDmgDef(this, eUnit).first
            if (eHealth > 7) continue

            closestFlank = playStage.getSurroundingTiles(eUnit.tiledX, eUnit.tiledY, true)
                .filter { eUnit.canMove(it) }
                .minByOrNull { tiledDst(tiledX, tiledY, it.tiledX, it.tiledY) + eHealth }
        }

        return closestFlank
    }

    /** returns suitable [UseAbilityCommand] for [unit] **/
    private fun abilityUsage(unit: UnitActor) : UseAbilityCommand? {
        val unitAbilities = unit.cAbilities
        if (unitAbilities != null && unitAbilities.abilities.isNotEmpty()) {
            val ability = unit.cAbilities!!.abilities.first()
            when (ability) {
                is BunnySlimeHuge.SlimeJumpAbility -> {
                    var freeTile: TileActor? = null
                    val enemyUnit: UnitActor? = playScreen.playStage.getUnits().apply {
                        removeAll {
                            it == unit
                                    || it.isAlly(aiPlayer)
                        }
                    }.firstOrNull {
                        freeTile = playScreen.playStage.getSurroundingTiles(it.tiledX, it.tiledY, true)
                            .firstOrNull { tile -> unit.isInRange(tile.tiledX, tile.tiledY, ability.range) }
                        freeTile != null
                    }

                    if (enemyUnit != null && freeTile != null) {
                        val command = UseAbilityCommand(
                            unit,
                            ability,
                            freeTile!!.tiledX,
                            freeTile!!.tiledY
                        )

                        if (command.canExecute(playScreen)) {
                            return command
                        }
                    }
                }
                is BigLivingTree.SummonOwlsAbility -> {
                    val command = UseAbilityCommand(
                        unit,
                        ability,
                        unit.tiledX,
                        unit.tiledY
                    )

                    if (command.canExecute(playScreen)) {
                        return command
                    }
                }
            }
        }

        return null
    }
}