package ctmn.petals.story.alissa

class CharacterSave(
    var name: String,
    var level: Int = 1,
    var exp: Int = 0,
    val abilities: MutableSet<String> = mutableSetOf(),
)