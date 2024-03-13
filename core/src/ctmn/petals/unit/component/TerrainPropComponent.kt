package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.unit.TerrainProps
import ctmn.petals.utils.CopyableComponent

/** String - terrain name; Int - movement cost. */
data class TerrainPropComponent(val props: TerrainProps) : Component, CopyableComponent {

    override fun makeCopy(): Component {
        return TerrainPropComponent(props.copy())
    }
}