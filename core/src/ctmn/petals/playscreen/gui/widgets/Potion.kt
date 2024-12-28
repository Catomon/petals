package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.Color
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.player.Tech
import ctmn.petals.player.Techs
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playstage.getBuildings
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.utils.removeCover
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton

class Potion(val guiStage: PlayGUIStage) : VisTable() {

    init {
        setFillParent(true)

        val player = guiStage.localPlayer

        add(VisScrollPane(
            VisTable().apply {
                fun addTechBtn(tech: Tech, learned: Boolean, available: Boolean) {
                    add(newTextButton(if (learned) "(learned)" + tech.name else tech.name).apply {
                        if (learned) label.color = Color.GREEN
                        isDisabled = learned || !available
                        addChangeListener {
                            if (player.techs.contains(tech.name)) return@addChangeListener
                            player.techs += tech.name
                            if (tech.targetType == Tech.TargetType.BoughtUnit) {
                                guiStage.playStage.getUnitsOfPlayer(player).forEach { unitActor ->
                                    tech.applyTechToUnit(unitActor)
                                }
                            }
                            isDisabled = true
                        }
                    }).width(325f)
                }
                for (tech in Techs.map.values) {
                    if (tech.species != player.species) continue
                    addTechBtn(tech, player.techs.contains(tech.name), tech.buildingsNeeded.all { needBuild ->
                        guiStage.playStage.getBuildings(player)
                            .any { it.tileName.startsWith(needBuild) }
                    })
                }
            }
        ).also { scrollPane ->
            guiStage.scrollFocus = scrollPane
            scrollPane.setScrollingDisabled(true, false)
        }).width(325f).expandY()
        row()
        add(newTextButton("Close").addChangeListener {
            stage?.removeCover()
            remove()
        }).width(325f)
    }
}