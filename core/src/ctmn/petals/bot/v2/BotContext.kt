package ctmn.petals.bot.v2

import ctmn.petals.player.Player
import ctmn.petals.playscreen.income
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getBases
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnitsOfEnemyOf
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.playstage.tiledDst
import ctmn.petals.sim.Battle
import ctmn.petals.sim.BattleField
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.isBase
import ctmn.petals.tile.isBuilding
import ctmn.petals.tile.isCrystal
import ctmn.petals.tile.isOccupied
import ctmn.petals.tile.isWaterBase
import ctmn.petals.tile.components.BaseBuildingComponent
import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.tile.components.CapturingComponent
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

    val battle: Battle get() = bot.battle
    val field: BattleField get() = battle.field
    val player: Player get() = bot.player
    val playerId: Int get() = player.id

    var units: List<UnitActor> = emptyList()
        private set
    var enemies: List<UnitActor> = emptyList()
        private set
    var ownBases: List<TileActor> = emptyList()
        private set

    /** Bases currently under construction by this player. */
    var basesUnderConstruction: Int = 0
        private set

    /** Owned plus under construction: the production capacity already committed to. */
    val baseCount: Int get() = ownBases.size + basesUnderConstruction
    var enemyBases: List<TileActor> = emptyList()
        private set
    var ownBuildings: List<TileActor> = emptyList()
        private set
    var enemyBuildings: List<TileActor> = emptyList()
        private set
    var capturables: List<TileActor> = emptyList()
        private set

    /** Crystal assigned to each free worker this tick. */
    var workerCaptureTargets: Map<String, TileActor> = emptyMap()
        private set

    val workers: List<UnitActor> get() = units.filter { it.isWorker }
    val leaders: List<UnitActor> get() = units.filter { it.isLeader }
    val combatUnits: List<UnitActor> get() = units.filter { !it.isWorker }

    var income: Int = 0
        private set

    fun refresh() {
        units = field.getUnitsOfPlayer(player).toList()
        enemies = field.getUnitsOfEnemyOf(player).toList()
        ownBases = field.getBases(player).toList()
        basesUnderConstruction = field.getTiles().count { tile ->
            tile.get(BaseBuildingComponent::class.java)?.playerId == playerId
        }
        enemyBases = field.getTiles().filter {
            it.isBase && it.cPlayerId?.playerId != null && it.cPlayerId?.playerId != playerId
        }
        ownBuildings = field.getTiles().filter {
            it.isBuilding && it.cPlayerId?.playerId == playerId
        }
        enemyBuildings = field.getTiles().filter {
            it.isBuilding && it.cPlayerId?.playerId != null && it.cPlayerId?.playerId != playerId
        }
        // Only crystal tiles are capturable. Bases can only be destroyed.
        capturables = field.getTiles().filter {
            it.isCrystal && it.cPlayerId?.playerId != playerId
        }
        income = player.income(battle)
        workerCaptureTargets = assignWorkerCaptureTargets()
    }

    /** Assigns at most one worker per crystal. */
    private fun assignWorkerCaptureTargets(): Map<String, TileActor> {
        val freeWorkers = workers.filterNot { isWorkerBusy(it) }
        if (freeWorkers.isEmpty() || capturables.isEmpty()) return emptyMap()

        // Crystals our own workers are already capturing stay reserved to them.
        val reserved = workers
            .filter { isCapturing(it) }
            .mapNotNull { field.getTile(it.tiledX, it.tiledY) }
            .toSet()

        val available = capturables.filter { it !in reserved }
        if (available.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, TileActor>()
        val used = mutableSetOf<TileActor>()

        // Workers already standing on a crystal take it first.
        for (worker in freeWorkers) {
            val tile = field.getTile(worker.tiledX, worker.tiledY) ?: continue
            if (tile in available && tile !in used) {
                result[worker.name] = tile
                used.add(tile)
            }
        }

        // Greedy nearest-pair matching for the rest.
        val pairs = mutableListOf<Pair<UnitActor, TileActor>>()
        for (worker in freeWorkers) {
            if (worker.name in result) continue
            for (crystal in available) {
                if (crystal in used) continue
                pairs.add(worker to crystal)
            }
        }
        pairs.sortBy { (worker, crystal) ->
            tiledDst(worker.tiledX, worker.tiledY, crystal.tiledX, crystal.tiledY)
        }

        val assignedWorkers = result.keys.toMutableSet()
        for ((worker, crystal) in pairs) {
            if (worker.name in assignedWorkers || crystal in used) continue
            result[worker.name] = crystal
            assignedWorkers.add(worker.name)
            used.add(crystal)
        }

        return result
    }

    private fun isCapturing(unit: UnitActor): Boolean =
        field.getTile(unit.tiledX, unit.tiledY)
            ?.get(CapturingComponent::class.java)?.playerId == playerId

    private fun isWorkerBusy(unit: UnitActor): Boolean {
        val tile = field.getTile(unit.tiledX, unit.tiledY) ?: return false
        if (tile.get(CapturingComponent::class.java)?.playerId == playerId) return true
        if (tile.get(BaseBuildingComponent::class.java)?.playerId == playerId) return true
        if (tile.get(BuildingComponent::class.java)?.playerId == playerId) return true
        return false
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

    /** True if the building is finished OR currently under construction by this player. */
    fun ownsOrBuilding(buildingName: String): Boolean {
        if (ownsBuilding(buildingName)) return true
        return field.getTiles().any { tile ->
            tile.get(BuildingComponent::class.java)?.let {
                it.playerId == playerId && it.buildingName == buildingName
            } == true
        }
    }

    fun isWaterBase(base: TileActor): Boolean = base.isWaterBase
}
