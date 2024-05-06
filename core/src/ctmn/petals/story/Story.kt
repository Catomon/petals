package ctmn.petals.story

import com.badlogic.gdx.utils.Array

abstract class Story(
    val name: String,
    val id: Int,
    var storySave: StorySaveGson = StorySaveGson(name, id),
) {

    val scenarios = Array<Scenario>()

    val currentUndoneScenario: Scenario?
        get() {
            val curI = scenarios.indexOfLast { sc -> storySave.progress.levels.any { it.key == sc.id } } + 1
            return if (curI >= scenarios.size) null else scenarios[curI]
        }

    var areScenariosInitialized = false

    init {

    }

    open fun initScenarios() {
        scenarios.clear()
        areScenariosInitialized = true
    }

    fun applySave() {
        if (currentUndoneScenario == null) return

        currentUndoneScenario!!.loadFrom(storySave)
    }

    fun onScenarioOverSave() {
        if (currentUndoneScenario == null) return

        currentUndoneScenario!!.saveTo(storySave)
    }
}