package ctmn.petals.playstage

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Array
import ctmn.petals.Config.PLAY_CAMERA_ZOOM
import ctmn.petals.Config.TILE_SIZE
import ctmn.petals.assets
import ctmn.petals.effects.AnimationEffect
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.Decorator.nameNoSuffix
import ctmn.petals.sim.BattleField
import ctmn.petals.tile.*
import ctmn.petals.tile.TerrainNames.land_capturable
import ctmn.petals.tile.components.HealthComponent
import ctmn.petals.unit.*
import ctmn.petals.utils.*
import kotlin.math.max
import kotlin.Array as KArray

/** Copies the interface's [List] into a libGDX [Array] so the old return types stay unchanged. */
private fun <T> List<T>.toGdxArray(): Array<T> {
    val array = Array<T>(size)
    for (item in this) array.add(item)
    return array
}

fun BattleField.isOutOfMap(tileX: Int, tileY: Int): Boolean {
    return tileX >= tiledWidth() || tileY >= tiledHeight() || tileX < 0 || tileY < 0
}

fun BattleField.newLeaderId(): Int {
    var id: Int
    random@ while (true) {
        id = MathUtils.random(1000, 9999)

        for (unit in getUnits())
            if (id == unit.leaderID || id == unit.followerID)
                continue@random

        break
    }

    return id
}

fun BattleField.getLabel(name: String): LabelActor {
    for (label in getLabels())
        if (label.labelName == name)
            return label

    throw IllegalArgumentException("No label with name '$name' found")
}

fun isInRange(x: Int, y: Int, range: Int, x2: Int, y2: Int): Boolean {
    return tiledDst(x, y, x2, y2) <= range
}

fun tiledDst(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    var x = x1 - x2
    var y = y1 - y2
    if (x < 0)
        x *= -1
    if (y < 0)
        y *= -1
    return x + y
}

fun tiledDst(tile1: TileActor, tile2: TileActor): Int {
    return tiledDst(tile1.tiledX, tile1.tiledY, tile2.tiledX, tile2.tiledY)
}

fun tiledDst(unit: UnitActor, tile2: TileActor): Int {
    return tiledDst(unit.tiledX, unit.tiledY, tile2.tiledX, tile2.tiledY)
}

fun tiledDst(unit: UnitActor, unit2: UnitActor): Int {
    return tiledDst(unit.tiledX, unit.tiledY, unit2.tiledX, unit2.tiledY)
}

fun PlayStage.zoomCameraByDefault() {
    (camera as OrthographicCamera).zoom = PLAY_CAMERA_ZOOM

//    val wDiff = mapWidth() / camera.viewportWidth
//    val hDiff = mapHeight() / camera.viewportHeight
//    val diff = if (wDiff > hDiff) wDiff else hDiff
//    (camera as OrthographicCamera).zoom = diff + 0.08f
}

fun PlayStage.centerCameraByDefault() {
    camera.position.set(mapWidth() / 2, mapHeight() / 2, camera.position.z)
}

fun BattleField.tiledWidth(): Int {
    return this.tiledWidth
}

fun BattleField.tiledHeight(): Int {
    return this.tiledHeight
}

fun BattleField.mapWidth(): Float = (tiledWidth() * TILE_SIZE).toFloat()

fun BattleField.mapHeight(): Float = (tiledHeight() * TILE_SIZE).toFloat()

fun BattleField.getMapSizedGridOfZero(): KArray<IntArray> {
    val sizeX = tiledWidth()
    val sizeY = tiledHeight()

    return KArray(sizeX) { IntArray(sizeY) { 0 } }
}

/** @return Two-dimensional array of Ints
 *   example of use of returned value (moveCostMap)
 *   for (x in moveCostMap.indices) {
 *       for (y in moveCostMap[x].indices) {
 *       val cost = moveCostMap[x][y]
 *       //..
 *       }
 *   }
 */
