package ctmn.petals.playscreen.gui.widgets

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
import ctmn.petals.player.Building
import ctmn.petals.player.Player
import ctmn.petals.playscreen.commands.BuildCommand
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getTiles
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.removeCover
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.*

class BuildMenu(
    val guiStage: PlayGUIStage,
    val unit: UnitActor,
    val tile: TileActor,
    val player: Player,
    val buildings: Array<Building>,
) : VisTable() {

    private val gridGroup = GridGroup(ICON_SIZE)
    private val scrollPane = VisScrollPane(gridGroup)

    companion object {
        private const val ICON_SIZE = 92f
    }

    init {
        setFillParent(true)

        //scroll pane
        scrollPane.setScrollingDisabled(true, false)
        guiStage.scrollFocus = scrollPane

        val closeButton = newTextButton("Close").addChangeListener { this@BuildMenu.remove() }.addClickSound()
        add(VisLabel("Buildings Menu")).center().padTop(6f).padBottom(10f)
        row()
        add(scrollPane).width(325f).center().fillY().expandY()
        row()
        add(closeButton).padBottom(16f).width(325f)

        val playStage = tile.playStageOrNull ?: throw IllegalStateException("The tile in not on the stage.")

        //grid group
        val tiles = playStage.getTiles()
        for (building in buildings) {
            val terrains = building.terrains
            val hasRequiredBuilding = building.requires == "" || tiles.any { it.selfName == building.requires && it.cPlayerId?.playerId == player.id }
            val unlocked = !terrains.none { it == tile.terrain } && hasRequiredBuilding

            val buildButton = BuildingButton(building.name, building.cost, unlocked)
            buildButton.button.addChangeListener {
                if (guiStage.currentState == guiStage.myTurn) {
                    val buildCommand = BuildCommand(
                        building.name,
                        building.buildTime,
                        building.cost,
                        unit,
                        tile
                    )

                    if (buildCommand.canExecute(guiStage.playScreen))
                        guiStage.playScreen.commandManager.queueCommand(buildCommand)
                }

                this@BuildMenu.remove()
            }
            gridGroup.addActor(buildButton)
        }
    }

    override fun setStage(stage: Stage?) {
        if (stage != null) {
            stage.root.addActorBefore(this, StageCover(0.5f))

            //fadeIn()
            //stage.addCaptureListener(captureListener)
        } else
            this.stage?.removeCover()

        super.setStage(stage)
    }

    private class BuildingButton(
        buildingName: String,
        cost: Int,
        unlocked: Boolean = true,
    ) : WidgetGroup() {

        val button = newIconButton("buy_unit")
        private val labelCost = newLabel(cost.toString())

        init {
            addActor(button)
            if (unlocked) {
                val icon = assets.atlases.findRegion("gui/icons/${buildingName}")
                val region = assets.tilesAtlas.regions.firstOrNull { it.name.endsWith(buildingName) }

                button.style.imageUp =
                    if (icon != null)
                        VisUI.getSkin().getDrawable("icons/${buildingName}")
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
