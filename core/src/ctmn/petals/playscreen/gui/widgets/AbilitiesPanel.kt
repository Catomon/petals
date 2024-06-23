package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.playscreen.commands.GrantAbilityCommand
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playscreen.events.UnitAddedEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.Ability
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.newLabel
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.widgets.addFocusBorder
import java.util.*

class AbilitiesPanel(val guiStage: PlayGUIStage) : VisTable() {

    var selectedAbility: Ability? = null
        set(value) {

            if (field is SummonAbility && value !is SummonAbility)
                    (cells.firstOrNull { it.actor is SummonAbilityButton }?.actor as SummonAbilityButton?)?.hidePane()

            for (button in buttons) {
                if (button is AbilityButton) {
                    button.isChecked = false
                    if (value != null && button.ability == value)
                        button.isChecked = true
                }
            }

            field = value
        }

    private val buttons = Array<VisImageButton>()

    private val maxButtons = 5

    var unit: UnitActor? = null
        set(value) {
            field = value

            isVisible = value != null
        }

    var rememberUnit = false

    init {
        padBottom(1f)

        isVisible = false

        updateButtons()

        guiStage.playStage.addListener {
            when (it) {
                is CommandExecutedEvent -> {
                    if (it.command !is GrantAbilityCommand) return@addListener false

                    val unit = this.unit

                    setUpForUnit(null)
                    setUpForUnit(unit)
                }

                is UnitAddedEvent -> {
                    if (rememberUnit && it.unit.name == UnitIds.ALICE_ID)
                        setUpForUnit(it.unit)
                }
            }

            false
        }
    }

    private fun updateButtons() {
        clear()

        while (buttons.size < maxButtons) {
            buttons.add(AbilityButton().apply { color.a = 0.5f })
        }
        for (button in buttons)
            add(button).bottom().center().padRight(-1f)
    }

    fun updateAbilities() {
        val unit = this.unit ?: return

        setUpForUnit(null, false)
        setUpForUnit(unit)
    }

    fun setUpForUnit(unitActor: UnitActor?, rememberUnit: Boolean = this.rememberUnit) {
        for (button in buttons) {
            if (button is AbilityButton)
                button.isChecked = false
        }

        var unitActor = if (rememberUnit && unitActor == null && unit != null) unit else unitActor
        unitActor =
            if (unitActor != null && (unitActor.stage == null || !unitActor.isAlive()))
                null
            else
                unitActor

        // if you want to select the same unit do as here:
        // setUpForUnit(null, false)
        // setUpForUnit(unit)
        if (unitActor == unit) {
            return
        }

        unit = unitActor

        buttons.clear()

        if (unitActor == null || unitActor.cAbilities == null) {
            updateButtons()
            isVisible = false
            return
        }

        isVisible = true

        for (ability in unitActor.abilities) {
            when (ability) {
                is SummonAbility -> buttons.add(SummonAbilityButton(guiStage, unitActor))
                else -> buttons.add(AbilityButton().apply { this.ability = ability })
            }

            if (buttons.size > maxButtons) {
                Gdx.app.log(
                    AbilitiesPanel::class.simpleName,
                    "Unit has too much abilities (${unitActor.abilities.size}>$maxButtons). Some of them are not shown.")

                break
            }
        }

        updateButtons()
    }

    inner class AbilityButton : VisImageButton(
            VisUI.getSkin().get("ability", VisImageButtonStyle::class.java)) {

        //private var iconSprite: Sprite = Sprite(guiStage.assets.getAtlasRegion("gui/healing_ic"))

        private val cooldownLabel = newLabel("0", "font_8")
        private val costLabel = newLabel("mana label", "font_5").apply { isVisible = false }

        private var drawable: Drawable? = null

        var ability: Ability? = null
        set(value) {
            isDisabled = value == null

            costLabel.isVisible = value != null

            drawable = if (value != null) {
                //set mana cost
                costLabel.setText("${value.cost}")
                costLabel.pack()

                //find drawable
                VisUI.getSkin().getDrawable(
                    guiStage.assets.atlases.findRegion(
                        "gui/icons/${value.name.toLowerCase(Locale.ROOT)}")?.let {
                        "icons/${value.name.toLowerCase(Locale.ROOT)}"
                    } ?: "icons/no_icon")
            } else null

            field = value
        }

        init {
            isDisabled = true

            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    super.clicked(event, x, y)

                    if (isDisabled)
                        return

                    if (ability == null) {
                        guiStage.mapClickListener = guiStage.unitSelectedCL
                        return
                    }

                    guiStage.selectUnit(unit)

                    if (guiStage.currentState != guiStage.myTurn) return

                    if (ability!!.range <= 0) {
                        guiStage.mapClickListener = guiStage.confirmAbilityCL
                        guiStage.abilityActivationRangeBorder.makeForRange(ability!!.activationRange,
                            guiStage.selectedUnit!!.tiledX,
                            guiStage.selectedUnit!!.tiledY,
                            guiStage.playStage)
                        guiStage.abilityActivationRangeBorder.show(true)

                        selectedAbility = ability

                        return
                    }

                    selectedAbility = ability

                    guiStage.mapClickListener = guiStage.useAbilityCL
                    guiStage.abilityRangeBorder.makeForRange(
                            ability!!.range,
                            guiStage.selectedUnit!!.tiledX,
                            guiStage.selectedUnit!!.tiledY,
                            guiStage.playStage)
                    guiStage.abilityRangeBorder.show(true)
                }
            })

            addFocusBorder()

            addActor(costLabel)
        }

        override fun draw(batch: Batch, parentAlpha: Float) {
            style.imageUp = drawable

            if (unit != null && ability != null)
                costLabel.color = if (ability!!.cost > unit!!.mana) Color.LIGHT_GRAY else Color.WHITE

            if (isPressed || isChecked)
                costLabel.setPosByCenter(width / 2, height - costLabel.height / 2 + style.pressedOffsetY * -1 - 2)
            else
                costLabel.setPosByCenter(width / 2, height - costLabel.height / 2 - 2)

            if (ability != null && ability!!.currentCooldown > 0) {
                super.draw(batch, 0.4f)

                cooldownLabel.setText("${ability!!.currentCooldown}")
                cooldownLabel.pack()
                cooldownLabel.setPosByCenter(centerX, centerY - 1)
                cooldownLabel.draw(batch, 1f)
            } else
                super.draw(batch, parentAlpha)
        }
    }
}