fun BattleField.getTileMovementCostMatrix(terrainProps: TerrainProps): KArray<IntArray> {
    var moveCostMapTmp: KArray<IntArray> = KArray(tiledWidth()) { IntArray(tiledHeight()) { 0 } }

//    if (moveCostMapTmp == null || moveCostMapTmp!!.size != tiledWidth || moveCostMapTmp!![0].size != tiledHeight) {
//        moveCostMapTmp = KArray(tiledWidth()) { IntArray(tiledHeight()) { 0 } }
//    } else {
//        moveCostMapTmp?.let { moveCostMap ->
//            for (i in moveCostMap.indices) {
//                moveCostMap[i].fill(0)
//            }
//        }
//    }

    val moveCostMap = moveCostMapTmp!!

    for (tile in getTiles()) {
        val terrainProp = terrainProps[tile.terrain]
        val movingCost: Int = terrainProp.movingCost
        moveCostMap[tile.tiledX][tile.tiledY] = movingCost
    }

    // for view cost
    val fogTiles = tilesOfLayer(10) //TODO fog layer free
    for (tile in fogTiles) {
        if (tile.terrain == TerrainNames.fog)
            moveCostMap[tile.tiledX][tile.tiledY] = terrainProps[TerrainNames.fog].movingCost
    }

    return moveCostMap
}

private val directions = listOf(
    Pair(0, 1), // right
    Pair(0, -1), // left
    Pair(1, 0), // down
    Pair(-1, 0) // up
)

/** With impassable and unit presence cost */
fun BattleField.getTileMovementCostMatrix(
    unitActor: UnitActor,
    unitsAsImpassable: Boolean = true,
    presenceCost: Boolean = true,
    exceptTeam: Int = unitActor.teamId,
): KArray<IntArray> {

    val moveCostMap = getTileMovementCostMatrix(unitActor.cTerrainProps ?: TerrainPropsPack.foot)

    //count units as Impassable (999 movement cost)
    if (unitsAsImpassable) {
        for (x in moveCostMap.indices) {
            for (y in moveCostMap[x].indices) {
                val unit = getUnit(x, y)
                if (unit != null && unit.teamId != exceptTeam)
                    moveCostMap[x][y] = 999
            }
        }
    }

    //presence cost
    if (presenceCost) {
        for (x in moveCostMap.indices) {
            for (y in moveCostMap[x].indices) {
                val unit = getUnit(x, y)
                if (unit != null && !unit.isAlly(unitActor)) {
                    for ((dx, dy) in directions) {
                        val newX = x + dx
                        val newY = y + dy
                        if (newX in moveCostMap.indices && newY in moveCostMap[0].indices && moveCostMap[newX][newY] >= 0) {
                            moveCostMap[newX][newY] += 1
                        }
                    }
                }
            }
        }
    }

    return moveCostMap
}

/** @return Two-dimensional array of Ints where if value == 0 then u cant move there */
fun BattleField.getMovementGrid(
    unitActor: UnitActor,
    unitsAsImpassable: Boolean = false,
    exceptTeam: Int = unitActor.teamId,
): KArray<IntArray> {
    with(unitActor) {
        return getMovementGrid(
            movingRange,
            tiledX, tiledY,
            terrainCost,
            unitsAsImpassable,
            exceptTeam,
            true,
            unitActor,
        )
    }
}

/** @return Two-dimensional array of Ints representing tiles available to move on
 * if array[x][y] > 0 then you can move there */
