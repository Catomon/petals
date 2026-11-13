package ctmn.petals.story

import com.badlogic.gdx.utils.Array
import ctmn.petals.Config.TREASURED_PETALS_STORY_ID
import ctmn.petals.story.alissa.AlissaStory
import kotlin.reflect.KClass

object StoriesManager : HashMap<Int, KClass<out Story>>() {

    init {
        put(TREASURED_PETALS_STORY_ID, AlissaStory::class)
    }

    private fun create(id: Int): Story =
        when (id) {
            TREASURED_PETALS_STORY_ID -> AlissaStory()
            else -> throw IllegalArgumentException("story '$id' not found")
        }

    fun getStories(): Array<Story> {
        val stories = Array<Story>()
        for (id in keys) {
            stories.add(create(id))
        }

        return stories
    }

    fun byName(name: String): Story {
        for (id in keys) {
            val story = create(id)
            if (name == story.name)
                return story
        }

        throw IllegalArgumentException("story '$name' not found")
    }

    fun byId(id: Int): Story = create(id)
}
