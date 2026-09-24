package ctmn.petals.bot.v2

import com.badlogic.gdx.utils.Array
import ctmn.petals.Config
import ctmn.petals.player.Player
import ctmn.petals.player.Species
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getBases
import ctmn.petals.playstage.getBuildings
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.isCapturable
import ctmn.petals.tile.isCrystal
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cShop
import ctmn.petals.unit.canBuildBase
import ctmn.petals.unit.canCapture
import ctmn.petals.unit.canDestroy
import ctmn.petals.unit.isWorker

/**
 * What the current map actually allows this player to do. This is what lets one bot handle
 * full-economy PvE battles and no-building campaign maps without separate classes.
 */
class BotCapabilities(
    val hasWorkers: Boolean,
    val hasOwnedBase: Boolean,
    val hasCapturable: Boolean,
    val canBuy: Boolean,
    val canBuildBase: Boolean,
    val canBuildBuildings: Boolean,
    val canCapture: Boolean,
    val canDestroy: Boolean,
) {
    val hasEconomy: Boolean
        get() = canBuy || canBuildBase || canBuildBuildings || canCapture

    companion object {
        fun detect(
            player: Player,
            playScreen: PlayScreen,
            speciesUnits: Array<UnitActor>,
            forceCombatOnly: Boolean,
        ): BotCapabilities {
            if (forceCombatOnly) {
                return BotCapabilities(
                    hasWorkers = false,
                    hasOwnedBase = false,
                    hasCapturable = false,
                    canBuy = false,
                    canBuildBase = false,
                    canBuildBuildings = false,
                    canCapture = false,
                    canDestroy = false,
                )
            }

            val playStage = playScreen.playStage
            val units = playStage.getUnitsOfPlayer(player).toList()

            val workers = units.filter { it.isWorker }
            val hasWorkers = workers.isNotEmpty() || speciesUnits.any { it.isWorker }

            val ownedBases = playStage.getBases(player)
            val hasOwnedBase = ownedBases.size > 0

            val capturables = playStage.getTiles().filter { it.isCapturable && it.canBeCapturedBy(player) }
            val hasCapturable = capturables.isNotEmpty()

            val hasShopUnits = speciesUnits.any { it.cShop != null }

            val buildableTerrain = playStage.getTiles().any {
                it.terrain == TerrainNames.grass || it.terrain == TerrainNames.water
            }

            val hasBuildings = runCatching {
                Species.getSpeciesBuildings(player.species).size > 0
            }.getOrDefault(false)

            return BotCapabilities(
                hasWorkers = hasWorkers,
                hasOwnedBase = hasOwnedBase,
                hasCapturable = hasCapturable,
                canBuy = hasShopUnits && hasOwnedBase && player.credits > 0,
                canBuildBase = hasWorkers && buildableTerrain,
                canBuildBuildings = hasWorkers && hasBuildings,
                canCapture = hasWorkers && hasCapturable,
                canDestroy = units.any { it.canDestroy() },
            )
        }
    }
}

private fun ctmn.petals.tile.TileActor.canBeCapturedBy(player: Player): Boolean {
    val owner = get(ctmn.petals.tile.components.PlayerIdComponent::class.java)?.playerId ?: Player.NONE
    return owner != player.id
}
