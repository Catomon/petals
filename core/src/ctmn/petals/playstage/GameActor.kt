package ctmn.petals.playstage

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.sim.BattleField

abstract class GameActor : Actor() {
    val components = Entity()

    /**
     * The battle field this actor currently belongs to, if any. It lets the shared model keep
     * positional caches correct both on a scene2d `PlayStage` and on a headless [BattleField].
     */
    var field: BattleField? = null

    open fun add(component: Component) : Component {
        components.add(component)
        return component
    }

    fun del(component: Component) : Component? {
        return del(component::class.java)
    }

    fun del(componentClass: Class<out Component>) : Component? {
        return components.remove(componentClass)
    }

    fun <T: Component> get(compType: Class<out T>) : T? {
        return components.getComponent(compType)
    }

    fun has(compType: Class<out Component>) : Boolean {
        return get(compType) != null
    }
}
