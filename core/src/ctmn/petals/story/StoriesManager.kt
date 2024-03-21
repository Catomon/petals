package ctmn.petals.story

import com.badlogic.gdx.utils.Array
import ctmn.petals.Const.TREASURED_PETALS_STORY_ID
import kotlin.reflect.KClass

object StoriesManager : HashMap<Int, KClass<out Story>>() {

    init {
        put(TREASURED_PETALS_STORY_ID, AlissaStory::class)
    }

    fun getStories() : Array<Story> {
        val stories = Array<Story>()
        for (scenarioClass in this.values) {
            val scenario = scenarioClass.java.constructors[0].newInstance() as Story
            stories.add(scenario)
        }

        return stories
    }

    fun byName(name: String) : Story {
        for (scenarioClass in this.values) {
            val scenario = scenarioClass.java.constructors[0].newInstance() as Story
            if (name == scenario.name)
                return scenario
        }

        throw IllegalArgumentException("story '$name' not found")
    }

    fun byId(id: Int) : Story {
        return this[id]!!.java.constructors[0].newInstance() as Story
    }
}
