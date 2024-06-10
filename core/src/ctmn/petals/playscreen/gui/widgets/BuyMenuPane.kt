package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.assets
import ctmn.petals.player.Player
import ctmn.petals.player.getSpeciesUnits
import ctmn.petals.playscreen.commands.BuyUnitCommand
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isWaterBase
import ctmn.petals.unit.*
import ctmn.petals.utils.addClickListener
import ctmn.petals.utils.removeCover
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.StageCover
import ctmn.petals.widgets.newIconButton
import ctmn.petals.widgets.newLabel

class BuyMenu : VisTable() {

    private val guiStage get() = stage as PlayGUIStage

    val availableUnits = HashMap<Int, Array<UnitActor>>()

    init {
        setFillParent(true)
    }

    fun show(base: TileActor, player: Player) {
        check(stage != null)

        clear()
        add(BuyMenuPane(guiStage, base, player, availableUnits[player.id]))
            .size(325f, 460f).center()
    }
}

private class BuyMenuPane(
    private val guiStage: PlayGUIStage,
    var baseTile: TileActor,
    val player: Player? = null,
    val units: Array<UnitActor>? = null,
) : VisTable() {

    companion object {
        private const val ICON_SIZE = 92f
    }

    private val gridGroup = GridGroup(ICON_SIZE)
    private val scrollPane = VisScrollPane(gridGroup)

    var unitLeaderId = -1

    private val captureListener = object : EventListener {
        override fun handle(event: Event?): Boolean {
            if (isVisible && event is InputEvent) {

                if (event.target.isDescendantOf(this@BuyMenuPane)) {
                    if (event.type == InputEvent.Type.touchDragged)
                        event.stop()
                } else {
                    if (event.type == InputEvent.Type.touchDown) {
                        event.stop()
                        remove()
                    }
                }

                return true
            }

            return false
        }
    }

    init {
        background = VisUI.getSkin().getDrawable("background")
        // setSize(340f, 400f)

        val playStage = baseTile.playStageOrNull ?: throw IllegalStateException("Base tile in not on the stage.")
        val isWater = baseTile.isWaterBase

        //grid group
        val unitsData = guiStage.playScreen.unitsData

        fun addB(unitActor: UnitActor, cost: Int) {
            if (isWater && !unitActor.isWater) return
            if (!isWater && !unitActor.isLand && !unitActor.isAir) return
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
            val units = this.units ?: getSpeciesUnits(player.species)
            for (unit in units) {
                if (unit.cShop != null)
                    addB(unit, unit.cShop?.price ?: 999999)
            }
        }

        //scroll pane
        scrollPane.setScrollingDisabled(true, false)
        guiStage.scrollFocus = scrollPane
        add(VisLabel("Buy Menu")).center()
        row()
        add(scrollPane).fill().expand()
    }

    private fun GridGroup.addButton(unitButton: UnitButton) {
        addActor(unitButton)
        unitButton.button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)

                if (guiStage.currentState == guiStage.myTurn && baseTile != null) {
                    // give the unit to the first leader that comes to the unit if there are no leaders by default
                    var unitLeaderIdLoc = unitLeaderId
                    if (unitLeaderIdLoc == -1) {
                        for (unit in guiStage.playStage.getUnitsOfPlayer(guiStage.localPlayer)) {
                            if (unit.isLeader)
                                unitLeaderIdLoc = unit.leaderID
                        }
                    }

                    val buyCommand = BuyUnitCommand(
                        unitButton.unit.selfName,
                        guiStage.localPlayer.id,
                        unitButton.cost,
                        baseTile!!.tiledX,
                        baseTile!!.tiledY,
                        unitLeaderIdLoc
                    )

                    if (buyCommand.canExecute(guiStage.playScreen))
                        guiStage.playScreen.commandManager.queueCommand(buyCommand)
                }

                this@BuyMenuPane.remove()
            }
        })
    }

    override fun setStage(stage: Stage?) {
        if (stage != null) {
            stage.root.addActorBefore(parent, StageCover(0.5f).addClickListener {
                remove()
            })

            //fadeIn()
            //stage.addCaptureListener(captureListener)
        } else
            this.stage?.removeCover()

        super.setStage(stage)
    }

    private class UnitButton(val unit: UnitActor, val cost: Int) : WidgetGroup() {

        val button = newIconButton("buy_unit")
        private val labelCost = newLabel(cost.toString())

        init {
            val icon = assets.atlases.findRegion("gui/icons/${unit.selfName}")
            val region = findUnitTextures(unit.selfName, unit.playerId).firstOrNull()

            button.style.imageUp =
                if (icon != null)
                    VisUI.getSkin().getDrawable("icons/${unit.selfName}")
                else
                    TextureRegionDrawable(region)

            button.style.imageUp.minWidth = 64f
            button.style.imageUp.minHeight = 64f

            labelCost.setPosByCenter(32f + labelCost.width / 2, 84f)

            addActor(button)
            addActor(labelCost)

            setScale(1f)
        }
    }
}
