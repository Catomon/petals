package ctmn.petals.editor

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.FocusManager
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import ctmn.petals.Const
import ctmn.petals.game
import ctmn.petals.screens.MenuScreen
import ctmn.petals.utils.addClickListener
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.addClickSound
import ctmn.petals.widgets.addFocusBorder
import ctmn.petals.widgets.addNotifyWindow
import kotlin.random.Random

class InterfaceStage(
    private val canvas: CanvasStage,
    private val actorsPackage: CanvasActorsPackage,
    private val tools: Tools,
    batch: Batch,
) :
    Stage(ScreenViewport(), batch) {

    val mainTable get() = root as VisTable

    val actorsPicker = CanvasActorsPicker()
    private val scrollPanePlaceholder = Actor().apply { name = "scrollPanePlaceholder" }
    private val srollPaneCloseButton = newTextButton(">").addClickListener {
        if (actorsPicker.stage != null) {
            mainTable.getCell(actorsPicker).setActor(scrollPanePlaceholder).minWidth(0f)
            (it.listenerActor as VisTextButton).setText("<")
        } else {
            mainTable.getCell(scrollPanePlaceholder).setActor(actorsPicker).minWidth(actorsPicker.prefWidth)
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

    private val mapFileNameTextFieldMaxWidth = 360f
    private val mapFileNameTextFieldMinWidth = 200f
    private val mapFileNameTextField = VisTextField("new_map${Random.nextInt(1000, 9999)}").apply {
        maxLength = 32
        width = 100f
        setAlignment(Align.center)
        /** See [touchDown] */
    }

    private val saveButton = newTextButton("Save").addClickListener {
        val saver = getSaver("Save") ?: return@addClickListener

        val toSave = {
            saver.saveMap(canvas.toMapSave().also {
                it.extra["game_mode"] = "crystals" //TODO extras edit
//                it.extra["credits_per_base"] = 100 //TODO extras edit
//                it.extra["credits_per_cluster"] = 100 //TODO extras edit
            })
            addNotifyWindow("Successfully saved", "Save Map")
        }

        if (saver.exists()) {
            addNotifyWindow("File with such name already exists.\n" + "Rewrite?", "Save Map", toSave, true)
            return@addClickListener
        }

        toSave()
    }

    private val loadButton = newTextButton("Load").addClickListener {
        val saver = getSaver("Load") ?: return@addClickListener

        val mapSave = saver.loadMap()
        canvas.getCanvasActors().forEach { it.remove() }
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
    }

    private val deleteButton = newTextButton("Delete").addClickListener {
        val saver = getSaver("Delete") ?: return@addClickListener

        addNotifyWindow("Are you sure?", "Delete Map", {
            if (saver.deleteMap())
                addNotifyWindow("Successfully deleted", "Delete Map")
            else
                addNotifyWindow("File was not deleted", "Delete Map")
        }, true)
    }

    private fun getSaver(operationName: String): Saver? {
        val mapFileName = mapFileNameTextField.text //TODO replace spaces with _ and so on
        if (mapFileName.isEmpty()
            || mapFileName.contains("/")
            || mapFileName.contains("\\")
        ) {
            addNotifyWindow("File name should not be empty or contain / \\", "$operationName Map")
            return null
        }

        val saver = Saver(mapFileName)
        if (operationName != "Save" && !saver.exists()) {
            addNotifyWindow("File does not exist", "$operationName Map")
            return null
        }

        return saver
    }

    private val exitButton = newTextButton("X").addClickListener {
        game.screen = MenuScreen()
    }

    init {
        root = VisTable()

        //table.debug = true

        //setup table
        mainTable.add(actorsPicker).expandY().minWidth(actorsPicker.prefWidth)
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
            add(mapFileNameTextField).prefWidth(mapFileNameTextFieldMinWidth)
            add(saveButton)
            add(loadButton)
            add(deleteButton)
            add(exitButton)
        }).align(Align.topRight)

        //default
        changeTool(tools.pencil)
        changeLayer(1)
    }

    private fun newTextButton(text: String, styleName: String = "default"): VisTextButton {
        return VisTextButton(text, styleName).addClickSound().addFocusBorder()
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
        if (mapFileNameTextField.hasKeyboardFocus())
            FocusManager.resetFocus(this)

        return super.touchDown(screenX, screenY, pointer, button).also {
            with (mapFileNameTextField) {
                if (this.hasKeyboardFocus()) {
                    (parent as VisTable).getCell(this).prefWidth(mapFileNameTextFieldMaxWidth)
                    (parent as VisTable).invalidate()
                    mainTable.invalidate()
                } else {
                    (parent as VisTable).getCell(this).prefWidth(mapFileNameTextFieldMinWidth)
                    (parent as VisTable).invalidate()
                    mainTable.invalidate()
                }
            }
        }
    }

    fun onScreenResize(width: Int, height: Int) {
        viewport.update(width, height, true)
        mainTable.setSize(viewport.worldWidth, viewport.worldHeight)
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
        private val itemsInRow = if (Const.IS_MOBILE) 3 else 5
        private val itemSize = tableWidth / itemsInRow //32f

        private val selectedFrame = VisImage("selected_item_frame").apply {
            setSize(48f / 32f * itemSize, 48f / 32f * itemSize)
        }

        init {
            with(this.actor as VisTable) {
                background("background")
                width = tableWidth

                val maxInRow = itemsInRow
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

        inner class Item(canvasActor: CanvasActor) : VisImage(SpriteDrawable(Sprite(canvasActor.sprite))) {
            var selectedFrame: VisImage? = null

            override fun draw(batch: Batch?, parentAlpha: Float) {
                selectedFrame?.setPosByCenter(centerX, centerY)
                selectedFrame?.draw(batch, parentAlpha)

                super.draw(batch, parentAlpha)
            }
        }
    }
}