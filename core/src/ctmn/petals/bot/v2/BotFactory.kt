package ctmn.petals.bot.v2

import com.badlogic.gdx.utils.Array
import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.UnitActor

/** Convenience entry points so call sites do not have to know about [AiBotSettings]. */
object BotFactory {

    fun create(
        player: Player,
        playScreen: PlayScreen,
        difficulty: Difficulty = Difficulty.NORMAL,
        speciesUnits: Array<UnitActor>? = null,
    ): AiBot = AiBot(
        player,
        playScreen,
        AiBotSettings(difficulty = difficulty, speciesUnits = speciesUnits),
    )

    fun create(
        player: Player,
        playScreen: PlayScreen,
        speciesUnits: Array<UnitActor>,
    ): AiBot = AiBot(
        player,
        playScreen,
        AiBotSettings(speciesUnits = speciesUnits),
    )

    /** Scripted campaign opponents used to be weak roaming bots. */
    fun campaign(
        player: Player,
        playScreen: PlayScreen,
        permaAgro: Boolean = true,
        agroRange: Int = 6,
        roamingIfNoAgro: Boolean = false,
        roamingMaxRange: Int = 3,
        attackBuildings: Boolean = false,
    ): AiBot = AiBot(
        player,
        playScreen,
        AiBotSettings(
            difficulty = Difficulty.EASY,
            permaAgro = permaAgro,
            agroRange = agroRange,
            roamingIfNoAgro = roamingIfNoAgro,
            roamingMaxRange = roamingMaxRange,
            attackBuildings = attackBuildings,
        ),
    )

    /** A combat-only bot for maps without resources or buildings. */
    fun combatOnly(player: Player, playScreen: PlayScreen): AiBot = AiBot(
        player,
        playScreen,
        AiBotSettings(forceCombatOnly = true),
    )
}
