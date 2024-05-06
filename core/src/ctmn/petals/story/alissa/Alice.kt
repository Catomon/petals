package ctmn.petals.story.alissa

import ctmn.petals.player.Player

const val ALICE_PLAYER_NAME = "Alissa"
const val ALICE_PLAYER_ID = 1
const val ALICE_TEAM = 1

const val ENEMY_PLAYER_NAME = "Enemy"
const val ENEMY_PLAYER_ID = 2
const val ENEMY_TEAM = 2

val AlicePlayer get() = Player(ALICE_PLAYER_NAME, ALICE_PLAYER_ID, ALICE_TEAM)