fun BattleField.getMovementGrid(
    distance: Int, x: Int, y: Int,
    terrainPropsPack: TerrainProps,
    unitsAsImpassable: Boolean = false,
    exceptTeam: Int = -1,
    presenceCost: Boolean = false,
    forUnit: UnitActor? = null,
): KArray<IntArray> {

    val moveCostGrid: kotlin.Array<IntArray> = getTileMovementCostMatrix(terrainPropsPack)

    //count units as Impassable (999 movement cost)
    if (unitsAsImpassable) {
        for (x in moveCostGrid.indices) {
            for (y in moveCostGrid[x].indices) {
                val unit = getUnit(x, y)
                if (unit != null && unit.teamId != exceptTeam)
                    moveCostGrid[x][y] = UNREACHABLE

                if (forUnit != null && moveCostGrid[x][y] == ONE_TILE)
                    moveCostGrid[x][y] = forUnit.movingRange - 1
            }
        }
    }

    //make land crystals unreachable for water units
    if (terrainPropsPack.contains(land_capturable)) {
        if (terrainPropsPack[land_capturable].movingCost == UNREACHABLE) {
            for (x in moveCostGrid.indices) {
                for (y in moveCostGrid[x].indices) {
                    val tile = getTile(x, y) ?: continue
                    if (getTile(x, y, tile.layer - 1)
                            ?.isFluid == false && (tile.terrain == TerrainNames.crystals || tile.terrain == TerrainNames.base)
                    )
                        moveCostGrid[x][y] = UNREACHABLE
                }
            }
        }
    }

    //...
    val moveGrid = KArray(moveCostGrid.size) { IntArray(moveCostGrid[0].size) }
    moveGrid[x][y] = distance + 1

    //...
    for (k in 0 until distance) {
        val extendArea = kotlin.Array(moveCostGrid.size) { IntArray(moveCostGrid[0].size) }
        // presenceCost: every non ally standing next to the tile being entered adds one to the
        // cost of that tile, so a unit can afford one step but keeps no budget left.
        //
        // The value below is the budget LEFT after stepping, not a cost, so the cost is paid by
        // subtracting from it. Adding instead made moving next to enemies cheaper and widened a
        // unit's range there, which is the reverse of the mechanic; it was "fixed" that way once.
        for (i in moveCostGrid.indices) {
            for (j in moveCostGrid[0].indices) {
                if (moveGrid[i][j] > 1) {
                    // exception occurs when near edges of map
                    try {
                        if (moveGrid[i][j] > moveGrid[i + 1][j] && extendArea[i + 1][j] < moveGrid[i][j] - 1 - moveCostGrid[i + 1][j]) {
                            var extendCost = moveGrid[i][j] - 1 - moveCostGrid[i + 1][j]
                            if (extendCost > 1 && presenceCost && forUnit != null) {
                                val unitsAround = getSurroundingUnits(i + 1, j).filter { !it.isAlly(forUnit) }
                                extendCost = max(1, extendCost - unitsAround.size)
                            }
                            extendArea[i + 1][j] = extendCost
                        }
                    } catch (_: ArrayIndexOutOfBoundsException) {
                    }

                    try {
                        if (moveGrid[i][j] > moveGrid[i][j + 1] && extendArea[i][j + 1] < moveGrid[i][j] - 1 - moveCostGrid[i][j + 1]) {
                            //extendArea[i][j + 1] = moveMap[i][j] - 1 - moveCostMap[i][j + 1]

                            var extendCost = moveGrid[i][j] - 1 - moveCostGrid[i][j + 1]
                            if (extendCost > 1 && presenceCost && forUnit != null) {
                                val unitsAround = getSurroundingUnits(i, j + 1).filter { !it.isAlly(forUnit) }
                                extendCost = max(1, extendCost - unitsAround.size)
                            }
                            extendArea[i][j + 1] = extendCost
                        }
                    } catch (_: ArrayIndexOutOfBoundsException) {
                    }

                    try {
                        if (moveGrid[i][j] > moveGrid[i - 1][j] && extendArea[i - 1][j] < moveGrid[i][j] - 1 - moveCostGrid[i - 1][j]) {
                            //extendArea[i - 1][j] = moveMap[i][j] - 1 - moveCostMap[i - 1][j]

                            var extendCost = moveGrid[i][j] - 1 - moveCostGrid[i - 1][j]
                            if (extendCost > 1 && presenceCost && forUnit != null) {
                                val unitsAround = getSurroundingUnits(i - 1, j).filter { !it.isAlly(forUnit) }
                                extendCost = max(1, extendCost - unitsAround.size)
                            }
                            extendArea[i - 1][j] = extendCost
                        }
                    } catch (_: ArrayIndexOutOfBoundsException) {
                    }

                    try {
                        if (moveGrid[i][j] > moveGrid[i][j - 1] && extendArea[i][j - 1] < moveGrid[i][j] - 1 - moveCostGrid[i][j - 1]) {
                            //extendArea[i][j - 1] = moveMap[i][j] - 1 - moveCostMap[i][j - 1]

                            var extendCost = moveGrid[i][j] - 1 - moveCostGrid[i][j - 1]
                            if (extendCost > 1 && presenceCost && forUnit != null) {
                                val unitsAround = getSurroundingUnits(i, j - 1).filter { !it.isAlly(forUnit) }
                                extendCost = max(1, extendCost - unitsAround.size)
                            }
                            extendArea[i][j - 1] = extendCost
                        }
                    } catch (_: ArrayIndexOutOfBoundsException) {
                    }
                }
            }
        }
        for (i in moveCostGrid.indices) {
            for (j in moveCostGrid[0].indices) {
                if (extendArea[i][j] > 0)
                    moveGrid[i][j] = extendArea[i][j]
            }
        }
    }

    //printGrid(moveMap)

    return moveGrid
}

