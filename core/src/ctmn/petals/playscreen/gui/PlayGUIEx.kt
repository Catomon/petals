package ctmn.petals.playscreen.gui

import ctmn.petals.effects.FloatingUpLabel
import ctmn.petals.unit.abilities
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.Ability
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox

fun PlayGUIStage.floatingLabel(text: String) {
    addActor(FloatingUpLabel(text))
}

class SelectBoxAbilityItem(val ability: Ability? = null, val name: String = ability?.name ?: "None") {
    override fun toString(): String {
        return name
    }
}

fun SelectBox<SelectBoxAbilityItem>.setSelectBoxAbilitiesFor(unit: UnitActor?) {
    clearItems()
    items.add(SelectBoxAbilityItem(null, "Abilities"))

    if (unit != null) {
        isDisabled = unit.abilities.isEmpty()
        for (a in unit.abilities)
            items.add(SelectBoxAbilityItem(a))
    } else {
        isDisabled = true
    }

    setItems(items)
    selectedIndex = 0
}
