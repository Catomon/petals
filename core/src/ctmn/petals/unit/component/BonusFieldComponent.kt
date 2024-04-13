package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

// Applies bonus units nearby
// healing applies at the start of every units players turn
data class BonusFieldComponent(
    var range: Int = 1,
    var damage: Int = 0,
    var defense: Int = 0,
    var healing: Int = 0
    ) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}