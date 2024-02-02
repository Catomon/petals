package ctmn.petals.story.alissa

class AlissaSave(
    var level: Int = 1,
    var exp: Int = 0,
    val faeries: MutableSet<String> = mutableSetOf(),
    val abilities: MutableSet<String> = mutableSetOf(),
)