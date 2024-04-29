package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const
import ctmn.petals.widgets.newIconButton
import ctmn.petals.playscreen.commands.CaptureCommand
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.*
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.utils.cornerX
import ctmn.petals.utils.cornerY
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import ctmn.petals.tile.cPlayerId
import ctmn.petals.unit.UnitActor
import java.lang.IllegalStateException

class UnitMiniMenu(val guiStage: PlayGUIStage) : VerticalGroup() {

    private val captureButton = newIconButton("umm_capture")
    private val moveButton = newIconButton("umm_move")
    private val attackButton = newIconButton("umm_attack")
    private val abilityButton = newIconButton("umm_ability")
    private val cancelButton = newIconButton("umm_cancel")
    private val closeButton = newIconButton("umm_close")

    private var itemWidth: Float = 0f

    var unit: UnitActor? = null; set(value) {
        field = value
        setupForUnit(value)

        if (value == null) {
            hide()
        } else {
            show()
        }
    }

    init {
        isVisible = false

        fun size(actor: Actor) {
//            if (Gdx.app.type == Application.ApplicationType.Android)
//                actor.setSize(actor.width * 2f, actor.height * 2f)
//            else actor.setSize(actor.width * 2f, actor.height * 2f)

            if (actor.width > itemWidth)
                itemWidth = actor.width
        }

        size(captureButton)
        size(moveButton)
        size(attackButton)
        size(abilityButton)
        size(cancelButton)
        size(closeButton)

        captureButton.addChangeListener {
            if (unit != null)
                guiStage.playScreen.commandManager.queueCommand(
                    CaptureCommand(unit!!, guiStage.playStage.getTile(unit!!.tiledX, unit!!.tiledY)!!))
            hide()
        }

        moveButton.addChangeListener {
            //guiStage.mapClickListener = guiStage.moveCs
            hide()
        }

        attackButton.addChangeListener {
            //guiStage.mapClickListener = guiStage.attackCs
            hide()
        }

        abilityButton.addChangeListener {
//            guiStage.mapClickListener = PlayGUIStage.MapClickListener.USE_ABILITY
            guiStage.abilitiesPanel.isVisible = true
        }

        closeButton.addChangeListener {
            unit = null
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        if (isVisible && !guiStage.playScreen.actionManager.isQueueEmpty) {
            hide()
        } else {
            if (!isVisible)
            if (unit != null && guiStage.playScreen.actionManager.isQueueEmpty) {
                if (guiStage.mapClickListener == guiStage.unitSelectedCL) {
                    unit = unit
                    show()
                }
            }
        }
    }

    /** Добавляет нужные кнопки и ставит группу на позицию [unit] */
    private fun setupForUnit(unit: UnitActor?) {
        if (stage == null) throw IllegalStateException("")

        if (unit == null)
            return

        clear()

        //add buttons
        val tile = guiStage.playStage.getTile(unit.tiledX, unit.tiledY)!!
        if (tile.tileName.contains("base") && guiStage.playScreen.turnManager.getPlayerById(tile.cPlayerId?.playerId ?: 0)?.teamId != unit.teamId) {
            addActor(captureButton)
        }

        if (unit.canMove())
            addActor(moveButton)

        if (unit.canAttack())
            addActor(attackButton)

        if (unit.cAbilities != null)
            addActor(abilityButton)

        addActor(closeButton)

        // set position based on unit position
        val guiCamera = guiStage.camera as OrthographicCamera
        val playCamera = guiStage.playStage.camera as OrthographicCamera

        val difW = (guiCamera.viewportWidth * guiCamera.zoom) / (playCamera.viewportWidth * playCamera.zoom)
        val difH = (guiCamera.viewportHeight * guiCamera.zoom) / (playCamera.viewportHeight * playCamera.zoom)

        val x = (unit.tiledX.unTiled() - playCamera.cornerX()) * difW
        val y = (unit.tiledY.unTiled() - playCamera.cornerY()) * difH

        setPosition(x + itemWidth / 2 + (Const.TILE_SIZE * difW), y + (Const.TILE_SIZE * difW))
    }

    fun hide() {
        //addAction(Actions.sequence(Actions.fadeOut(0.25f), Actions.visible(false)))
        isVisible = false
        guiStage.abilitiesPanel.isVisible = false
    }

    fun show() {
        //addAction(Actions.sequence(Actions.visible(true), Actions.fadeIn(0.25f)))
        isVisible = true
    }

    override fun addActor(actor: Actor) {
        val cont = Container(actor)
        cont.size(actor.width, actor.height)

        super.addActor(cont)
    }
}