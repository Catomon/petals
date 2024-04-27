package ctmn.petals.unit.buff

data class Buff(
    val name: String,
    val value: Int = 0,
    val coefficient: Float = 1f,
    var duration: Float
)