package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.assets
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.cAbilities
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPositionByCenter
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.unit.abilities
import ctmn.petals.unit.Abilities
import ctmn.petals.widgets.addChangeListener
import java.util.*

class AbilitiesSelectTable(
    val unitActor: UnitActor,
    var buttonChangeListener: ChangeListener? = null
) : VisTable() {

    private var abilitiesCount = 0
    var abilitiesRows = 5

    val abilities = Array<Ability>().apply {
        for (name in Abilities.keys()) {
            add(Abilities.getAbility(name))
        }
    }

    private val abilitiesTable = VisTable()
    private val scrollPane = VisScrollPane(abilitiesTable)

    init {
        setBackground("background")

        add(scrollPane).center()

        setForUnit(unitActor)
    }

    fun setForUnit(unit: UnitActor, lockAll: Boolean = false) {
        val unitAbilities = unit.cAbilities?.abilities ?: return

        abilitiesTable.clear()
        abilitiesCount = 0

        for (ability in abilities) {
            val unlock = if (unitActor.abilities.firstOrNull { it.name == ability.name } != null) true else lockAll

            addAbilityButton(ability, unlock).isDisabled = unitAbilities.firstOrNull { it.name == ability.name } != null
        }

        invalidate()
        pack()
    }

    fun unlock(abilityName: String) {
        for (abilityButtonCell in abilitiesTable.cells) {
            if (abilityButtonCell.actor is AbilityButton) {
                if ((abilityButtonCell.actor as AbilityButton).ability.name == abilityName) {
                    (abilityButtonCell.actor as AbilityButton).unlock()
                }
            }
        }
    }

    private fun addAbilityButton(ability: Ability, locked: Boolean = false) : AbilityButton {
        val button = AbilityButton(ability, locked)

        abilitiesTable.add(button)

        abilitiesCount++
        if (abilitiesCount % abilitiesRows == 0)
            abilitiesTable.row()

        return button
    }

    private inner class AbilityButton(val ability: Ability, locked: Boolean = false) : VisImageButton("ability_item") {

        private val sprite = Sprite(assets.atlases.findRegion("gui/icons/${ability.name.toLowerCase(Locale.ROOT)}")
            ?: assets.atlases.findRegion("gui/icons/no_icon"))

        init {
            userObject = ability

            if (locked) {
                sprite.setRegion(assets.atlases.findRegion("gui/icons/locked"))
            } else {
                unlock()
            }
        }

        fun unlock() {
            sprite.setRegion(assets.atlases.findRegion("gui/icons/${ability.name.toLowerCase(Locale.ROOT)}")
                ?: assets.atlases.findRegion("gui/icons/no_icon"))

            buttonChangeListener?.let {
                addListener(it)
            } ?: addChangeListener {
                unitActor.cAbilities?.abilities?.add(ability)?.let {

                    (stage as PlayGUIStage).abilitiesPanel.updateAbilities()
                }

                this@AbilitiesSelectTable.remove()
            }
        }

        override fun draw(batch: Batch?, parentAlpha: Float) {
            super.draw(batch, parentAlpha)

            sprite.setPositionByCenter(centerX, centerY)
            sprite.draw(batch)
        }
    }
}