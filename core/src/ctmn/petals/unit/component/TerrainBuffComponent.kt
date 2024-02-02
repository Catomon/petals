package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class TerrainBuffComponent(
    val buffs: HashMap<String, Pair<Int, Int>> = HashMap()
) : Component, CopyableComponent {

    override fun makeCopy(): Component {
        return copy()
    }
}