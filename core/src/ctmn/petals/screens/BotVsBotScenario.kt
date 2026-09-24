package ctmn.petals.screens

import com.badlogic.gdx.utils.Array
import ctmn.petals.Config
import ctmn.petals.PetalsGame
import ctmn.petals.bot.v2.BotFactory
import ctmn.petals.bot.v2.Difficulty
import ctmn.petals.map.loadMap
import ctmn.petals.player.Player
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.PlayScreen

/**
 * Dev / spectate scenario: two [ctmn.petals.bot.v2.AiBot]s fight on a fixed map while the
 * local player sits the match out.
 *
 * Started by the desktop launcher argument `botvsbot` (optionally plus `easy`/`normal`/`hard`):
 * `./gradlew :desktop:run --args="botvsbot hard"`
 *
 * The map only has two bases, so the third player owns nothing, gets marked out of game and
 * the turn cycle skips them. That leaves a clean bot vs bot match to watch.
 */
object BotVsBotScenario {

    const val MAP_NAME = "2L_Blooming_Mountains"

    fun createFromConfig(game: PetalsGame): PlayScreen {
        val difficulty = when (Config.botTestDifficulty.lowercase()) {
            "easy" -> Difficulty.EASY
            "hard" -> Difficulty.HARD
            else -> Difficulty.NORMAL
        }
        return create(game, difficulty)
    }

    fun create(game: PetalsGame, difficulty: Difficulty = Difficulty.NORMAL): PlayScreen {
        val blue = newBluePlayer
        val red = newRedPlayer
        val spectator = Player("Spectator", Player.GREEN, Player.GREEN)

        val players = Array<Player>().apply {
            add(blue)
            add(red)
            add(spectator)
        }

        val playScreen = PlayScreenTemplate.pvp(
            game,
            loadMap(MAP_NAME),
            players,
            GameType.PVP_SAME_SCREEN,
            CaptureBases(),
            GameMode.CRYSTALS,
            spectator,
        ).apply {
            botManager.add(BotFactory.create(blue, this, difficulty))
            botManager.add(BotFactory.create(red, this, difficulty))

            fogOfWarManager.drawFog = false
        }

        playScreen.setDefaultPlayerIdToLabels()
        playScreen.ready()

        return playScreen
    }
}