/**
 * A route the unit could walk to [destX]:[destY] within its movement budget, or null.
 *
 * Dijkstra over the same costs `getMovementGrid` uses: one step plus the terrain entry cost,
 * minus one per adjacent non ally (units near the route make it cheaper, never dearer), with
 * tiles held by another team blocked. The result only decides between an optimistic move and a
 * server verified one: any disagreement with the grid just sends the move to the server, never
 * a wrong verdict.
 *
 * @return the tiles after the unit's own, ending in the destination, or null when unreachable.
 */
/**
 * The movement expansion `getMovementGrid` runs, with the parent of every tile remembered.
 *
 * This is the same relaxation rounds for rounds, so its verdict agrees with the grid by
 * construction: a tile is reached here exactly when the grid says the unit can move there.
 * A Dijkstra would count cost where the grid counts steps, and the two disagree as soon as
 * enemies stand near the route, which is precisely the case this exists for.
 */
internal class MoveSearch(
    val reached: Set<Pair<Int, Int>>,
    val parent: Map<Pair<Int, Int>, Pair<Int, Int>>,
)

internal fun BattleField.searchMoves(unit: UnitActor): MoveSearch {
    val budget = unit.movingRange
    val props = unit.cTerrainProps ?: TerrainPropsPack.foot

    // Entry costs, mirroring the grid: terrain, other teams impassable, land crystals and
    // bases closed to water.
    val width = tiledWidth()
    val height = tiledHeight()
    val entryCost = KArray(width) { IntArray(height) { UNREACHABLE } }
    for (tile in getTiles()) {
        entryCost[tile.tiledX][tile.tiledY] = props[tile.terrain].movingCost
    }
    for (x in 0 until width) {
        for (y in 0 until height) {
            val occupant = getUnit(x, y)
            if (occupant != null && occupant != unit && occupant.teamId != unit.teamId) {
                entryCost[x][y] = UNREACHABLE
            } else if (entryCost[x][y] == ONE_TILE) {
                entryCost[x][y] = budget - 1
            }
        }
    }
    if (props.contains(land_capturable)) {
        if (props[land_capturable].movingCost == UNREACHABLE) {
            for (tile in getTiles()) {
                if (getTile(tile.tiledX, tile.tiledY, tile.layer - 1)?.isFluid == false &&
                    (tile.terrain == TerrainNames.crystals || tile.terrain == TerrainNames.base)
                ) {
                    entryCost[tile.tiledX][tile.tiledY] = UNREACHABLE
                }
            }
        }
    }

    val remaining = KArray(width) { IntArray(height) }
    val parent = HashMap<Pair<Int, Int>, Pair<Int, Int>>()
    remaining[unit.tiledX][unit.tiledY] = budget + 1

    // One round per step of range: a tile s steps away takes s rounds, whatever it costs.
    for (k in 0 until budget) {
        val extendArea = KArray(width) { IntArray(height) }
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (remaining[x][y] <= 1) continue
                for ((dx, dy) in directions) {
                    val nextX = x + dx
                    val nextY = y + dy
                    if (isOutOfMap(nextX, nextY)) continue
                    if (remaining[x][y] <= remaining[nextX][nextY]) continue

                    var reach = remaining[x][y] - 1 - entryCost[nextX][nextY]
                    if (reach > 1) {
                        // Same rule as the movement grid, and the same sign: reach is the budget
                        // left, so company near the tile being entered costs budget rather than
                        // granting it. The two must agree, or a move would be judged optimistic
                        // here and refused by the server.
                        val company = getSurroundingUnits(nextX, nextY)
                            .count { it != unit && !it.isAlly(unit) }
                        reach = max(1, reach - company)
                    }
                    if (extendArea[nextX][nextY] < reach) {
                        extendArea[nextX][nextY] = reach

                        // The parent is set once, on the first round that reaches a tile.
                        //
                        // It used to be rewritten whenever a later round found a better reach, and
                        // reach is not monotonic: the near enemy bonus below can raise it, so two
                        // neighbouring tiles could end up as each other's parent. Walking that back
                        // to the unit never finished, which froze the whole game on a move. One
                        // parent per tile makes the parents a tree, and a tree cannot cycle.
                        if (parent[nextX to nextY] == null) parent[nextX to nextY] = x to y
                    }
                }
            }
        }
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (extendArea[x][y] > 0) remaining[x][y] = extendArea[x][y]
            }
        }
    }

    val reached = HashSet<Pair<Int, Int>>()
    for (x in 0 until width) {
        for (y in 0 until height) {
            if (remaining[x][y] != 0) reached.add(x to y)
        }
    }
    return MoveSearch(reached, parent)
}

