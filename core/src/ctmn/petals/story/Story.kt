package ctmn.petals.story

import com.badlogic.gdx.utils.Array

abstract class Story(
    val name: String,
    val id: Int,
    var storySave: StorySaveGson = StorySaveGson(name, id)
) {

    val scenarios = Array<Scenario>()

    val currentScenario: Scenario get() =
        if (storySave.progress > scenarios.size)
            scenarios[scenarios.size - 1]
        else
            scenarios[storySave.progress]

    var areScenariosInitialized = false

    init {

    }

    open fun initScenarios() {
        scenarios.clear()
        areScenariosInitialized = true
    }

    fun applySave() {
        currentScenario.loadFrom(storySave)
    }

    fun onScenarioOverSave() {
        currentScenario.saveTo(storySave)

        storySave.progress++
        if (storySave.progress == scenarios.size)
            storySave.progress = 0
    }
}