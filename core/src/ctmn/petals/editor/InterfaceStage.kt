package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.FocusManager
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.*
import ctmn.petals.Const
import ctmn.petals.game
import ctmn.petals.screens.MenuScreen
import ctmn.petals.utils.*
import ctmn.petals.widgets.*

class InterfaceStage(
    private val canvas: CanvasStage,
    private val actorsPackage: CanvasActorsPackage,
    private val tools: Tools,
    batch: Batch,
) :
    Stage(ExtendViewport(GUI_VIEWPORT_WIDTH, GUI_VIEWPORT_HEIGHT), batch) {

    val mainTable get() = root as VisTable

    val actorsPicker = CanvasActorsPicker()
    private val actorsPickerVerticalTable = VisTable()
    private val actorsPickerHorizontalTable = VisTable()
    private val actorsPickerTable get() = if (IS_PORTRAIT) actorsPickerHorizontalTable else actorsPickerVerticalTable
    private val srollPaneCloseButton = newTextButton(">").addClickListener {
        if (actorsPicker.stage != null) {
            actorsPickerHorizontalTable.clear()
            actorsPickerVerticalTable.clear()
            (it.listenerActor as VisTextButton).setText("<")
        } else {
            actorsPickerHorizontalTable.clear()
            actorsPickerVerticalTable.clear()
            actorsPickerTable.add(actorsPicker)
            (it.listenerActor as VisTextButton).setText(">")
        }
    }

    private val toolButtons = ButtonGroup<VisImageButton>()

    private val layerButton = newTextButton("${tools.pencil.layer} +", "layers")
        .addClickListener { _ ->
            changeLayer(tools.pencil.layer + 1)
        }

    private val layerButtonMin = newTextButton("${tools.pencil.layer} -", "layers")
        .addClickListener {
            changeLayer(tools.pencil.layer - 1)
        }

    private val layerVisibilityAll = "All"
    private val layerVisibilityCurrent = "Current"

    private val layerVisibilityButton: VisTextButton = newTextButton(layerVisibilityAll, "layers").apply {
        addClickListener { _ ->
            setText(if (text.contentEquals(layerVisibilityAll)) layerVisibilityCurrent else layerVisibilityAll)
            changeLayer(tools.pencil.layer)
        }
    }

//    private val mapFileNameTextFieldMaxWidth = 360f
//    private val mapFileNameTextFieldMinWidth = 200f
//    private val mapFileNameTextField = VisTextField("new_map${Random.nextInt(1000, 9999)}").apply {
//        maxLength = 32
//        width = 100f
//        setAlignment(Align.center)
//        /** See [touchDown] */
//    }

    private var mapName = ""
    private val mapsButton = newTextButton("Maps").addClickListener {
        root.findActor<MapsWindow>(MapsWindow::class.simpleName)?.remove()
        addActor(MapsWindow().also {
            it.name = MapsWindow::class.simpleName
            it.centerWindow()
        })
    }

    private val clearButton = newTextButton("Clear").addClickListener {
        if (canvas.isEmpty()) return@addClickListener

        val clear = {
            canvas.clearCanvasActors()
        }
        if (canvas.contentChanged)
            addNotifyWindow("You have unsaved data, clear canvas anyway?", "Clear Canvas", clear, true)
        else
            clear()
    }

    private val exitButton = newTextButton("X").addClickListener {
        val exit = {
            game.screen = MenuScreen()
        }

        if (canvas.contentChanged)
            addNotifyWindow("You have unsaved data, leave anyway?", "Exit", exit, true)
        else
            exit()
    }

    init {
        root = VisTable()

        //table.debug = true

        //setup table
        mainTable.add(actorsPickerVerticalTable).left()
        mainTable.add(GridGroup(srollPaneCloseButton.width).apply {
            addActor(srollPaneCloseButton)
            for (tool in tools.toolList) {
                addActor(newToolButton(tool))
            }
            addActor(layerButton)
            addActor(layerButtonMin)
            addActor(layerVisibilityButton)
        }).minWidth(srollPaneCloseButton.width).padRight(12f).align(Align.top)
        mainTable.add().expandX()
        mainTable.add(VisTable().apply {
            add(mapsButton)
            add(clearButton)
            add(exitButton)
        }).align(Align.topRight)
        mainTable.row().expandY()
        mainTable.add(actorsPickerHorizontalTable).colspan(4).bottom()

        //default
        changeTool(tools.pencil)
        changeLayer(1)
    }

    private fun newToolButton(tool: Tool): VisImageButton {
        return VisImageButton("tool_${tool.name}").apply {
            addClickSound().addFocusBorder().addClickListener { changeTool(tool) }
            this.userObject = tool
            toolButtons.add(this)
        }
    }

    private fun changeLayer(layer: Int) {
        tools.pencil.layer = layer
        layerButton.setText("${tools.pencil.layer} +")
        layerButtonMin.setText("${tools.pencil.layer} -")

        val show = layerVisibilityButton.text.toString()
        if (show == layerVisibilityAll) {
            canvas.changeLayersVisible()
        } else {
            canvas.changeLayersVisible(tools.pencil.layer)
        }

        canvas.highlightLayerId = tools.pencil.layer
    }

    fun changeTool(tool: Tool) {
        tools.current = tool
        toolButtons.buttons.forEach { it.color.a = 0.5f }
        toolButtons.buttons.firstOrNull { it.userObject === tool }?.color?.a = 1f
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (scrollFocus == null) {
            (canvas.viewport.camera as OrthographicCamera).zoom += amountY * 0.1f

            return true
        }

        return super.scrolled(amountX, amountY)
    }

    override fun keyDown(keyCode: Int): Boolean {
        when (keyCode) {
            Keys.NUM_1 -> tools.current = tools.pencil
            Keys.NUM_2 -> tools.current = tools.eraser
            Keys.NUM_3 -> tools.current = tools.dragCanvas
            Keys.ENTER -> {
                FocusManager.resetFocus(this)
            }

            Keys.ESCAPE -> {
                FocusManager.resetFocus(this)
            }
        }

        return super.keyDown(keyCode)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (root.findActor<MapsWindow>(MapsWindow::class.simpleName)?.mapNameField?.hasKeyboardFocus() == true)
            FocusManager.resetFocus(this)
//
//        return super.touchDown(screenX, screenY, pointer, button).also {
//            with(mapFileNameTextField) {
//                if (this.hasKeyboardFocus()) {
//                    (parent as VisTable).getCell(this).prefWidth(mapFileNameTextFieldMaxWidth)
//                    (parent as VisTable).invalidate()
//                    mainTable.invalidate()
//                } else {
//                    (parent as VisTable).getCell(this).prefWidth(mapFileNameTextFieldMinWidth)
//                    (parent as VisTable).invalidate()
//                    mainTable.invalidate()
//                }
//            }
//        }

        return super.touchDown(screenX, screenY, pointer, button)
    }

    fun onScreenResize(width: Int, height: Int) {
        (viewport as ExtendViewport).minWorldWidth = width / GUI_SCALE
        (viewport as ExtendViewport).minWorldHeight = height / GUI_SCALE

        if (IS_PORTRAIT) {
            actorsPicker.setupItemsTable(actorsPicker.horizontal)
            actorsPickerHorizontalTable.clear()
            actorsPickerVerticalTable.clear()
            actorsPickerTable.add(actorsPicker)
        } else {
            actorsPicker.setupItemsTable(actorsPicker.vertical)
            actorsPickerHorizontalTable.clear()
            actorsPickerVerticalTable.clear()
            actorsPickerTable.add(actorsPicker)
        }

        viewport.update(width, height, true)
        mainTable.setSize(viewport.worldWidth, viewport.worldHeight)

        root.findActor<MapsWindow>(MapsWindow::class.simpleName)?.centerWindow()
    }

    inner class CanvasActorsPicker : VisScrollPane(VisTable()) {

        var hovered: Item? = null
        var selected: Item? = null
            set(value) {
                field = value

                if (value != null)
                    tools.pencil.canvasActor = value.userObject as CanvasActor
            }

        val tableWidth = 160f
        private val itemsInRow = if (Const.IS_MOBILE) 4 else 5
        private val itemSize = tableWidth / itemsInRow //32f

        private val selectedFrame = VisImage("selected_item_frame").apply {
            setSize(48f / 32f * itemSize, 48f / 32f * itemSize)
        }

        val horizontal = 0
        val vertical = 1

        fun setupItemsTable(arr: Int) {
            with(this.actor as VisTable) {
                clear()

                width = tableWidth

                val maxInRow = if (arr == horizontal) {
                    setScrollingDisabled(false, true)
                    actorsPackage.canvasActors.size / itemsInRow + 1
                } else {
                    setScrollingDisabled(true, false)
                    itemsInRow
                }
                var currentInRow = 0
                for (canvasActor in actorsPackage.canvasActors) {
                    val item = Item(canvasActor)
                    item.name = canvasActor.name
                    item.userObject = canvasActor

                    add(item).size(itemSize).pad(4f)
                    currentInRow++

                    if (currentInRow >= maxInRow) {
                        row()
                        currentInRow = 0
                    }
                }
            }
        }

        init {
            (this.actor as VisTable).background("background")
            setupItemsTable(horizontal)

            addListener(object : InputListener() {
                override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                    super.enter(event, x, y, pointer, fromActor)

                    scrollFocus = actorsPicker
                }

                override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                    super.exit(event, x, y, pointer, toActor)

                    if (hovered != selected)
                        hovered?.selectedFrame = null

                    scrollFocus = null
                }

                override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
                    val actor = hit(x, y, false)

                    if (hovered != selected)
                        hovered?.selectedFrame = null

                    if (actor is Item?) {
                        hovered = actor
                        actor.selectedFrame = selectedFrame
                    }

                    return false
                }

                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    hit(x, y, false)?.let {
                        if (it is Item) {
                            selected?.selectedFrame = null
                            selected = it
                            it.selectedFrame = selectedFrame

                            changeTool(tools.pencil)
                        }
                    }

                    return false
                }
            })
        }

        inner class Item(canvasActor: CanvasActor) : Widget() {
            var selectedFrame: VisImage? = null

            val sprite = Sprite(canvasActor.sprite)

            private val oversized = sprite.width > tileSize

            override fun draw(batch: Batch?, parentAlpha: Float) {
                selectedFrame?.setPosByCenter(centerX, centerY)
                selectedFrame?.draw(batch, parentAlpha)

                super.draw(batch, parentAlpha)
                sprite.setPositionByCenter(centerX, centerY)
                sprite.draw(batch)
            }

            override fun sizeChanged() {
                super.sizeChanged()

                if (oversized)
                    sprite.setSize(width * 1.5f, height * 1.5f)
                else
                    sprite.setSize(width, height)
            }
        }
    }

    inner class MapsWindow : VisWindow("Maps") {

        val DEFAULT = "DEFAULT"
        val CUSTOM = "CUSTOM"
        val SHARED = "SHARED"

        var mapItems: Map<String, List<MapItem>> = emptyMap()

        val mapNameField = VisTextField(mapName).apply {
            messageText = "Map Name"
            maxLength = 32
            width = 175f
            setAlignment(Align.center)
        }

        //val mapPreview = MapPreview()
        val mapsTable = VisTable()
        val mapsScrollingPane = VisScrollPane(mapsTable)

        private fun myMapNameExists(name: String): Boolean {
            for (map in mapItems[CUSTOM] ?: emptyList()) {
                if (map.mapSave.name == name)
                    return true
            }

            return false
        }

        private val saveButton = newTextButton("Save").addClickListener {
            val saver = getSaver("Save") ?: return@addClickListener

            val toSave = {
                saver.saveMap(canvas.toMapSave(mapNameField.text).also {
                    it.extra["game_mode"] = "crystals" //TODO extras edit
//                it.extra["credits_per_base"] = 100 //TODO extras edit
//                it.extra["credits_per_cluster"] = 100 //TODO extras edit

                }, override = true)

                mapName = mapNameField.text
                canvas.contentChanged = false

                updateMapsListTable()

                addNotifyWindow("Successfully saved", "Save Map")
            }

            if (myMapNameExists(mapNameField.text)) {
                addNotifyWindow("File with such name already exists.\n" + "Rewrite?", "Save Map", toSave, true)
                return@addClickListener
            }

            toSave()
        }

        private fun loadButton(mapItem: MapItem) = newImageButton("edit").addClickListener {
            val mapSave = fromGson(mapItem.fileHandle.readString(), MapSave::class.java)

            if (mapSave.isOutdatedVersion) {
                addNotifyWindow("Outdated map", "Load Map")
                return@addClickListener
            }

            val load = {
                canvas.clearCanvasActors()
                mapSave.layers.forEach { layerSave ->
                    layerSave.actors.forEach { tileSave ->
                        canvas.addActor(
                            actorsPackage.get(tileSave.id).copy().apply {
                                layer = layerSave.id
                                x = tileSave.x * tileSize
                                y = tileSave.y * tileSize
                            },
                            layerSave.id
                        )
                    }
                }

                mapNameField.text = mapSave.name

                mapName = mapNameField.text
                canvas.contentChanged = false
            }

            if (canvas.contentChanged)
                addNotifyWindow("You have unsaved changes, load anyway?", "Load Map", load, true)
            else
                load()
        }

        private fun deleteButton(mapItem: MapItem) = newImageButton("remove").addChangeListener {
            addNotifyWindow("Are you sure?", "Delete Map", {
                if (mapItem.fileHandle.delete()) {

                    updateMapsListTable()


                    addNotifyWindow("Done.", "Delete Map")
                } else
                    addNotifyWindow("File was not deleted", "Delete Map")
            }, true)
        }

        private fun getSaver(operationName: String): Saver? {
            val mapFileName = mapNameField.text
            if (mapFileName.isEmpty()
                || mapFileName.contains("/")
                || mapFileName.contains("\\")
            ) {
                addNotifyWindow("File name should not be empty or contain / \\", "$operationName Map")
                return null
            }

            val saver = Saver()
            if (operationName != "Save" && !saver.exists()) {
                addNotifyWindow("File does not exist", "$operationName Map")
                return null
            }

            return saver
        }

        init {
            closeOnEscape()
            addCloseButton()
            setSize(400f, 700f)

            mapsScrollingPane.setScrollingDisabled(true, false)
            //mapsScrollingPane.setSize(300f, 250f)
            mapsScrollingPane.addListener(object : InputListener() {
                override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                    super.enter(event, x, y, pointer, fromActor)

                    scrollFocus = mapsScrollingPane
                }

                override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                    super.exit(event, x, y, pointer, toActor)

                    scrollFocus = null
                }
            })

            //  add(mapPreview).size(300f)
            //        row()
            add(mapNameField).width(300f)
            row()
            add(VisTable().apply {
                add(saveButton)
            })
            row()
            add(mapsScrollingPane).expandX().fillX().expandY()

            updateMapsListTable()
        }

        fun updateMapsListTable() {
            mapsTable.clear()

            fun VisTable.newMapButton(mapItem: MapItem) {
                add(VisTable().apply {
                    add(deleteButton(mapItem).also {
                        it.isDisabled = mapItem.type == DEFAULT
                    })
                    add(
                        newTextButton(
                            if (mapItem.mapSave.name.isNotEmpty()) mapItem.mapSave.name else mapItem.fileHandle.nameWithoutExtension(),
                            "normal"
                        ).apply {
                            isDisabled = mapItem.mapSave.isOutdatedVersion
                            addChangeListener {
                                //mapPreview.setPreview(createMapFromJson(mapItem.fileHandle.readString()))
                            }
                        }
                    ).width(260f)
                    add(loadButton(mapItem))
                })
                row()
            }

            mapItems = getMapItemsList().groupBy { it.type }
            val maps = getMapItemsList().groupBy { it.type }

            mapsTable.add(VisLabel("- My Maps -")).row()
            for (map in maps[CUSTOM] ?: emptyList()) {
                mapsTable.newMapButton(map)
            }

            mapsTable.add(VisLabel("- Shared -")).row()
            for (map in maps[SHARED] ?: emptyList()) {
                mapsTable.newMapButton(map)
            }

            mapsTable.add(VisLabel("- Default -")).row()
            for (map in maps[DEFAULT] ?: emptyList()) {
                mapsTable.newMapButton(map)
            }
        }

        private fun getMapItemsList(): ArrayList<MapItem> {
            val defMaps = Gdx.files.internal("maps/default")
            val customMaps = Gdx.files.local("maps/custom")
            val sharedMaps = Gdx.files.local("maps/shared")

            val maps = ArrayList<MapItem>()
            for (path in defMaps.list()) {
                if (path.isDirectory) continue
                maps.add(MapItem(path, DEFAULT))
            }
            for (path in customMaps.list()) {
                if (path.isDirectory) continue
                maps.add(MapItem(path, CUSTOM))
            }
            for (path in sharedMaps.list()) {
                if (path.isDirectory) continue
                maps.add(MapItem(path, SHARED))
            }

            return maps
        }

        inner class MapItem(
            val fileHandle: FileHandle,
            val type: String,
        ) {
            val mapSave = fromGson(fileHandle.readString(), MapSave::class.java)
        }
    }
}