/** Every tile the unit could reach this turn. Agrees with `getMovementGrid` by construction. */
fun BattleField.reachableMoveTiles(unit: UnitActor): Set<Pair<Int, Int>> =
    searchMoves(unit).reached

/**
 * A route the unit could walk to [destX]:[destY], or null when unreachable.
 *
 * See `searchMoves` for the costs. The result only decides between an optimistic move and a
 * server verified one: any disagreement with `getMovementGrid` just sends the move to the
 * server, never a wrong verdict.
 *
 * @return the tiles after the unit's own, ending in the destination.
 */
fun BattleField.findMovePath(unit: UnitActor, destX: Int, destY: Int): List<Pair<Int, Int>>? {
    if (unit.tiledX == destX && unit.tiledY == destY) return emptyList()

    val search = searchMoves(unit)
    val dest = destX to destY
    if (!search.reached.contains(dest)) return null

    val route = ArrayList<Pair<Int, Int>>()
    val seen = HashSet<Pair<Int, Int>>()
    var step = dest
    while (step.first != unit.tiledX || step.second != unit.tiledY) {
        // A parent chain that revisits a tile would loop forever, and an infinite loop here
        // freezes the game, so it is not left to the parent map being well formed. Reporting no
        // route costs only an optimistic move: the move is sent to the server instead.
        if (!seen.add(step)) return null

        route.add(step)
        step = search.parent[step] ?: return null
    }
    return route.reversed()
}

fun printGrid(array: KArray<IntArray>) {
    println("Array start:")
    for (y in array[0].indices.reversed()) {
        for (x in 0 until array.size) {
            print("${array[y][x]}")
        }
        println()
    }
    println("Array end.")
}

/** returns a two-dim int array with the size of the map; if a cell value is 0 then it's not visible for the [unitActor] */
fun BattleField.getRangeOfView(unitActor: UnitActor): KArray<IntArray> {
    return getMovementGrid(
        unitActor.viewRange, unitActor.tiledX, unitActor.tiledY,
        TerrainPropsPack.view
    )
}

fun BattleField.getLabels(): Array<LabelActor> {
    return allActors.filterIsInstance<LabelActor>().toGdxArray()
}

fun PlayStage.getEffects(): Array<ctmn.petals.effects.EffectActor> {
    val effects = Array<ctmn.petals.effects.EffectActor>()

    for (actor in actors) {
        if (actor is ctmn.petals.effects.EffectActor)
            effects.add(actor)
    }

    return effects
}

