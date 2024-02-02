package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.Color
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.events.MapClickedEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.actors.*
import ctmn.petals.utils.*
import ctmn.petals.widgets.newLabel
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.addFocusBorder
import ctmn.petals.widgets.newImageButton
import java.io.FileNotFoundException

class SummonAbilityButton(val gui: PlayGUIStage, pUnitActor: UnitActor? = null) : VisImageButton("summon") {

    var caster: UnitActor? = null
        set(value) {
            if (field != value)
            if (holdAlice) {
                if (value != null) {
                    field = value
                    isDisabled = false
                }
            } else {
                if (field == null) {
                    field = value
                    isDisabled = value == null
                }
            }

            if (pane.stage != null) {
                pane.remove()
            }

            costLabel.setText(summonAbility?.cost ?: 0)
            costLabel.isVisible = value != null
        }

    private val summonAbility get() = caster?.abilities?.firstOrNull { it is SummonAbility }

    private val dollsListTable = VisTable()
    private val pane = VisScrollPane(dollsListTable, "doll_scroll")
    private val cooldownLabel = newLabel("0", "font_8")
    private val costLabel = newLabel("0", "font_5")

    private val dollsShownOnPane = 6

    private var dollsAmount = 0

    private var holdAlice = true

    private val guiEventListener = EventListener {
        if (it is MapClickedEvent) {
            if (pane.stage != null) {
                pane.remove()
            }
        }

        return@EventListener false
    }

    init {
        caster = pUnitActor

        val caster = this.caster
        val summonAbility = this.summonAbility!! as SummonAbility

        if (caster is Alice)
            style = VisUI.getSkin()["summon_doll", VisImageButtonStyle::class.java]

        pane.setScrollingDisabled(true, false)

        addChangeListener {
            if (gui.currentState == gui.myTurn && caster?.stage != null) {

                dollsListTable.clear()
                dollsAmount = 0
                for (unitId in caster.summoner.units) {
                    if (Units.names.contains(unitId))
                        addDoll(unitId)
                }

                if (gui.selectedUnit != caster)
                    gui.selectUnit(caster)

                if (pane.stage != null) {
                    pane.remove()

                    gui.clickStrategy = gui.unitSelectedCs

                    return@addChangeListener
                }

                //move camera to alice
                if (gui.playStage.isOffScreen(
                        caster.sprite!!.centerX(),
                        caster.sprite!!.centerY(),
                        100f
                    )
                )
                    gui.playScreen.actionManager.queueAction(
                        CameraMoveAction(caster.sprite!!.centerX(), caster.sprite!!.centerY())
                    )

                if (summonAbility.currentCooldown > 0)
                    return@addChangeListener


                if (caster.summoner.units.isEmpty())
                    return@addChangeListener

                //show dolls pane
                pane.scrollY = 4f
                pane.setSize(width, (height + 1) * (if (dollsAmount > dollsShownOnPane) dollsShownOnPane else dollsAmount))
                val inStagePosition = localToStageCoordinates(Vector2(x, y + height))
                pane.setPosition(inStagePosition.x, inStagePosition.y - 2)
                gui.addActor(pane)

                gui.abilitiesPanel.selectedAbility = summonAbility

                gui.clickStrategy = gui.useAbilityCs
            }
        }

        gui.addListener(guiEventListener)

        addFocusBorder()

        costLabel.pack()
        addActor(costLabel)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        if (summonAbility != null)
            costLabel.color = if (summonAbility!!.cost > caster!!.mana) Color.LIGHT_GRAY else Color.WHITE

        if (isPressed)
            costLabel.setPosByCenter(width / 2, height - costLabel.height / 2 + style.pressedOffsetY * -1 - 2)
        else
            costLabel.setPosByCenter(width / 2, height - costLabel.height / 2 - 2)

        val summonAbility = caster!!.abilities.first { it is SummonAbility } as SummonAbility
        if (caster != null && summonAbility.currentCooldown > 0) {
            super.draw(batch, 0.4f)
            cooldownLabel.setText("${summonAbility.currentCooldown}")
            cooldownLabel.setPosByCenter(centerX + 3f, centerY)
            cooldownLabel.draw(batch, parentAlpha)
        }
        else
            super.draw(batch, parentAlpha)
    }

    private fun addDoll(unitName: String) {
        if (!Units.names.contains(unitName)) throw IllegalArgumentException("No unit with such name: $unitName")

        val button = newImageButton("doll")

        val icon = Image(gui.assets.get<TextureAtlas>("textures.atlas").findRegion("gui/icons/$unitName").also { if (it == null) throw FileNotFoundException("No unit icon found in icons/${unitName}.") })
        icon.setSize(16f, 16f)
        icon.setPosByCenter(button.width / 2, button.width / 2)
        button.addActor(icon)

        val unitSummonCost = Units.get(unitName).cSummonable?.cost ?: 0
        val dollCostLabel = newLabel("$unitSummonCost", "font_5")
        dollCostLabel.pack()
        dollCostLabel.setPosByCenter(button.width / 2, button.height - dollCostLabel.height / 2 - 2)
        if (summonAbility != null)
            dollCostLabel.color = if (unitSummonCost > caster!!.mana) Color.LIGHT_GRAY else Color.WHITE

        button.addActor(dollCostLabel)

        button.userObject = caster

        button.addChangeListener {
            caster?.summoner?.selectedUnit = unitName

            //clickStrategy = SummonDollCs??

            if (pane.stage != null) {
                pane.remove()
            }
        }

        dollsListTable.add(button)
        dollsListTable.row()

        dollsAmount++
    }

    fun hidePane() {
        pane.remove()
    }

    override fun setDisabled(disabled: Boolean) {
        super.setDisabled(disabled)

        if (pane.stage != null) {
            pane.remove()
        }
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage == null) {
            pane.remove()
            gui.removeListener(guiEventListener)
        }
    }
}