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
            mainTable.getCell(actorsPicker).setActor(scrollPanePlaceholder)
            (it.listenerActor as VisTextButton).setText("<")
        } else {
            mainTable.getCell(scrollPanePlaceholder).setActor(actorsPicker)
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

    private val mapFileNameTextField = VisTextField("new_map${Random.nextInt(1000, 9999)}").apply {
        maxLength = 32
        width = 160f
    }

    private val saveButton = newTextButton("Save").addClickListener {
        val mapFileName = mapFileNameTextField.text
        if (mapFileName.isEmpty()
            || mapFileName.contains("/")
            || mapFileName.contains("\\")
        ) {
            //todo
            return@addClickListener
        }

        val saver = Saver(mapFileName)
        if (saver.exists()) {
            //todo
            return@addClickListener
        }

        saver.saveMap(canvas.asMapSave())
    }

    private val loadButton = newTextButton("Load").addClickListener {
        val mapFileName = mapFileNameTextField.text
        if (mapFileName.isEmpty()
            || mapFileName.contains("/")
            || mapFileName.contains("\\")
        ) {
            //todo
            return@addClickListener
        }

        val saver = Saver(mapFileName)
        if (!saver.exists()) {
            //todo
            return@addClickListener
        }

        val mapSave = saver.loadMap()
        canvas.getCanvasActors().forEach { it.remove() }
        mapSave.actors.forEach {
            canvas.addActor(
                actorsPackage.get(it.id).copy().apply {
                    layer = it.layer
                    x = it.x * tileSize
                    y = it.y * tileSize
                },
                it.layer
            )
        }
    }

    private val exitButton = newTextButton("X").addClickListener {
        game.screen = MenuScreen()
    }

    init {
        root = VisTable()

        //table.debug = true

        //setup table
        mainTable.add(actorsPicker).expandY()
        mainTable.add(GridGroup(srollPaneCloseButton.width).apply {
            addActor(srollPaneCloseButton)
            for (tool in tools.toolList) {
                addActor(newToolButton(tool))
            }
            addActor(layerButton)
            addActor(layerButtonMin)
            addActor(layerVisibilityButton)
        }).align(Align.top)
        mainTable.add().expandX()
        mainTable.add(VisTable().apply {
            add(mapFileNameTextField).minWidth(100f).prefWidth(300f)
            add(saveButton)
            add(loadButton)
            add(exitButton)
        }).align(Align.topRight)

        //default
        changeTool(tools.pencil)
        changeLayer(1)
    }

    fun newTextButton(text: String, styleName: String = "default"): VisTextButton {
        return VisTextButton(text, styleName).addClickSound().addFocusBorder()
    }

    fun newToolButton(tool: Tool): VisImageButton {
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

        return super.touchDown(screenX, screenY, pointer, button)
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

        private val tableWidth = 160f
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