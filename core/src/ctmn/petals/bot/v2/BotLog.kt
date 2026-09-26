package ctmn.petals.bot.v2

import ctmn.petals.Config
import ctmn.petals.bot.v2.util.ArmyPlanner
import ctmn.petals.bot.v2.util.UnitRole
import ctmn.petals.bot.v2.util.UnitRoles
import ctmn.petals.player.Species
import ctmn.petals.playscreen.income
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getTiles
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.BaseBuildingComponent
import ctmn.petals.tile.components.CrystalsComponent
import ctmn.petals.tile.isBase
import ctmn.petals.tile.isCrystal
import ctmn.petals.utils.GameLog
import java.util.Locale

/**
 * Formats the bot balance lines that are gated behind the `botlog` run argument.
 *
 * Lines use a stable `[BotBalance]` prefix plus key=value pairs so they can be read by eye
 * and grepped or parsed later. This is the tuning dashboard: map resources, army progression
 * per turn, every buy with its role slot and the runners-up it was scored against.
 */
object BotLog {

    private var matchLogged = false
    private val spent = HashMap<Int, Int>()

    /** Called on level creation, so a new match starts from a clean slate. */
    fun resetMatch() {
        matchLogged = false
        spent.clear()
    }

    /** Map size, crystals and starting bot setup. Logged once per match. */
    fun mapSummaryIfNeeded(battle: Battle) {
        if (!GameLog.balanceEnabled || matchLogged) return
        matchLogged = true

        val field = battle.field
        val map = battle.map
        val width = field.tiledWidth
        val height = field.tiledHeight
        GameLog.balance(
            "match map=\"${map?.mapSave?.name ?: "?"}\" id=${map?.mapId ?: "?"} " +
                "size=${width}x$height area=${width * height}"
        )

        val crystalTiles = field.getTiles().filter { it.isCrystal }
        val maxDeposit = crystalTiles.size * Config.CRYSTALS_CLUSTER
        val deposit = crystalTiles.sumOf {
            it.get(CrystalsComponent::class.java)?.amount ?: Config.CRYSTALS_CLUSTER
        }
        val neutral = crystalTiles.count { it.cPlayerId?.playerId == null }
        GameLog.balance(
            "match crystals=${crystalTiles.size} deposit=$deposit/$maxDeposit " +
                "perTile=${Config.CRYSTALS_CLUSTER} neutral=$neutral"
        )

        val players = battle.turnManager.players
        val baseSummary = players.joinToString(" ") { player ->
            "${player.name}=${field.getCapturablesOf(player).count { it.isBase }}"
        }
        val neutralBases = field.getTiles().count { it.isBase && it.cPlayerId?.playerId == null }
        GameLog.balance("match bases $baseSummary neutral=$neutralBases")

        val bots = battle.botManager.botPlayers.filterIsInstance<AiBot>()
        val botSummary = bots.joinToString(" ") { bot ->
            "${bot.player.name}=${bot.player.species}/${bot.difficulty.displayName}" +
                " credits=${bot.player.credits} income=${bot.player.income(battle)}"
        }
        if (botSummary.isNotEmpty()) GameLog.balance("match bots $botSummary")
    }

    /** Per-turn snapshot: resources, reserves and how full each role slot is. */
    fun turnStart(bot: AiBot) {
        if (!GameLog.balanceEnabled) return

        val ctx = bot.context
        val round = bot.battle.turnManager.round + 1
        val targets = ArmyPlanner.roleTargets(bot)
        val crystals = bot.field.getCapturablesOf(ctx.player).count { it.isCrystal }
        val roles = UnitRole.values().mapNotNull { role ->
            val target = targets[role] ?: 0
            val count = ctx.units.count { UnitRoles.of(it) == role }
            if (target <= 0 && count <= 0) null else "${role.name.lowercase()}=$count/$target"
        }
        GameLog.balance(
            "r=$round ${bot.player.name} START credits=${ctx.player.credits} income=${ctx.income} " +
                "bases=${ctx.ownBases.size} building=${ctx.basesUnderConstruction} " +
                "free=${ctx.freeOwnedBases().size} crystals=$crystals " +
                "units=${ctx.units.size} reserve=${ArmyPlanner.nextTechReserve(bot)} spent=${spent[bot.playerID] ?: 0} " +
                "army=[${roles.joinToString(" ")}]"
        )

        val names = ctx.units.map { it.selfName }.sorted()
        val shown = if (names.size > 14) {
            names.take(14).joinToString(",") + ",+${names.size - 14}"
        } else {
            names.joinToString(",")
        }
        GameLog.balance("r=$round ${bot.player.name} UNITS [$shown]")
    }

