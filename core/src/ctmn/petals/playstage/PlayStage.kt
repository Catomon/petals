package ctmn.petals.playstage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ArrayMap
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ctmn.petals.assets
import ctmn.petals.map.label.LabelActor
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playscreen.events.PlayStageListener
import ctmn.petals.playscreen.events.UnitAddedEvent
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cUnit
import ctmn.petals.utils.TintShader
import ctmn.petals.utils.tiledX
import ctmn.petals.utils.tiledY
import ctmn.petals.widgets.addNotifyWindow

/** The stage of the game.
 * Do not use clear() for removing tiles and units. It's removing all layer groups as well as actors for drawing gui.
 * Use clearGameActors() for that. */
class PlayStage(
    batch: Batch,
    var tiledWidth: Int = 0, //specify only if map is very large (>64)
    var tiledHeight: Int = 0,
) : Stage(ExtendViewport(400f, 240f), batch) {

    var initView = true

    val tileLayers = ArrayMap<Int, PlayStageGroup>()
    val tilesLayer1 = PlayStageGroup().also {
        it.name = "tiles"
        tileLayers.put(0, PlayStageGroup().apply { name = "tiles" })
        tileLayers.put(1, it)
    }
    val unitsLayer = PlayStageGroup().apply { name = "units" }

    private val beforeTilesActor = PlayStageGroup().apply { name = "before_tiles" }
    private val afterTilesActor = PlayStageGroup().apply { name = "after_tiles" }

    private val nightTint = Vector3(0.2f, 0.3f, 0.5f)
    private val eveningTint = Vector3(1.0f, 0.5f, 0.0f)
    private val tintShader = TintShader(eveningTint, 0.5f)

    private var shaderApplied = false

    var idCounter = 0
    private val newId get() = "%06d".format(idCounter++)

    var timeOfDay = DayTime.DAY
        set(value) {
            field = value

            when (value) {
                DayTime.NIGHT -> tintShader.intensity = .7f
                DayTime.EVENING -> tintShader.intensity = .3f
                else -> {}
            }
        }

    enum class DayTime {
        DAY,
        EVENING,
        NIGHT
    }

    private val actorIdsMap = HashMap<String, Actor>()
    private val unitPositionsMap =
        Array(if (tiledWidth == 0) 64 else tiledWidth) { Array<UnitActor?>(if (tiledHeight == 0) 64 else tiledHeight) { null } }
    val border = Border(this)

    private val background = Background()
    private val cloudShadowDrawer = CloudShadowDrawer()

    val lightsEngine = LightsEngine(this)

    init {
        root = RootGroup()

        addActor(background)

        for (layer in tileLayers)
            addActor(layer.value)

        addActor(unitsLayer)

        addActor(cloudShadowDrawer)

        addListener {
            if (it is CommandExecutedEvent || it is ActionCompletedEvent) {
                updateCaches()
            }

            false
        }

        root.addActorBefore(tileLayers.firstValue(), beforeTilesActor)
        root.addActorAfter(tileLayers.last().value, afterTilesActor)
    }

    override fun draw() {
        camera.update()

        if (!root.isVisible) return

        val batch = batch
        batch.projectionMatrix = camera.combined
        batch.begin()

//        border.draw(batch)

        //draw PlayStageGroup actors
        beforeTilesActor.draw(batch, 1f)

        for (actor in actors) {
            if (actor.name == "tiles" || actor.name == "background") {
                shaderBegin()

                actor.draw(batch, 1f)

                shaderEnd()
            }
        }

        border.draw(batch)

        afterTilesActor.draw(batch, 1f)

        for (actor in actors) {
            if (actor.name == "units") {
                shaderBegin()

                actor.draw(batch, 1f)

                shaderEnd()
            }
        }

        //all other actors are drawn last
        for (actor in actors) {
            if (actor !is PlayStageGroup) {
                if (actor.isVisible)
                    actor.draw(batch, actor.color.a)
            }
        }

        batch.end()

        lightsEngine.draw(batch.projectionMatrix)
    }

    private fun shaderBegin() {
        when (timeOfDay) {
            DayTime.DAY -> return
            DayTime.EVENING -> tintShader.tint = eveningTint
            DayTime.NIGHT -> tintShader.tint = nightTint
        }

        batch.shader = tintShader.shader
        tintShader.setUniformfs()

        shaderApplied = true
    }

    private fun shaderEnd() {
        if (shaderApplied) {
            batch.shader = null

            shaderApplied = false
        }
    }

    fun updateCaches() {
        unitPositionsMap.forEach { it.forEachIndexed { i, _ -> it[i] = null } }
        for (unit in getUnits()) {
            if (unit.tiledX >= 0 && unit.tiledY >= 0 && unit.tiledX < unitPositionsMap.size && unit.tiledY < unitPositionsMap[0].size)
                unitPositionsMap[unit.tiledX][unit.tiledY] = unit
        }
    }

    fun addActorBeforeTiles(actor: Actor) {
        beforeTilesActor.addActor(actor)
    }

    fun addActorAfterTiles(actor: Actor) {
        afterTilesActor.addActor(actor)
    }

    private fun addTile(tile: TileActor) {
        if (tileLayers.containsKey(tile.layer))
            tileLayers[tile.layer].addActor(tile)
        else {
            tileLayers.put(tile.layer, PlayStageGroup().apply {
                name = "tiles"
                addActor(tile)
                this@PlayStage.root.addActorAfter(tileLayers.get(tile.layer - 1), this)
            })

            tileLayers.keys.sortedByDescending { it }.forEach { key ->
                if (key != null) {
                    tileLayers[key].remove()
                    root.addActorAfter(background, tileLayers[key])
                }
            }
        }

        if (initView && tile.tileViewComponent == null)
            tile.initView()
    }

    override fun addActor(actor: Actor) {
        if (initView && actor is UnitActor && !actor.isViewInitialized) {
            actor.initView(assets)

            actor.updateView()
        }

        // trigger Actor.positionChanged()
        if (actor is UnitActor) {
            val x = actor.tiledX
            val y = actor.tiledY
            actor.setPosition(0, 0)
            actor.setPosition(x, y)
        }

        when (actor) {
            is TileActor -> {
                if (actor.name == null) actor.name = actor.tileName + "@" + newId

                if (tiledWidth < actor.tiledX + 1) tiledWidth = actor.tiledX + 1
                if (tiledHeight < actor.tiledY + 1) tiledHeight = actor.tiledY + 1

                actor.setPosition(actor.tiledX, actor.tiledY)
                addTile(actor)
            }

            is UnitActor -> {
                if (actor.name == null) actor.name = actor.cUnit.name + "@" + newId

                unitsLayer.addActor(actor)

                if (initView)
                    root.fire(UnitAddedEvent(actor))
            }

            else -> super.addActor(actor)
        }
    }

    fun clearUnits() {
        for (unit in unitsLayer.children) {
            unit.remove()
        }
        unitsLayer.clear()
    }

    fun clearTiles() {
        for (layer in tileLayers) {
            for (tile in layer.value.children) {
                tile.remove()
            }
            layer.value.clear()
            layer.value.tilesGrid.clear()
        }
    }

    private fun clearLabels() {
        for (actor in actors) {
            if (actor is LabelActor)
                actor.remove()
        }
    }

    private fun clearMinorGameActors() {
        actors.forEach {
            if (it is GameActor)
                it.remove()
        }
    }

    fun clearGameActors() {
        clearTiles()

        clearUnits()

        clearLabels()

        clearMinorGameActors()
    }

    fun findUnit(name: String): UnitActor? {
        if (actorIdsMap.containsKey(name))
            return actorIdsMap[name] as UnitActor

        return null
    }

    fun getUnit(x: Int, y: Int): UnitActor? {
        return if (x >= 0 && y >= 0 && x < unitPositionsMap.size && y < unitPositionsMap[0].size) unitPositionsMap[x][y] else null
    }

    fun getTile(x: Int, y: Int): TileActor? {
        if (tilesLayer1.tilesGrid.size == 0) return null
        if (x < 0 || y < 0) return null
        if (x >= tilesLayer1.tilesGrid.size || tilesLayer1.tilesGrid[x] == null || y >= tilesLayer1.tilesGrid[x].size) return null

        return tilesLayer1.tilesGrid.get(x)?.get(y)
    }

    fun getTile(x: Int, y: Int, layerId: Int): TileActor? {
        val layer = tileLayers.get(layerId) ?: return null
        if (layer.tilesGrid.size == 0) return null
        if (x < 0 || y < 0) return null
        if (x >= layer.tilesGrid.size || layer.tilesGrid[x] == null || y >= layer.tilesGrid[x].size) return null

        return layer.tilesGrid.get(x)?.get(y)
    }

    override fun addListener(listener: EventListener): Boolean {
        if (listener is PlayStageListener) {
            listener.playStage = this
            listener.onPlayStage()
        }

        return super.addListener(listener)
    }

    fun onScreenResize(width: Int, height: Int) {
        viewport.update(width, height, false)
        lightsEngine.update(width, height)
    }

    private inner class Background : PlayStageGroup() {

        private val backTile = TileActor("grass", "grass").apply { initView() }

        init {
            name = "background"
        }

        override fun draw(batch: Batch, parentAlpha: Float) {
            for (tile in tilesLayer1.children) {
                backTile.setPosition(tile.tiledX, tile.tiledY)
                //todo remove when fix map generator
                //backTile.draw(batch, parentAlpha)
            }
        }
    }

    open inner class PlayStageGroup : Group() {

        val tilesGrid = Array<Array<TileActor?>>(if (tiledWidth == 0) 64 else tiledWidth)

        override fun draw(batch: Batch, parentAlpha: Float) {
            if (Gdx.input.isKeyPressed(Input.Keys.F3))
                if (tileLayers[-1] != this) return

            if (Gdx.input.isKeyPressed(Input.Keys.F4)) {
                if (tileLayers[0] != this) return
            }

            if (Gdx.input.isKeyPressed(Input.Keys.F5))
                if (tileLayers[1] != this) return

            if (Gdx.input.isKeyPressed(Input.Keys.F6))
                if (tileLayers[2] != this) return

            if (Gdx.input.isKeyPressed(Input.Keys.F7))
                if (tileLayers[3] != this) return

            super.draw(batch, parentAlpha)
        }

        override fun addActor(actor: Actor) {
            if (actor is TileActor) {
                if (tilesGrid.size <= actor.tiledX)
                    tilesGrid.setSize(actor.tiledX + 1)

                if (tilesGrid[actor.tiledX] == null)
                    tilesGrid[actor.tiledX] = Array<TileActor?>(if (tiledHeight == 0) 64 else tiledHeight)

                if (tilesGrid[actor.tiledX].size <= actor.tiledY)
                    tilesGrid[actor.tiledX].setSize(actor.tiledY + 1)

                tilesGrid[actor.tiledX][actor.tiledY] = actor
            }

            if (actor.name != null)
                actorIdsMap[actor.name] = actor

            updateCaches()

            super.addActor(actor)
        }

        override fun removeActor(actor: Actor, unfocus: Boolean): Boolean {
            if (actor is TileActor)
                if (actor.tiledX >= 0 && actor.tiledY >= 0 && actor.tiledX < tilesGrid.size && actor.tiledY < tilesGrid[actor.tiledX].size) {
                    if (actor == tilesGrid[actor.tiledX][actor.tiledY])
                        tilesGrid[actor.tiledX][actor.tiledY] = null
                }

            if (actor.name != null)
                actorIdsMap.remove(actor.name)

            updateCaches()

            return super.removeActor(actor, unfocus)
        }
    }

    private inner class RootGroup : Group() {
        override fun fire(event: Event): Boolean {
            if (event.stage == null) event.stage = stage
            event.target = this

            try {
                // Notify the target capture listeners.
                notify(event, true)
                if (event.isStopped) return event.isCancelled

                // Notify the target listeners.
                notify(event, false)
                if (!event.bubbles) return event.isCancelled
                if (event.isStopped) return event.isCancelled

                return event.isCancelled
            } catch (e: Exception) {
                e.printStackTrace()
                addNotifyWindow(e.message ?: "null", "PlayStage listener exc.")
            }

            return event.isCancelled
        }

        override fun notify(event: Event?, capture: Boolean): Boolean {
            return super.notify(event, capture)
        }
    }
}