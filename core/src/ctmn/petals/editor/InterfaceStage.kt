package ctmn.petals.editor

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import ctmn.petals.utils.addClickListener
import ctmn.petals.utils.addClickSound
import ctmn.petals.widgets.addClickSound
import ctmn.petals.widgets.addFocusBorder

class InterfaceStage(val editorScreen: EditorScreen, val ActorsPackage: CanvasActorsPackage, batch: Batch) :
    Stage(ScreenViewport(), batch) {

    val table get() = root as VisTable

    val drawingsTable = VisTable()
    val scrollPane = VisScrollPane(drawingsTable)
    val scrollPanePlaceholder = Actor().apply { name = "scrollPanePlaceholder" }

    val closeScrollPaneButton = VisTextButton(">").addClickSound().addFocusBorder().addClickListener {
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

    val toolButtons = ButtonGroup<VisTextButton>()

    val pencilButton = VisTextButton("P").addClickListener {
        Tool.current = Tool.Pencil

        toolButtons.buttons.forEach { it.color.a = 0.5f }
        (it.listenerActor as VisTextButton).color.a = 1f
    }
    val eraserButton = VisTextButton("E").addClickListener {
        Tool.current = Tool.Eraser

        toolButtons.buttons.forEach { it.color.a = 0.5f }
        (it.listenerActor as VisTextButton).color.a = 1f
    }

    init {
        root = VisTable()

        //table.debug = true

        setUpCanvasActorsScrollPane()

        toolButtons.add(pencilButton, eraserButton)

        table.add(scrollPane).expandY()
        table.add(GridGroup(closeScrollPaneButton.width).apply {
            addActor(closeScrollPaneButton)
            addActor(pencilButton)
            addActor(eraserButton)
        }).align(Align.top)
        table.add().expandX()
        table.add()
    }

    private fun setUpCanvasActorsScrollPane() {
        scrollPane.addListener(object : InputListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                super.enter(event, x, y, pointer, fromActor)

                scrollFocus = scrollPane
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                super.exit(event, x, y, pointer, toActor)

                scrollFocus = null
            }
        })

        drawingsTable.background("background")
        drawingsTable.width = 160f

        val maxInRow = 5
        var currentInRow = 0
        for (canvasActor in ActorsPackage.canvasActors) {
            val item = VisImage(SpriteDrawable(Sprite(canvasActor.sprite)))
            item.name = canvasActor.name
            item.userObject = canvasActor
            item.addClickListener {
                Tool.Pencil.canvasActor = item.userObject as CanvasActor
            }

            drawingsTable.add(item).size(32f, 32f).pad(4f)
            currentInRow++

            if (currentInRow >= maxInRow) {
                drawingsTable.row()
                currentInRow = 0
            }
        }
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
}