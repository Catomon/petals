package ctmn.petals.story

class Progress(
    val levels: HashMap<String, LevelProgress> = hashMapOf()
) {
    override fun toString(): String {
        return "Progress[${levels.entries.joinToString()}]"
    }
}

data class LevelProgress(
    var state: Int
)