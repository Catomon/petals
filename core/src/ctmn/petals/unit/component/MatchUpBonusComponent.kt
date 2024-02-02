package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

/** DMG, DEF */
data class MatchUpBonusComponent(val bonuses: HashMap<String, Pair<Int, Int>> = HashMap()) : Component, CopyableComponent {

    override fun makeCopy(): Component {
        return copy()
    }
}