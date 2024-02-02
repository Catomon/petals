package ctmn.petals.player

import com.badlogic.gdx.graphics.Color

class Team(var name: String, var id: Int) {
    companion object Id {
        const val NONE = -1
        const val WHITE = 0
        const val BLUE = 1
        const val RED = 2
        const val YELLOW = 3
        const val PURPLE = 4
        const val GREEN = 5
        const val BLACK = 6
        const val BROWN = 7

        fun getColor(id: Int) : Color {
            return when (id) {
                NONE -> Color.PINK
                WHITE -> Color.WHITE
                BLUE -> Color.BLUE
                RED -> Color.RED
                YELLOW -> Color.YELLOW
                PURPLE -> Color.PURPLE
                GREEN -> Color.GREEN
                BLACK -> Color.BLACK
                BROWN -> Color.BROWN
                else -> Color.PINK
            }
        }
    }
}