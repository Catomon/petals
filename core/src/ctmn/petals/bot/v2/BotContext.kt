package ctmn.petals.bot.v2

import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.income
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getBases
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnitsOfEnemyOf
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.isBase
import ctmn.petals.tile.isBuilding
import ctmn.petals.tile.isCapturable
import ctmn.petals.tile.isCrystal
import ctmn.petals.tile.isOccupied
import ctmn.petals.tile.isWaterBase
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.followerID
import ctmn.petals.unit.isFollower
import ctmn.petals.unit.isLeader
import ctmn.petals.unit.isWorker
import ctmn.petals.unit.leaderID
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/**
 * A refreshed snapshot of the game state the bot cares about. Rebuilt at the start of every
 * action tick so decisions are never based on stale positions.
 */
class BotContext(val bot: AiBot) {

    val playScreen: PlayScreen get() = bot.playScreen
    val playStage: PlayStage get() = playScreen.playStage
    val player: Player get() = bot.player
    val playerId: Int get() = player.id

    var units: List<UnitActor> = emptyList()
        private set
    var enemies: List<UnitActor> = emptyList()
        private set
    var ownBases: List<TileActor> = emptyList()
        private set
    var enemyBases: List<TileActor> = emptyList()
        private set
    var ownBuildings: List<TileActor> = emptyList()
        private set
    var enemyBuildings: List<TileActor> = emptyList()
        private set
    var capturables: List<TileActor> = emptyList()
        private set

    val workers: List<UnitActor> get() = units.filter { it.isWorker }
    val leaders: List<UnitActor> get() = units.filter { it.isLeader }
    val combatUnits: List<UnitActor> get() = units.filter { !it.isWorker }

    var income: Int = 0
        private set

    fun refresh() {
        units = playStage.getUnitsOfPlayer(player).toList()
        enemies = playStage.getUnitsOfEnemyOf(player).toList()
        ownBases = playStage.getBases(player).toList()
        enemyBases = playStage.getTiles().filter {
            it.isBase && it.cPlayerId?.playerId != null && it.cPlayerId?.playerId != playerId
        }
        ownBuildings = playStage.getTiles().filter {
            it.isBuilding && it.cPlayerId?.playerId == playerId
        }
        enemyBuildings = playStage.getTiles().filter {
            it.isBuilding && it.cPlayerId?.playerId != null && it.cPlayerId?.playerId != playerId
        }
        // Only crystal tiles are capturable. Bases can only be destroyed.
        capturables = playStage.getTiles().filter {
            it.isCrystal && it.cPlayerId?.playerId != playerId
        }
        income = player.income(playScreen)
    }

    fun nearestEnemy(from: UnitActor): UnitActor? =
        enemies.minByOrNull { tiledDst(from.tiledX, from.tiledY, it.tiledX, it.tiledY) }

    fun nearestCapturable(from: UnitActor): TileActor? =
        capturables.minByOrNull { tiledDst(from.tiledX, from.tiledY, it.tiledX, it.tiledY) }

    fun nearestEnemyBase(from: UnitActor): TileActor? =
        enemyBases.minByOrNull { tiledDst(from.tiledX, from.tiledY, it.tiledX, it.tiledY) }

    fun nearestEnemyBuilding(from: UnitActor): TileActor? =
        (enemyBases + enemyBuildings).minByOrNull { tiledDst(from.tiledX, from.tiledY, it.tiledX, it.tiledY) }

    /** Followers that are assigned to [leader] and are still alive. */
    fun followersOf(leader: UnitActor): List<UnitActor> =
        units.filter { it.isFollower && it.followerID == leader.leaderID }

    /** Leader a freshly bought follower should be attached to, or -1. */
    fun leaderForNewUnit(): Int {
        val leader = leaders.minByOrNull { followersOf(it).size } ?: return -1
        return leader.leaderID
    }

    fun freeOwnedBases(): List<TileActor> = ownBases.filter { !it.isOccupied }

    fun ownsBuilding(buildingName: String): Boolean =
        ownBuildings.any { it.selfName == buildingName }

    fun isWaterBase(base: TileActor): Boolean = base.isWaterBase
}
