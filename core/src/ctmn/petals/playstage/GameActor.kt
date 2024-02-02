package ctmn.petals.playstage

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.scenes.scene2d.Actor

abstract class GameActor : Actor() {
    val components = Entity()

    /** Add component */
    open fun add(component: Component) : Component {
        components.add(component)

//        if (component is UnitComponent)
//            name = component.name

        return component
    }

    /** Delete component */
    fun del(component: Component) : Component? {
        return del(component::class.java)
    }

    fun del(componentClass: Class<out Component>) : Component? {
        return components.remove(componentClass)
    }

    /** @return component with the type of compType */
    fun <T: Component> get(compType: Class<out T>) : T? {
        return components.getComponent(compType)
    }

    /** @return true if there is component of type compType */
    fun has(compType: Class<out Component>) : Boolean {
        return get(compType) != null
    }
}
