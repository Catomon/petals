package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class ShopComponent(var price: Int) : Component, CopyableComponent {

    override fun makeCopy(): Component {
        return copy()
    }
}