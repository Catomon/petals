package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.*
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
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.triggers.OnActionCompleteTrigger
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.*
import ctmn.petals.widgets.*

public infix fun <A, B> A.said(that: B): Pair<A, B> = Pair(this, that)

class StoryDialog(
    val quotes: Array<Quote>,
) : VisTable() {

    private lateinit var guiStage: PlayGUIStage

    constructor(vararg dialogs: Pair<String, UnitActor?>) : this(
        Array<Quote>().apply { dialogs.forEach { add(Quote(it.first, it.second)) } })

    constructor(unit: UnitActor, vararg quotes: Quote) : this(
        Array(quotes.onEach { if (it.source == null) it.source = unit })
    )

    constructor(vararg quotes: Quote) : this(Array(quotes))

    constructor(text: String, unit: UnitActor? = null) : this(Array(arrayOf(Quote(text, unit))))

    constructor(text: String) : this(Array(arrayOf(Quote(text))))

    private val label = newLabel("", "quote")
    private val quoteTail = VisImage("unit_dialog_tail")

    private var quoteIndex = 0
    val currentQuote get() = if (quoteIndex >= quotes.size) null else quotes[quoteIndex]

    var cameraMove = true

    var addOkayButton = false
    private val okayButton = newIconButton("confirm").addChangeListener {
        guiStage.nextDialogButton.press()
    }

    fun doNotMoveCamera(): StoryDialog {
        cameraMove = false

        return this
    }

    companion object {
        const val PC_ENTER_BUTTON_NAME = "enter_button"
    }

    private val enterButton = newIconButton("enter").apply {
        name = PC_ENTER_BUTTON_NAME
        addChangeListener {
            guiStage.nextDialogButton.press()
        }
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

        val currentQuote = if (currentQuote == null) return else currentQuote

        if (Const.IS_DESKTOP)
            enterButton.setPosition(width - enterButton.width - 3, 5f)

        if (currentQuote?.source == null)
            setPosition(guiStage.camera.position.x - width / 2, guiStage.camera.position.y - height / 2)
        else
            setPositionFromSource(currentQuote.source!!)
    }

    fun nextQuote() {
        //remove talking animation
        currentQuote?.source?.let { source ->
            if (source is UnitActor) {
                source.setAnimation(null)
            }
        }

        quoteIndex++
        if (quoteIndex >= quotes.size) {
            remove()
            isVisible = false
            quoteTail.isVisible = false
        } else {
            currentQuote?.let { quote ->
                quote.source?.let { if (it.stage == null) nextQuote() }

                updateDialog(quote)
            }
        }
    }

    //update dialog and unit dialog icon
    private fun updateDialog(quote: Quote) {
        if (quote.source == null) {
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

        val sourceActor = quote.source

        if (sourceActor != null) {
            setPositionFromSource(sourceActor)

            if (cameraMove) {
                isVisible = false
                quoteTail.isVisible = false

                val cameraMoveAction = CameraMoveAction(sourceActor.centerX, sourceActor.centerY)
                guiStage.playScreen.actionManager.addAction(cameraMoveAction)
                guiStage.playScreen.triggerManager.addTrigger(OnActionCompleteTrigger(cameraMoveAction).onTrigger {

                    isVisible = true
                    quoteTail.isVisible = true
                })
            }

            //set talking animation to unit
            if (sourceActor is UnitActor)
                sourceActor.setAnimation(sourceActor.talkingAnimation, TALKING_ANIMATION_DURATION)
        } else
            quoteTail.isVisible = false
    }

    private val tmpVec = Vector2()
    private fun setPositionFromSource(sourceActor: Actor) {
        val sourceWidth = if (sourceActor is UnitActor) 16f else sourceActor.width
        val sourceHeight = if (sourceActor is UnitActor) 16f else sourceActor.height
        val sourceStage: Stage = sourceActor.stage ?: let {
            logErr("Dialog source stage is null")
            return
        }
        tmpVec.set(sourceActor.centerX, sourceActor.centerY)
        val sPos = sourceActor.localToStageCoordinates(tmpVec)
        val (sX, sY) = sPos.x to sPos.y
        val center = guiStage.screenToStageCoordinates(sourceStage.stageToScreenCoordinates(Vector2(sX, sY)))
        val top = guiStage.screenToStageCoordinates(
            sourceStage.stageToScreenCoordinates(
                Vector2(
                    sX,
                    sY + sourceHeight / 2
                )
            )
        )
        val bottom = guiStage.screenToStageCoordinates(
            sourceStage.stageToScreenCoordinates(
                Vector2(
                    sX,
                    sY - sourceHeight / 2
                )
            )
        )
        val left = guiStage.screenToStageCoordinates(
            sourceStage.stageToScreenCoordinates(
                Vector2(
                    sX - sourceWidth / 2,
                    sY
                )
            )
        )
        val right = guiStage.screenToStageCoordinates(
            sourceStage.stageToScreenCoordinates(
                Vector2(
                    sX + sourceWidth / 2,
                    sY
                )
            )
        )

        val camera = guiStage.camera
        val topFit = center.y < camera.viewportHeight / 2
                && center.y > 0f
                && center.x > 0f
                && center.x < camera.viewportWidth
                && camera.viewportHeight - top.x > height

        val bottomFit = center.y > camera.viewportHeight / 2
                && center.y < camera.viewportHeight
                && center.x > 0f
                && center.x < camera.viewportWidth
                && camera.viewportHeight - (camera.viewportHeight - bottom.x) > height

        val leftFit = center.x > camera.viewportWidth / 2
                && center.x < camera.viewportWidth
                && center.y < camera.viewportHeight
                && center.y > height
                && camera.viewportWidth - (camera.viewportWidth - left.x) > width

        val rightFit = center.x < camera.viewportWidth / 2
                && center.x > 0f
                && center.y < camera.viewportHeight
                && center.y > height
                && camera.viewportWidth - right.x > width

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

        if (topLeftCorner.y > camera.viewportHeight)
            y = camera.viewportHeight - height
        if (topLeftCorner.x < 0f)
            x = 0f
        if (bottomRightCorner.y < 0f)
            y = 0f
        if (bottomRightCorner.x > camera.viewportWidth)
            x = camera.viewportWidth - width
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

    class Quote(var text: String, var source: Actor? = null)

    class NextDialogButton(val guiStage: PlayGUIStage) : VisImageButton("next_dialog") {

        private var dialogs = Queue<StoryDialog>()

        fun press() {
            val oldQuote = dialogs.last().currentQuote
            dialogs.last().nextQuote()

            if (oldQuote != null) {
                guiStage.root.fire(QuoteEndedEvent(oldQuote))
            }

            if (dialogs.last().currentQuote == null) {
                dialogs.removeLast()
            } else {
                guiStage.root.fire(QuoteStartedEvent(dialogs.last().currentQuote!!))
            }

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

        fun hasDialogs() = !dialogs.isEmpty

        fun addDialog(dialog: StoryDialog) {
            isVisible = true

            val table = guiStage.endTurnButton.parent as VisTable

            table.getCell(guiStage.endTurnButton).setActor(this)

            dialogs.addFirst(dialog)

            if (dialog.currentQuote != null) {
                guiStage.root.fire(QuoteStartedEvent(dialog.currentQuote!!))
            }
        }
    }

    class QuoteStartedEvent(val quote: Quote) : Event()
    class QuoteEndedEvent(val quote: Quote) : Event()
}
