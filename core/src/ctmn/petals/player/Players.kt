package ctmn.petals.player

val newBluePlayer get() = Player("Blue", Player.BLUE, Player.BLUE)

val newRedPlayer get() = Player("Red", Player.RED, Player.RED).apply {
    species = goblin
}