fun BattleField.getBasesOfEnemyOf(player: Player): Array<TileActor> {
    val bases = Array<TileActor>()
    for (tile in getTiles()) {
        if (tile.isBase && !player.isAllyId(tile.cPlayerId?.playerId ?: Player.NONE))
            bases.add(tile)
    }

    return bases
}

fun BattleField.getBases(player: Player? = null): Array<TileActor> {
    val bases = Array<TileActor>()

    for (tile in getTiles()) {
        if (tile.isBase)
            if (player == null)
                bases.add(tile)
            else
                if (player.isAllyId(tile.cPlayerId?.playerId ?: Player.NONE))
                    bases.add(tile)
    }

    return bases
}

/** get capturable tiles owned by player */
fun BattleField.getCapturablesOf(player: Player): Array<TileActor> {
    val bases = Array<TileActor>()

    for (tile in getTiles()) {
        if (tile.isOwnable)
            if (tile.cPlayerId?.playerId == player.id)
                bases.add(tile)
    }

    return bases
}

fun BattleField.getBuildings(player: Player): Array<TileActor> {
    val buildings = Array<TileActor>()

    for (tile in getTiles()) {
        if (tile.isBuilding)
            if (tile.cPlayerId?.playerId == player.id)
                buildings.add(tile)
    }

    return buildings
}

fun BattleField.getUnitOrTile(x: Int, y: Int): Actor? {
    return getUnit(x, y) ?: getTile(x, y)
}

fun BattleField.getUnit(tile: TileActor): UnitActor? {
    return getUnit(tile.tiledX, tile.tiledY)
}

inline fun <reified T : UnitActor> BattleField.getUnit(player: Player? = null): T? {
    for (unit in getUnits()) {
        if (unit is T && if (player == null) true else unit.isPlayerUnit(player))
            return unit
    }

    return null
}

/** returns array containing unit and tiles of layer 1 */
fun BattleField.getTilesAndUnits(): Array<Actor> {
    val array = Array<Actor>()
    for (tile in getTiles())
        array.add(tile)
    for (unit in getUnits())
        array.add(unit)

    return array
}

fun BattleField.getUnits(): Array<UnitActor> {
    return allUnits.toGdxArray()
}

fun BattleField.getUnitsForLeader(leaderID: Int, excludeLeader: Boolean = false): Array<UnitActor> {
    val units = Array<UnitActor>()

    for (unit in getUnits()) {
        val leader = unit.cLeader
        val follower = unit.cFollower

        if (leader != null && excludeLeader)
            continue
//        if (follower == null && leaderID != -1)
//            continue

        if (follower?.leaderID == leaderID || leader?.leaderID == leaderID || (follower == null && leader == null))
            units.add(unit)
    }

    return units
}

/** returns units of [player] */
fun BattleField.getUnitsOfPlayer(player: Int, array: Array<UnitActor>? = null): Array<UnitActor> {
    val actors = array ?: Array<UnitActor>()

    for (unit in getUnits()) {
        if (unit.playerId == player)
            actors.add(unit as UnitActor)
    }

    return actors
}

fun BattleField.getUnitsOfPlayer(player: Player, array: Array<UnitActor>? = null): Array<UnitActor> {
    return getUnitsOfPlayer(player.id, array)
}

/** returns units of the enemy of [player] */
fun BattleField.getUnitsOfEnemyOf(player: Player, array: Array<UnitActor>? = null): Array<UnitActor> {
    val actors = array ?: Array<UnitActor>()

    for (unit in getUnits()) {
        if (!player.isAlly(unit.teamId))
            actors.add(unit as UnitActor)
    }

    return actors
}

fun BattleField.getUnitsForTeam(teamId: Int): Array<UnitActor> {
    val units = Array<UnitActor>()

    for (unit in getUnits()) {
        if (unit.teamId == teamId)
            units.add(unit as UnitActor)
    }

    return units
}

fun BattleField.getLeadUnit(leaderID: Int): UnitActor? {
    if (leaderID <= 0)
        return null

    for (unit in getUnits()) {
        if (unit.isLeader && unit.leaderID == leaderID)
            return unit
    }

    return null
}

