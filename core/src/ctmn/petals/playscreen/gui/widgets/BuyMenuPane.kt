package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
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
import ctmn.petals.widgets.*

class BuyMenu : VisTable() {

    private val guiStage get() = stage as PlayGUIStage

    val availableUnits = HashMap<Int, Array<UnitActor>>()

    init {
        setFillParent(true)
    }

    fun show(base: TileActor, player: Player) {
        check(stage != null)

        clear()
        val buyMenuPane = BuyMenuPane(guiStage, base, player, availableUnits[player.id])
        val closeButton = newTextButton("Close").addChangeListener { buyMenuPane.remove(); it.remove() }.addClickSound()
        add(VisLabel("Buy Menu")).center().padTop(6f).padBottom(10f)
        row()
        add(buyMenuPane).width(325f).center().fillY().expandY()
        row()
        add(closeButton).padBottom(16f).width(325f)
    }
}

private class BuyMenuPane(
    private val guiStage: PlayGUIStage,
    var baseTile: TileActor,
    val player: Player? = null,
    val filterUnits: Array<UnitActor>? = null,
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
        //background = VisUI.getSkin().getDrawable("background")
        // setSize(340f, 400f)

        val playStage = baseTile.playStageOrNull ?: throw IllegalStateException("Base tile in not on the stage.")
        val isWater = baseTile.isWaterBase

        //grid group
        val unitsData = guiStage.playScreen.unitsData

        fun addB(unitActor: UnitActor, cost: Int, unlocked: Boolean = true): UnitButton? {
            if (isWater && !unitActor.isWater) return null
            if (!isWater && !unitActor.isLand && !unitActor.isAir) return null
            val button = UnitButton(unitActor, cost, unlocked)
            gridGroup.addButton(button)

            return UnitButton(unitActor, cost)
        }

        if (player == null) {
            //add all units with ShopComponent
            for (name in unitsData.names) {
                val newUnit = unitsData.get(name)
                if (newUnit.cShop != null) {
                    addB(newUnit, newUnit.cShop?.price ?: 999999)
                }
            }
        } else {
            //add species units
            val units = getSpeciesUnits(player.species)
            val lockedUnits = Array<UnitActor>()
            for (unit in units) {
                if (unit.cShop != null) {
                    val unlocked = filterUnits?.any { it.selfName == unit.selfName } ?: true
                    if (unlocked) {
                        addB(unit, unit.cShop?.price ?: 999999, true)
                    } else {
                        lockedUnits.add(unit)
                    }
                }
            }

            lockedUnits.forEach { addB(it, it.cShop?.price ?: 999999, false) }
        }

        //scroll pane
        scrollPane.setScrollingDisabled(true, false)
        guiStage.scrollFocus = scrollPane
        add(scrollPane).fill().expand()
    }

    private fun GridGroup.addButton(unitButton: UnitButton) {
        addActor(unitButton)
        unitButton.button.addChangeListener {
            if (guiStage.currentState == guiStage.myTurn && baseTile != null) {
                guiStage.buyMenu.clear()

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
    }

    override fun setStage(stage: Stage?) {
        if (stage != null) {
            stage.root.addActorBefore(parent, StageCover(0.5f))

            //fadeIn()
            //stage.addCaptureListener(captureListener)
        } else
            this.stage?.removeCover()

        super.setStage(stage)
    }

    private class UnitButton(val unit: UnitActor, val cost: Int, val unlocked: Boolean = true) : WidgetGroup() {

        val button = newIconButton("buy_unit")
        private val labelCost = newLabel(cost.toString())

        init {
            addActor(button)
            if (unlocked) {
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

                addActor(labelCost)
            } else {
                button.isDisabled = true
            }

            setScale(1f)
        }
    }
}