    /** A bought unit, the role slot it fills and the scored runners-up. */
    fun buy(bot: AiBot, choice: ArmyPlanner.BuyChoice, base: TileActor, price: Int) {
        if (!GameLog.balanceEnabled) return

        spent[bot.playerID] = (spent[bot.playerID] ?: 0) + price

        val ctx = bot.context
        val targets = ArmyPlanner.roleTargets(bot)
        val target = targets[choice.role] ?: 0
        val count = ctx.units.count { UnitRoles.of(it) == choice.role }
        val alternatives = choice.alternatives.drop(1).take(3)
            .joinToString(",") { "${it.first.selfName}:${format(it.second)}" }
        val via = if (choice.emergencyAir) " via=antiAir" else ""
        GameLog.balance(
            "r=${bot.battle.turnManager.round + 1} ${bot.player.name} BUY ${choice.unit.selfName} " +
                "\$${price} creditsAfter=${ctx.player.credits - price} role=${choice.role} " +
                "slot=${count + 1}/$target score=${format(choice.score)} " +
                "base=${base.tiledX},${base.tiledY}$via alts=[$alternatives]"
        )
    }

    /** A constructed building, its cost and which units it unlocks. */
    fun build(bot: AiBot, buildingName: String, cost: Int, tile: TileActor) {
        if (!GameLog.balanceEnabled) return

        spent[bot.playerID] = (spent[bot.playerID] ?: 0) + cost
        val unlocks = runCatching {
            Species.getSpeciesUnits(bot.player.species)
                .filter { buildingName in it.requiredBuildings }
                .map { it.unitActor.selfName }
        }.getOrDefault(emptyList())
        val unlockText = if (unlocks.isEmpty()) "" else " unlocks=[${unlocks.joinToString(",")}]"
        GameLog.balance(
            "r=${bot.battle.turnManager.round + 1} ${bot.player.name} BUILD $buildingName " +
                "\$${cost} creditsAfter=${bot.player.credits - cost} at=${tile.tiledX},${tile.tiledY}$unlockText"
        )
    }

    /** A constructed base. */
    fun buildBase(bot: AiBot, cost: Int, tile: TileActor) {
        if (!GameLog.balanceEnabled) return

        spent[bot.playerID] = (spent[bot.playerID] ?: 0) + cost
        val building = bot.field.getTiles().count { t ->
            t.get(BaseBuildingComponent::class.java)?.playerId == bot.playerID
        }
        GameLog.balance(
            "r=${bot.battle.turnManager.round + 1} ${bot.player.name} BASE " +
                "\$${cost} creditsAfter=${bot.player.credits - cost} at=${tile.tiledX},${tile.tiledY} " +
                "bases=${bot.context.ownBases.size} building=$building"
        )
    }

    /** The bot ran out of things to do this turn. */
    fun noActions(bot: AiBot) {
        if (!GameLog.balanceEnabled) return
        GameLog.balance("r=${bot.battle.turnManager.round + 1} ${bot.player.name} IDLE no actions left")
    }

    /** The INSANE cheat payout. */
    fun incomeBonus(bot: AiBot, amount: Int) {
        if (!GameLog.balanceEnabled) return
        GameLog.balance(
            "r=${bot.battle.turnManager.round + 1} ${bot.player.name} CHEAT " +
                "+${amount} credits=${bot.player.credits}"
        )
    }

    /** What the bot actually did during its turn. */
    fun turnEnd(bot: AiBot, actions: Map<String, Int>) {
        if (!GameLog.balanceEnabled || actions.isEmpty()) return
        val summary = actions.entries.sortedBy { it.key }.joinToString(" ") { "${it.key}=${it.value}" }
        GameLog.balance(
            "r=${bot.battle.turnManager.round + 1} ${bot.player.name} END cmds $summary " +
                "spentTotal=${spent[bot.playerID] ?: 0}"
        )
    }

    private fun format(value: Float): String = String.format(Locale.ROOT, "%.2f", value)
}
