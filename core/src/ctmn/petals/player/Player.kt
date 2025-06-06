package ctmn.petals.player

import com.badlogic.gdx.graphics.Color
import ctmn.petals.Const
import kotlin.random.Random

data class Player(var name: String = "Player${Random.nextInt(0, 999)}", var id: Int = -1, var teamId: Int = -1) {

    companion object Id {
        const val NONE = -1
        const val BLUE = 1
        const val RED = 2
        const val GREEN = 3
        const val PURPLE = 4
        const val YELLOW = 5
        const val ORANGE = 6
        const val PINK = 7
        const val BROWN = 8

        fun colorById(id: Int) : Color? {
            return when (id) {
                NONE -> Color.WHITE
                BLUE -> Color.SKY
                RED -> Color.RED
                YELLOW -> Color.YELLOW
                PURPLE -> Color.PURPLE
                GREEN -> Color.GREEN
                ORANGE -> Color.ORANGE
                PINK -> Color.PINK
                BROWN -> Color.BROWN
                else -> Color.WHITE
            }
        }
    }

    init {
        if (id < -1 || id == 0 || id > 8) throw IllegalStateException("nah")
    }

    var clientId: String? = null
        set(value) {
            if (value == "null")
                throw IllegalArgumentException("player's clientId cannot be \"null\", as it used for not identified clients")

            field = value
        }

    var allies: MutableSet<Int> = mutableSetOf()

    var credits = 0
    var creditsPassiveReserve = Const.PLAYER_CREDITS_RESERVE

    var isOutOfGame = false

    var species = fairy

    var techs: MutableList<String> = mutableListOf()

    fun setFrom(player: Player) : Player {
        name = player.name
        id = player.id
        teamId = player.teamId
        credits = player.credits
        allies = player.allies
        isOutOfGame = player.isOutOfGame
        creditsPassiveReserve = player.creditsPassiveReserve

        return this
    }
}
