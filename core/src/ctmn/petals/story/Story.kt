package ctmn.petals.story

abstract class Story(
    val name: String,
    val id: Int,
    var storySave: StorySaveGson = StorySaveGson(name, id),
) {

    protected val scenarios = LinkedHashMap<String, Class<out Scenario>>()

    val size: Int get() = scenarios.size

    var scenariosAdded = false

    val isEmpty: Boolean get() = scenarios.isEmpty()

    /** @returns Index of the [Scenario] that is after the last finished [Scenario] ([LevelProgress.state] > 0), or -1*/
    fun nextScenarioIndex() =
        scenarios.keys.indexOfLast { id -> storySave.progress.levels.any { it.key == id && it.value.state > 0 } } + 1

    /** @returns The [Scenario] after the last [Scenario] with [LevelProgress.state] > 0, or nul */
    fun createNextUndoneScenario(): Scenario? {
        val nextIndex = nextScenarioIndex()
        return if (nextIndex !in 0 until scenarios.size) null else scenarios.values.elementAt(nextIndex).getConstructor().newInstance()
    }

    fun idOf(index: Int): String =
        scenarios.keys.elementAtOrNull(index) ?: throw IndexOutOfBoundsException("No scenario at index of $index")

    fun getScenario(index: Int): Scenario = scenarios.values.elementAtOrNull(index)?.getConstructor()?.newInstance()
        ?: throw IllegalArgumentException("Scenario of id<$id> was not found")

    fun getScenario(id: String): Scenario = scenarios[id]?.getConstructor()?.newInstance()
        ?: throw IllegalArgumentException("Scenario of id<$id> was not found")

    open fun addScenarios() {
        scenarios.clear()
        scenariosAdded = true
    }

    fun applySave(scenario: Scenario) {
        scenario.loadFrom(storySave)
    }

    fun onScenarioOverSave(scenario: Scenario) {
        scenario.saveTo(storySave)
    }
}