package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.*
import ctmn.petals.GamePref
import ctmn.petals.player.Species
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.Units
import ctmn.petals.unit.cMatchUp
import ctmn.petals.unit.findUnitTextures
import ctmn.petals.utils.removeCover
import ctmn.petals.widgets.TextureRegionActor
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton

class Book(val guiStage: PlayGUIStage) : VisTable() {

    data class BookSave(
        var units: MutableList<String> = mutableListOf(),
        var matchups: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    )

    companion object {
        val bookSave = GamePref.bookSave
    }

    private val unitsTable =
        VisTable().apply {
            for (unitName in Units.names.sortedBy {
                when {
                    Species.fairies.units[it] != null -> 1
                    Species.goblins.units[it] != null -> 2
                    else -> 3
                }
            }) {
                val unitActor = Units.find(unitName) ?: continue
                if (!bookSave.units.contains(unitActor.selfName)) continue

                val matchups = unitActor.cMatchUp?.filter { bookSave.matchups[unitName]?.contains(it.key) == true }
                add(HorizontalGroup().apply {
                    addActor(TextureRegionActor(findUnitTextures(unitName, 1).firstOrNull()).also {
                        it.setSize(
                            64f,
                            64f
                        )
                    })
                    addActor(VerticalGroup().apply {
                        addActor(VisLabel(unitActor::class.simpleName))
                        addActor(Actor().also { it.setSize(16f, 16f) })
                        if (matchups?.isNotEmpty() == true) {
                            addActor(VisLabel("Bonus against:").apply { setFontScale(0.75f) })
                        }
                    })
                })

                matchups ?: continue

                row()
                add(GridGroup(32f).also gridAlso@{ grid ->
                    for (matchup in matchups) {
                        val matchUnit = Units.find(matchup.key) ?: continue
                        grid.addActor(
                            TextureRegionActor(
                                findUnitTextures(
                                    matchUnit.selfName,
                                    1
                                ).firstOrNull() ?: continue
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

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage == null)
            GamePref.bookSave = bookSave
    }
}