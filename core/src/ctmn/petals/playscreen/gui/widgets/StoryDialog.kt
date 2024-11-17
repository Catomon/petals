package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.Const
import ctmn.petals.Const.TALKING_ANIMATION_DURATION
import ctmn.petals.actors.actions.TimeAction
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.triggers.OnActionCompleteTrigger
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.*

public infix fun <A, B> A.said(that: B): Pair<A, B> = Pair(this, that)

class StoryDialog(
    val quotes: Array<Quote>,
) : VisTable() {

    private lateinit var guiStage: PlayGUIStage

    constructor(vararg dialogs: Pair<String, UnitActor?>) : this(
        Array<Quote>().apply { dialogs.forEach { add(Quote(it.first, it.second)) } })

    constructor(unit: UnitActor, vararg quotes: Quote) : this(
        Array(quotes.onEach { if (it.unit == null) it.unit = unit })
    )

    constructor(vararg quotes: Quote) : this(Array(quotes))

    constructor(text: String, unit: UnitActor? = null) : this(Array(arrayOf(Quote(text, unit))))

    constructor(text: String) : this(Array(arrayOf(Quote(text))))

    private val label = newLabel("", "quote")
    private val quoteTail = VisImage("unit_dialog_tail")

    private var quoteIndex = 0
    private val currentQuote get() = if (quoteIndex >= quotes.size) null else quotes[quoteIndex]

    var cameraMove = true

    var addOkayButton = false
    private val okayButton = newIconButton("confirm").addChangeListener {
        guiStage.nextDialogButton.press()
    }

    fun doNotMoveCamera(): StoryDialog {
        cameraMove = false

        return this
    }

    private val enterButton = newIconButton("enter").addChangeListener {
        guiStage.nextDialogButton.press()
    }

    init {
        quoteTail.setOrigin(12f, 12f)

        label.setAlignment(Align.center)

        add(label).expandX()
        //row()
        //add().right()

        if (Const.IS_DESKTOP)
            addActorAt(0, enterButton)
    }

    override fun act(delta: Float) {
        super.act(delta)

        if (currentQuote == null) return

        if (Const.IS_DESKTOP)
            enterButton.setPosition(width - enterButton.width - 3, 5f)

        if (currentQuote!!.unit == null)
            setPosition(guiStage.camera.position.x - width / 2, guiStage.camera.position.y - height / 2)
        else
            setPositionFromSource(currentQuote!!.unit!!.centerX, currentQuote!!.unit!!.centerY)
    }

    fun nextQuote() {
        //remove talking animation
        currentQuote?.unit?.setAnimation(null)

        quoteIndex++
        if (quoteIndex >= quotes.size) {
            remove()
            isVisible = false
            quoteTail.isVisible = false
        } else {
            currentQuote?.let { quote ->
                quote.unit?.let { if (it.playStageOrNull == null) nextQuote() }

                updateDialog(quote)
            }
        }
    }

    //update dialog and unit dialog icon
    private fun updateDialog(quote: Quote) {
        if (quote.unit == null) {
            setBackground("dialog")
            if (addOkayButton)
                add(okayButton).right()
        } else {
            setBackground("quote")

            removeActor(okayButton)
        }

        label.setText(quote.text)

        label.wrap = false
        label.pack()

        if (getCell(label).minWidth > guiStage.camera.viewportWidth - 50) {
            label.wrap = true
            getCell(label).prefWidth(guiStage.camera.viewportWidth - 50)
        }

        getCell(label).maxWidth(guiStage.camera.viewportWidth - 50)

        pack()

        val unit = quote.unit

        if (unit != null) {
            setPositionFromSource(unit.centerX, unit.centerY)

            if (cameraMove) {
                isVisible = false
                quoteTail.isVisible = false

                val cameraMoveAction = CameraMoveAction(unit.centerX, unit.centerY)
                guiStage.playScreen.actionManager.addAction(cameraMoveAction)
                guiStage.playScreen.triggerManager.addTrigger(OnActionCompleteTrigger(cameraMoveAction).onTrigger {

                    isVisible = true
                    quoteTail.isVisible = true
                })
            }

            //set talking animation to unit
            unit.setAnimation(unit.talkingAnimation, TALKING_ANIMATION_DURATION)
        } else
            quoteTail.isVisible = false
    }

    private fun setPositionFromSource(sX: Float, sY: Float, sourceWidth: Float = 16f, sourceHeight: Float = 16f) {
        val center = guiStage.screenToStageCoordinates(guiStage.playStage.stageToScreenCoordinates(Vector2(sX, sY)))
        val top = guiStage.screenToStageCoordinates(
            guiStage.playStage.stageToScreenCoordinates(
                Vector2(
                    sX,
                    sY + sourceHeight / 2
                )
            )
        )
        val bottom = guiStage.screenToStageCoordinates(
            guiStage.playStage.stageToScreenCoordinates(
                Vector2(
                    sX,
                    sY - sourceHeight / 2
                )
            )
        )
        val left = guiStage.screenToStageCoordinates(
            guiStage.playStage.stageToScreenCoordinates(
                Vector2(
                    sX - sourceWidth / 2,
                    sY
                )
            )
        )
        val right = guiStage.screenToStageCoordinates(
            guiStage.playStage.stageToScreenCoordinates(
                Vector2(
                    sX + sourceWidth / 2,
                    sY
                )
            )
        )

        val topFit = center.y < guiStage.camera.viewportHeight / 2
                && center.y > 0f
                && center.x > 0f
                && center.x < guiStage.camera.viewportWidth

        val bottomFit = center.y > guiStage.camera.viewportHeight / 2
                && center.y < guiStage.camera.viewportHeight
                && center.x > 0f
                && center.x < guiStage.camera.viewportWidth

        val leftFit = center.x > guiStage.camera.viewportWidth / 2
                && center.x < guiStage.camera.viewportWidth
                && center.y < guiStage.camera.viewportHeight
                && center.y > height
                && center.y < guiStage.camera.viewportHeight - height
                && guiStage.camera.viewportWidth - (guiStage.camera.viewportWidth - left.x) > width

        val rightFit = center.x < guiStage.camera.viewportWidth / 2
                && center.x > 0f
                && center.y < guiStage.camera.viewportHeight
                && center.y > height
                && center.y < guiStage.camera.viewportHeight - height
                && guiStage.camera.viewportWidth - right.x > width

        val tailOffset = 4
        when {
            leftFit -> {
                setPosition(left.x - width, left.y - height / 2)
                quoteTail.setPosByCenter(x + width + tailOffset, y + height / 2); quoteTail.rotation = 90f
            }

            rightFit -> {
                setPosition(right.x, right.y - height / 2)
                quoteTail.setPosByCenter(x - tailOffset, y + height / 2); quoteTail.rotation = 270f
            }

            topFit -> {
                setPosition(top.x - width / 2, top.y)
                quoteTail.setPosByCenter(x + width / 2, y - tailOffset); quoteTail.rotation = 0f
            }

            bottomFit -> {
                setPosition(bottom.x - width / 2, bottom.y - height)
                quoteTail.setPosByCenter(x + width / 2, y + height + tailOffset); quoteTail.rotation = 180f
            }
        }
    }

    override fun setStage(stage: Stage?) {
        if (stage != null) {
            guiStage = stage as PlayGUIStage

            setPosByCenter(guiStage.camera.viewportWidth / 2, guiStage.camera.viewportHeight / 2)

            guiStage.nextDialogButton.addDialog(this)

            guiStage.addActor(quoteTail)

            currentQuote?.let { updateDialog(it) }
        } else {
            quoteTail.remove()
        }

        super.setStage(stage)
    }

    class Quote(var text: String, var unit: UnitActor? = null)

    class NextDialogButton(val guiStage: PlayGUIStage) : VisImageButton("next_dialog") {

        private var dialogs = Queue<StoryDialog>()

        fun press() {
            dialogs.last().nextQuote()

            if (dialogs.last().currentQuote == null)
                dialogs.removeLast()

            if (dialogs.isEmpty) {
                isVisible = false

                val table = this.parent as VisTable

                guiStage.endTurnButton.isDisabled = true

                table.getCell(this).setActor(guiStage.endTurnButton)

                guiStage.endTurnButton.addAction(TimeAction {
                    guiStage.endTurnButton.isDisabled = true

                    if (it >= 1.5f) {
                        guiStage.endTurnButton.isDisabled = false

                        return@TimeAction true
                    }

                    false
                })
            }
        }

        init {
            isVisible = false

            addChangeListener {
                press()
            }

            addClickSound()
            addFocusBorder()

            guiStage.addListener(object : InputListener() {
                override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                    if (Input.Keys.ENTER == keycode) {
                        if (!isDisabled && isVisible)
                            press()
                    }
                    return super.keyDown(event, keycode)
                }
            })
        }

        fun addDialog(dialog: StoryDialog) {
            isVisible = true

            val table = guiStage.endTurnButton.parent as VisTable

            table.getCell(guiStage.endTurnButton).setActor(this)

            dialogs.addFirst(dialog)
        }
    }
}
