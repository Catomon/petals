package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

/** String - terrain name; Int - movement cost. */
data class TerrainCostComponent(val costs: HashMap<String, Int> = HashMap()) : Component, CopyableComponent {

    override fun makeCopy(): Component {
        return copy()
    }
}