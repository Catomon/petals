package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.*
import ctmn.petals.player.getSpeciesUnits
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.Units
import ctmn.petals.unit.cMatchUp
import ctmn.petals.unit.findUnitTextures
import ctmn.petals.utils.removeCover
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton

class Book(val guiStage: PlayGUIStage) : VisTable() {

    private val unitsTable =
        VisTable().apply {
            for (unitName in Units.names) {
                val unitActor = Units.find(unitName) ?: continue
                if (getSpeciesUnits(guiStage.localPlayer.species).none { it.unitActor.selfName == unitName }) continue
                add(VisTable().apply {
                    add(VisImage(SpriteDrawable(Sprite(findUnitTextures(unitName, 1).firstOrNull())))).size(64f)
                    add(VisTable().apply {
                        add(VisLabel(unitActor::class.simpleName))
                        row()
                        add(VisLabel("Bonus against:").apply { setFontScale(0.75f) }).padTop(16f)
                    })
                })
                val matchups = unitActor.cMatchUp ?: continue

                row()
                add(GridGroup(32f).also gridAlso@{ grid ->
                    for (matchup in matchups) {
                        val matchUnit = Units.find(matchup.key) ?: continue
                        grid.addActor(
                            VisImage(
                                SpriteDrawable(
                                    Sprite(
                                        findUnitTextures(
                                            matchUnit.selfName,
                                            1
                                        ).firstOrNull() ?: continue
                                    )
                                )
                            )
                        )
                    }
                }).width(300f)
                row()
                add(Separator(VisUI.getSkin()[Separator.SeparatorStyle::class.java])).fillX().expandX()
                row()
            }
        }

    private val unitsCollapsibleWidget = CollapsibleWidget(unitsTable).apply { setCollapsed(false, false) }

    private val unitsButtonName get() = if (unitsCollapsibleWidget.isCollapsed) "Units >" else "Units v"

    init {
        setFillParent(true)

        val player = guiStage.localPlayer

        add(VisScrollPane(
            VisTable().apply {
                background = VisUI.getSkin().getDrawable("background")

                add(newTextButton(unitsButtonName).addChangeListener {
                    unitsCollapsibleWidget.setCollapsed(!unitsCollapsibleWidget.isCollapsed, false)
                    it.setText(unitsButtonName)
                }).width(325f)
                row()
                add(unitsCollapsibleWidget)
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