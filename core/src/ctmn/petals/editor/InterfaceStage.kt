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
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import ctmn.petals.Const
import ctmn.petals.utils.addClickListener
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.addClickSound
import ctmn.petals.widgets.addFocusBorder

class InterfaceStage(
    private val editorScreen: EditorScreen,
    private val actorsPackage: CanvasActorsPackage,
    private val tools: Tools,
    batch: Batch,
) :
    Stage(ScreenViewport(), batch) {

    val table get() = root as VisTable

    val actorsPicker = CanvasActorsPicker()
    private val scrollPanePlaceholder = Actor().apply { name = "scrollPanePlaceholder" }
    private val srollPaneCloseButton = newTextButton(">").addClickListener {
        if (actorsPicker.stage != null) {
            table.getCell(actorsPicker).setActor(scrollPanePlaceholder)
            (it.listenerActor as VisTextButton).setText("<")
        } else {
            table.getCell(scrollPanePlaceholder).setActor(actorsPicker)
            (it.listenerActor as VisTextButton).setText(">")
        }
    }

    private val toolButtons = ButtonGroup<VisImageButton>()

    private val layerButton = newTextButton("${tools.pencil.layer} +", "layers")
        .addClickListener { _ ->
            changeCanvasLayer(tools.pencil.layer + 1)
        }

    private val layerButtonMin = newTextButton("${tools.pencil.layer} -", "layers")
        .addClickListener {
            changeCanvasLayer(tools.pencil.layer - 1)
        }

    private val layerVisibilityAll = "All"
    private val layerVisibilityCurrent = "Current"

    private val layerVisibilityButton: VisTextButton = newTextButton(layerVisibilityAll, "layers").apply {
        addClickListener { _ ->
            setText(if (text.contentEquals(layerVisibilityAll)) layerVisibilityCurrent else layerVisibilityAll)
            changeCanvasLayer(tools.pencil.layer)
        }
    }

    private fun changeCanvasLayer(layer: Int) {
        tools.pencil.layer = layer
        layerButton.setText("${tools.pencil.layer} +")
        layerButtonMin.setText("${tools.pencil.layer} -")

        val show = layerVisibilityButton.text.toString()
        if (show == layerVisibilityAll) {
            editorScreen.canvas.changeLayersVisible()
        } else {
            editorScreen.canvas.changeLayersVisible(tools.pencil.layer)
        }
    }

    fun newTextButton(text: String, styleName: String = "default"): VisTextButton {
        return VisTextButton(text, styleName).addClickSound().addFocusBorder()
    }

    fun newToolButton(tool: Tool): VisImageButton {
        return VisImageButton("tool_${tool.name}").apply {
            addClickSound().addFocusBorder().addClickListener { _ ->
                tools.current = tool

                toolButtons.buttons.forEach { it.color.a = 0.5f }
                color.a = 1f
            }.also { toolButtons.add(this) }
        }
    }

    init {
        root = VisTable()

        //table.debug = true

        setUpCanvasActorsScrollPane()

        table.add(actorsPicker).expandY()
        table.add(GridGroup(srollPaneCloseButton.width).apply {
            addActor(srollPaneCloseButton)
            for (tool in tools.toolList) {
                addActor(newToolButton(tool))
            }
            addActor(layerButton)
            addActor(layerButtonMin)
            addActor(layerVisibilityButton)
        }).align(Align.top)
        table.add().expandX()
        table.add()

        toolButtons.buttons.forEach { it.color.a = 0.5f }
        toolButtons.buttons.first().color.a = 1f
    }

    private fun setUpCanvasActorsScrollPane() {
        actorsPicker.addListener(object : InputListener() {
            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                return super.mouseMoved(event, x, y)
            }

            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                super.enter(event, x, y, pointer, fromActor)

                scrollFocus = actorsPicker
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                super.exit(event, x, y, pointer, toActor)

                scrollFocus = null
            }
        })
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (scrollFocus == null) {
            (editorScreen.canvas.viewport.camera as OrthographicCamera).zoom += amountY * 0.1f

            return true
        }

        return super.scrolled(amountX, amountY)
    }

    override fun keyDown(keyCode: Int): Boolean {
        when (keyCode) {
            Keys.NUM_1 -> tools.current = tools.pencil
            Keys.NUM_2 -> tools.current = tools.eraser
            Keys.NUM_3 -> tools.current = tools.dragCanvas
        }

        return false
    }

    fun onScreenResize(width: Int, height: Int) {
        viewport.update(width, height, true)
        table.setSize(viewport.worldWidth, viewport.worldHeight)
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
                override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                    super.exit(event, x, y, pointer, toActor)

                    if (hovered != selected)
                        hovered?.selectedFrame = null
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