/** returns array containing unit and STORY tiles */
fun BattleField.getAllTilesAndUnits(): Array<Actor> {
    val actors = Array<Actor>()
    for (tile in allTiles) {
        actors.add(tile)
    }

    for (unit in getUnits()) {
        actors.add(unit)
    }

    return actors
}

fun BattleField.getAllTiles(): Array<TileActor> {
    return allTiles.toGdxArray()
}

/** returns tiles of layer 1 */
fun BattleField.getTiles(): Array<TileActor> {
    return layer1Tiles.toGdxArray()
}

fun getPositionsArray(actors: Array<Actor>): Array<Vector2> {
    val array = Array<Vector2>(actors.size)
    for (actor in actors) {
        array.add(Vector2(actor.x, actor.y))
    }
    return array
}

/** sorting tiles so tiles with sprite size more that TILE_SIZE and higher tiledY will be rendered first */
fun sortTiles(tiles: List<TileActor>): Array<TileActor> {
    // collect trees and other tiles with sprite larger that TILE_SIZE and sort them
    val sortedHash = HashMap<Int, Array<TileActor>>()
    for (tile in tiles) {
        if (!sortedHash.containsKey(tile.tiledY)) sortedHash[tile.tiledY] = Array<TileActor>()

        if (tile.sprite.height == TILE_SIZE.toFloat() ||
            tile.sprite.height == 64f
        ) {
            sortedHash[tile.tiledY]!!.add(null)
            continue
        }

        sortedHash[tile.tiledY]!!.add(tile)
    }
    sortedHash.values.forEach { it.removeAll { tile -> tile == null } }

    val sortedTiles = Array<TileActor>()

    for (key in sortedHash.keys) {
        sortedHash[key]!!.forEach { tile -> sortedTiles.add(tile) }
    }

    sortedTiles.reverse()

    // collect tiles such as grass, floors and other tiles with sprite size of TILE_SIZE
    val groundTiles = Array<TileActor>()
    for (tile in tiles) {
        if (tile.sprite.height == TILE_SIZE.toFloat() ||
            tile.sprite.height == 64f
        )
            groundTiles.add(tile)
    }

    return Array<TileActor>().apply { addAll(groundTiles); addAll(sortedTiles) }
}

fun BattleField.damageTile(tile: TileActor, damage: Int) {
    tile.get(HealthComponent::class.java)?.let {
        it.health -= damage

        if (it.health <= 0) {
            destroyTile(tile)
        }
    }
}

fun BattleField.destroyTile(tile: TileActor) {
    logMsg("Tile destroyed: ${tile.selfName}")

    val debris = TileData.getOrNull(tile.selfName + "_debris") ?: TileData.getOrNull(tile.nameNoSuffix() + "_debris")
    ?: TileData.getOrNull(tile.nameNoTeamName() + "_debris") ?: TileData.getOrNull(tile.terrain + "_debris")
    if (debris != null) {
        logMsg("Tile debris added: ${tile.selfName} debris")
        removeGameActor(tile)
        val debrisTile = TileActor(debris, tile.layer, tile.tiledX, tile.tiledY)
        addGameActor(debrisTile)
    } else {
        logMsg("Tile ${tile.selfName} had no debris")
        removeTileSafely(tile)
    }

    if (this is ctmn.petals.playstage.PlayStage) {
        val destroyEffect = AnimationEffect(assets.effectsAtlas.findRegions("tile_destroyed"), 0.075f)
        destroyEffect.setPosition(tile.centerX, tile.centerY)
        addGameActor(destroyEffect)
    }
}

fun BattleField.removeTileSafely(tile: TileActor) {
    shiftLayerAt(tile.tiledX, tile.tiledY, 1)

    if (getTile(tile.tiledX, tile.tiledY) == null) {
        logErr("No back tile; added a grass tile then")
        addGameActor(
            TileActor(
                TileData.get("grass")!!,
                1,
                tile.tiledX,
                tile.tiledY
            )
        )

    }

    removeGameActor(tile)
}
