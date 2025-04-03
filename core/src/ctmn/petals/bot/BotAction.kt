package ctmn.petals.bot

abstract class BotAction {

    open val defaultPriority: Int = 50

    var priority: Int = 0

    companion object {
        const val IMPOSSIBLE = 0
    }

    abstract fun evaluate(): Int

    abstract fun execute(): Boolean
}