package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.tile.TileActor
import ctmn.petals.playscreen.commands.BuyUnitCommand
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.unit.*
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.assets
import ctmn.petals.player.Player
import ctmn.petals.player.getSpeciesUnits
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newImageButton
import ctmn.petals.widgets.newLabel

class BuyMenuPanel(
    private val guiStage: PlayGUIStage,
    var selectedCastleTile: TileActor? = null,
    val player: Player? = null,
) : VisWindow("Buy Menu") {

    companion object {
        private const val ICON_SIZE = 64f
    }

    private val gridGroup = GridGroup(ICON_SIZE, 32f)
    private val scrollPane = VisScrollPane(gridGroup)

    var unitLeaderId = -1

    private val captureListener = object : EventListener {
        override fun handle(event: Event?): Boolean {
            if (isVisible && event is InputEvent) {

                if (event.target.isDescendantOf(this@BuyMenuPanel)) {
                    if (event.type == InputEvent.Type.touchDragged)
                        event.stop()
                } else {
                    if (event.type == InputEvent.Type.touchDown) {
                        event.stop()
                        fadeOut()
                    }
                }

                return true
            }

            return false
        }
    }

    init {
        //window
        FADE_TIME = 0f
        isMovable = false
        isResizable = false
        setSize(250f, 400f)
        setCenterOnAdd(true)

        //grid group
        val unitsData = guiStage.playScreen.unitsData

        fun addB(unitActor: UnitActor, cost: Int) {
            gridGroup.addButton(UnitButton(unitActor, cost))
        }

        if (player == null) {
            //add all units with ShopComponent
            for (name in unitsData.names) {
                val newUnit = unitsData.get(name)
                if (newUnit.cShop != null)
                    addB(newUnit, newUnit.cShop?.price ?: 999999)
            }
        } else {
            //add species units
            val units = getSpeciesUnits(player.species)
            for (unit in units) {
                if (unit.cShop != null)
                    addB(unit, unit.cShop?.price ?: 999999)
            }
        }


        //scroll pane
        //scrollPane.setScrollingDisabled(true, false)
        add(scrollPane).size(340f, 360f).padLeft(18f)
        row()
        add(newImageButton("cancel").addChangeListener { fadeOut() }).padTop(24f)
    }

    private fun GridGroup.addButton(unitButton: UnitButton) {
        addActor(unitButton)
        unitButton.button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)

                if (guiStage.currentState == guiStage.myTurn && selectedCastleTile != null) {
                    // give the unit to the first leader that comes to the unit if there are no leaders by default
                    var unitLeaderIdLoc = unitLeaderId
                    if (unitLeaderIdLoc == -1) {
                        for (unit in guiStage.playStage.getUnitsOfPlayer(guiStage.player)) {
                            if (unit.isLeader)
                                unitLeaderIdLoc = unit.leaderID
                        }
                    }

                    val buyCommand = BuyUnitCommand(
                        unitButton.unit.selfName,
                        guiStage.player,
                        unitButton.cost,
                        selectedCastleTile!!.tiledX,
                        selectedCastleTile!!.tiledY,
                        unitLeaderIdLoc
                    )

                    if (buyCommand.canExecute(guiStage.playScreen))
                        guiStage.playScreen.commandManager.queueCommand(buyCommand)
                }

                fadeOut()
            }
        })
    }

    override fun setStage(stage: Stage?) {
        if (stage != null) {
            fadeIn()

            stage.addCaptureListener(captureListener)
        } else
            this.stage?.removeCaptureListener(captureListener)

        super.setStage(stage)
    }

    private class UnitButton(val unit: UnitActor, val cost: Int) : WidgetGroup() {

        val button = newImageButton("buy_unit")
        private val labelCost = newLabel(cost.toString())

        init {
            if (assets.textureAtlas.findRegion("gui/icons/${unit.selfName}") != null)
                button.style.imageUp = VisUI.getSkin().getDrawable("icons/${unit.selfName}")
            else
                button.style.imageUp = VisUI.getSkin().getDrawable("icons/no_icon")

            labelCost.setPosByCenter(32f + labelCost.width / 2, 84f)

            addActor(button)
            addActor(labelCost)

            setScale(1f)
        }
    }
}
