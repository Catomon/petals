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
import ctmn.petals.utils.addClickListener
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.addClickSound
import ctmn.petals.widgets.addFocusBorder

class InterfaceStage(val editorScreen: EditorScreen, val actorsPackage: CanvasActorsPackage, batch: Batch) :
    Stage(ScreenViewport(), batch) {

    val table get() = root as VisTable

    val scrollPane = CanvasActorsPicker()
    val scrollPanePlaceholder = Actor().apply { name = "scrollPanePlaceholder" }
    val srollPaneCloseButton = newTextButton(">").addClickListener {
        if (scrollPane.stage != null) {
            table.getCell(scrollPane).setActor(scrollPanePlaceholder)

            (it.listenerActor as VisTextButton).setText("<")
//                (it.listenerActor as VisImageButton).style = VisUI.getSkin().get("open", VisImageButton.VisImageButtonStyle::class.java)
        } else {
            table.getCell(scrollPanePlaceholder).setActor(scrollPane)

            (it.listenerActor as VisTextButton).setText(">")
//                (it.listenerActor as VisImageButton).style = VisUI.getSkin().get("close", VisImageButton.VisImageButtonStyle::class.java)
        }
    }

    val toolButtons = ButtonGroup<VisImageButton>()

    val layerButton: VisTextButton =
        VisTextButton("${Tool.Pencil.layer} +", "layers").apply {
            addClickListener { _ ->
                Tool.Pencil.layer++
                setText("${Tool.Pencil.layer} +")
                layerButtonMin.setText("${Tool.Pencil.layer} -")

                updateLayersVisibility(layerVisibilityButton.text.toString())
            }
        }

    val layerButtonMin = VisTextButton("${Tool.Pencil.layer} -", "layers").apply {
        addClickListener {
            Tool.Pencil.layer--
            setText("${Tool.Pencil.layer} -")
            layerButton.setText("${Tool.Pencil.layer} +")

            updateLayersVisibility(layerVisibilityButton.text.toString())
        }
    }

    val layerVisibilityButton: VisTextButton = VisTextButton("All", "layers").apply {
        addClickListener { _ ->
            setText(if (text.contentEquals("All")) "Current" else "All")
            updateLayersVisibility(text.toString())
        }
    }

    private fun updateLayersVisibility(show: String) {
        if (show == "All") {
            editorScreen.canvas.changeLayersVisible()
        } else {
            editorScreen.canvas.changeLayersVisible(Tool.Pencil.layer)
        }
    }

    fun newTextButton(text: String): VisTextButton {
        return VisTextButton(text).addClickSound().addFocusBorder()
    }

    fun newToolButton(name: String, tool: Tool): VisImageButton {
        return VisImageButton("tool_$name").apply {
            addClickSound().addFocusBorder().addClickListener { _ ->
                Tool.current = tool

                toolButtons.buttons.forEach { it.color.a = 0.5f }
                color.a = 1f
            }.also { toolButtons.add(this) }
        }
    }

    init {
        root = VisTable()

        //table.debug = true

        setUpCanvasActorsScrollPane()

        table.add(scrollPane).expandY()
        table.add(GridGroup(srollPaneCloseButton.width).apply {
            addActor(srollPaneCloseButton)
            addActor(newToolButton("pencil", Tool.Pencil))
            addActor(newToolButton("eraser", Tool.Eraser))
            addActor(newToolButton("drag_canvas", Tool.DragCanvas))
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
        scrollPane.addListener(object : InputListener() {
            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                return super.mouseMoved(event, x, y)
            }

            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                super.enter(event, x, y, pointer, fromActor)

                scrollFocus = scrollPane
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
            Keys.NUM_1 -> Tool.current = Tool.Pencil
            Keys.NUM_2 -> Tool.current = Tool.Eraser
            Keys.NUM_3 -> Tool.current = Tool.Select
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
                    Tool.Pencil.canvasActor = value.userObject as CanvasActor
            }

        private val selectedFrame = VisImage("selected_item_frame")

        init {
            with(this.actor as VisTable) {
                background("background")
                width = 160f

                val maxInRow = 5
                var currentInRow = 0
                for (canvasActor in actorsPackage.canvasActors) {
                    val item = Item(canvasActor)
                    item.name = canvasActor.name
                    item.userObject = canvasActor

                    add(item).size(32f, 32f).pad(4f)
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