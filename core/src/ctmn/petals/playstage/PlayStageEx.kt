package ctmn.petals.playstage

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const.PLAY_CAMERA_ZOOM
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.assets
import ctmn.petals.effects.AnimationEffect
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.Decorator.nameNoSuffix
import ctmn.petals.tile.*
import ctmn.petals.tile.TerrainNames.land_capturable
import ctmn.petals.tile.components.HealthComponent
import ctmn.petals.unit.*
import ctmn.petals.utils.*
import kotlin.math.max
import kotlin.Array as KArray

fun PlayStage.isOutOfMap(tileX: Int, tileY: Int): Boolean {
    return tileX >= tiledWidth() || tileY >= tiledHeight() || tileX < 0 || tileY < 0
}

fun PlayStage.newLeaderId(): Int {
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

fun PlayStage.getLabel(name: String): LabelActor {
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

fun PlayStage.tiledWidth(): Int {
    return tiledWidth
}

fun PlayStage.tiledHeight(): Int {
    return tiledHeight
}

fun PlayStage.mapWidth(): Float = (tiledWidth() * TILE_SIZE).toFloat()

fun PlayStage.mapHeight(): Float = (tiledHeight() * TILE_SIZE).toFloat()

fun PlayStage.getMapSizedGridOfZero(): KArray<IntArray> {
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

private var moveCostMap: KArray<IntArray>? = null

fun PlayStage.getTileMovementCostMatrix(terrainProps: TerrainProps): KArray<IntArray> {
    if (moveCostMap == null || moveCostMap!!.size != tiledWidth || moveCostMap!![0].size != tiledHeight) {
        moveCostMap = KArray(tiledWidth()) { IntArray(tiledHeight()) { 0 } }
    }

    val moveCostMap = moveCostMap!!

    for (tile in getTiles()) {
        val terrainProp = terrainProps[tile.terrain]
        val movingCost: Int = terrainProp.movingCost
        moveCostMap[tile.tiledX][tile.tiledY] = movingCost
    }

    // for view cost
    val fogLayer = tileLayers.get(10) //TODO fog layer free
    if (fogLayer != null)
        for (tile in fogLayer.children) {
            tile as TileActor
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
fun PlayStage.getTileMovementCostMatrix(
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
fun PlayStage.getMovementGrid(
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
fun PlayStage.getMovementGrid(
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
fun PlayStage.getRangeOfView(unitActor: UnitActor): KArray<IntArray> {
    return getMovementGrid(
        unitActor.viewRange, unitActor.tiledX, unitActor.tiledY,
        TerrainPropsPack.view
    )
}

fun PlayStage.getLabels(): Array<LabelActor> {
    val labels = Array<LabelActor>()

    for (actor in actors) {
        if (actor is LabelActor)
            labels.add(actor)
    }

    return labels
}

fun PlayStage.getEffects(): Array<ctmn.petals.effects.EffectActor> {
    val effects = Array<ctmn.petals.effects.EffectActor>()

    for (actor in actors) {
        if (actor is ctmn.petals.effects.EffectActor)
            effects.add(actor)
    }

    return effects
}

fun PlayStage.getBasesOfEnemyOf(player: Player): Array<TileActor> {
    val bases = Array<TileActor>()
    for (tile in getTiles()) {
        if (tile.isBase && !player.isAllyId(tile.cPlayerId?.playerId ?: Player.NONE))
            bases.add(tile)
    }

    return bases
}

fun PlayStage.getBases(player: Player? = null): Array<TileActor> {
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
fun PlayStage.getCapturablesOf(player: Player): Array<TileActor> {
    val bases = Array<TileActor>()

    for (tile in getTiles()) {
        if (tile.isCapturable)
            if (tile.cPlayerId?.playerId == player.id)
                bases.add(tile)
    }

    return bases
}

fun PlayStage.getUnitOrTile(x: Int, y: Int): Actor? {
    return getUnit(x, y) ?: getTile(x, y)
}

fun PlayStage.getUnit(tile: TileActor): UnitActor? {
    return getUnit(tile.tiledX, tile.tiledY)
}

inline fun <reified T : UnitActor> PlayStage.getUnit(player: Player? = null): T? {
    for (unit in getUnits()) {
        if (unit is T && if (player == null) true else unit.isPlayerUnit(player))
            return unit
    }

    return null
}

/** returns array containing unit and tiles of layer 1 */
fun PlayStage.getTilesAndUnits(): Array<Actor> {
    val array = Array<Actor>()
    for (tile in getTiles())
        array.add(tile)
    for (unit in getUnits())
        array.add(unit)

    return array
}

fun PlayStage.getUnits(): Array<UnitActor> {
    val units = Array<UnitActor>(unitsLayer.children.size)

    for (actor in unitsLayer.children.items) {
        if (actor is UnitActor)
            units.add(actor)
    }

    return units
}

fun PlayStage.getUnitsForLeader(leaderID: Int, excludeLeader: Boolean = false): Array<UnitActor> {
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
fun PlayStage.getUnitsOfPlayer(player: Int, array: Array<UnitActor>? = null): Array<UnitActor> {
    val actors = array ?: Array<UnitActor>()

    for (unit in getUnits()) {
        if (unit.playerId == player)
            actors.add(unit as UnitActor)
    }

    return actors
}

fun PlayStage.getUnitsOfPlayer(player: Player, array: Array<UnitActor>? = null): Array<UnitActor> {
    return getUnitsOfPlayer(player.id, array)
}

/** returns units of the enemy of [player] */
fun PlayStage.getUnitsOfEnemyOf(player: Player, array: Array<UnitActor>? = null): Array<UnitActor> {
    val actors = array ?: Array<UnitActor>()

    for (unit in getUnits()) {
        if (!player.isAlly(unit.teamId))
            actors.add(unit as UnitActor)
    }

    return actors
}

fun PlayStage.getUnitsForTeam(teamId: Int): Array<UnitActor> {
    val units = Array<UnitActor>()

    for (unit in getUnits()) {
        if (unit.teamId == teamId)
            units.add(unit as UnitActor)
    }

    return units
}

fun PlayStage.getLeadUnit(leaderID: Int): UnitActor? {
    if (leaderID <= 0)
        return null

    for (unit in getUnits()) {
        if (unit.isLeader && unit.leaderID == leaderID)
            return unit
    }

    return null
}

/** returns array containing unit and STORY tiles */
fun PlayStage.getAllTilesAndUnits(): Array<Actor> {
    val actors = Array<Actor>()
    for (tileLayer in tileLayers.values()) {
        for (actor in tileLayer.children.items) {
            actors.add(actor)
        }
    }

    for (unit in getUnits()) {
        actors.add(unit)
    }

    return actors
}

fun PlayStage.getAllTiles(): Array<TileActor> {
    val tiles = Array<TileActor>()
    for (tileLayer in tileLayers.values()) {
        for (tile in tileLayer.children.items) {
            if (tile == null) continue

            tiles.add(tile as TileActor)
        }
    }

    return tiles
}

/** returns tiles of layer 1 */
fun PlayStage.getTiles(): Array<TileActor> {
    val tiles = Array<TileActor>(tilesLayer1.children.items.size)

    for (actor in tilesLayer1.children.items) {
        if (actor is TileActor)
            tiles.add(actor)
    }

    return tiles
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

fun PlayStage.shiftLayerAt(tileX: Int, tileY: Int, shiftAmount: Int) {
    val tilesToShift = Array<TileActor>()
    tileLayers.values.forEach {
        if (it != null && tileX in 0 until it.tilesGrid.size && tileY in 0 until (it.tilesGrid[0]?.size ?: 0)) {
            it.tilesGrid[tileX][tileY]?.let { tile ->
                tilesToShift.add(tile)
            }
        }
    }

    tilesToShift.forEach { tile ->
        tile.layer += shiftAmount
        tile.remove()
        addActor(tile)
    }
}

fun PlayStage.damageTile(tile: TileActor, damage: Int) {
    tile.get(HealthComponent::class.java)?.let {
        it.health -= damage

        if (it.health <= 0) {
            destroyTile(tile)
        }
    }
}

fun PlayStage.destroyTile(tile: TileActor) {
    logMsg("Tile destroyed: ${tile.selfName}")

    val debris = TileData.getOrNull(tile.selfName + "_debris") ?:  TileData.getOrNull(tile.nameNoSuffix() + "_debris") ?: TileData.getOrNull(tile.nameNoTeamName() + "_debris") ?: TileData.getOrNull(tile.terrain + "_debris")
    if (debris != null) {
        logMsg("Tile debris added: ${tile.selfName} debris")
        tile.remove()
        val debrisTile = TileActor(debris, tile.layer, tile.tiledX, tile.tiledY)
        addActor(debrisTile)
    } else {
        logMsg("Tile ${tile.selfName} had no debris")
        removeTileSafely(tile)
    }

    val destroyEffect = AnimationEffect(assets.effectsAtlas.findRegions("tile_destroyed"), 0.075f)
    destroyEffect.setPosition(tile.centerX, tile.centerY)
    addActor(destroyEffect)
}

fun PlayStage.removeTileSafely(tile: TileActor) {
    shiftLayerAt(tile.tiledX, tile.tiledY, 1)

    if (getTile(tile.tiledX, tile.tiledY) == null) {
        logErr("No back tile; added a grass tile then")
        addActor(
            TileActor(
                TileData.get("grass")!!,
                1,
                tile.tiledX,
                tile.tiledY
            )
        )

    }

    tile.remove()
}