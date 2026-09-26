package playScreen

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Array
import ctmn.petals.Config
import ctmn.petals.player.Player
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.events.CreditsChangeEvent
import ctmn.petals.playscreen.events.TileCapturedEvent
import ctmn.petals.playscreen.listeners.TurnsCycleListener
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.*
import ctmn.petals.sim.Battle
import ctmn.petals.tile.*
import ctmn.petals.tile.components.*
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.ObjBlob
import ctmn.petals.unit.actors.ObjRoot
import ctmn.petals.unit.component.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.getSurroundingTiles

class PlayTurnCycleListener(private val battle: Battle) : EventListener {
    override fun handle(event: Event): Boolean {
        val turnCycleEvent = if (event is TurnsCycleListener.TurnCycleEvent) event else return false
        val nextPlayer = turnCycleEvent.nextPlayer

        // calculate income
        if (!nextPlayer.isOutOfGame) {
            //passive income
            if (nextPlayer.creditsPassiveReserve > 0) {
                nextPlayer.creditsPassiveReserve -= battle.creditsPassiveIncome
                if (nextPlayer.creditsPassiveReserve < 0) {
                    nextPlayer.credits += battle.creditsPassiveIncome - nextPlayer.creditsPassiveReserve
                    nextPlayer.creditsPassiveReserve = 0
                } else {
                    nextPlayer.credits += battle.creditsPassiveIncome
                }
            }

            val capturables = battle.field.getCapturablesOf(nextPlayer)
            val creditsPerCluster = battle.creditsPerCluster
            //capturables income
            for (capturable in capturables) {
                if (capturable.isCrystal) {
                    val cCrystals = capturable.get(CrystalsComponent::class.java) ?: CrystalsComponent().also {
                        capturable.add(it)
                    }

                    fun showLabel(amount: Int) {
                        battle.showFloatingIconLabel("+$amount", "credits", capturable.centerX, capturable.centerY)
                        battle.fireEvent(CreditsChangeEvent(nextPlayer, amount))
                    }

                    if (cCrystals.amount > 0) {
                        cCrystals.amount -= creditsPerCluster
                        if (cCrystals.amount < 0) {
                            val resultingAmount = creditsPerCluster - cCrystals.amount
                            nextPlayer.credits += resultingAmount
                            cCrystals.amount = 0

                            showLabel(resultingAmount)
                        } else {
                            nextPlayer.credits += creditsPerCluster

                            showLabel(creditsPerCluster)
                        }
                    }

                    if (cCrystals.amount <= 0) {
                        battle.field.removeTileSafely(capturable)
                    }
                }
            }
        }

        updateUnits(turnCycleEvent)

        updateTilesCaptureBuildDestroy(turnCycleEvent, nextPlayer)

        //summoner component
        for (unit in battle.field.getUnitsOfPlayer(turnCycleEvent.lastPlayer)) {
            unit.cSummoner?.giveAP = false
        }

        //heal units that are near their leader
        for (unit in battle.field.getUnitsOfPlayer(turnCycleEvent.nextPlayer)) {
            if (unit.isUnitNear(battle.field.getLeadUnit(unit.followerID) ?: continue, 1))
                if (unit.health < unit.unitComponent.baseHealth)
                    unit.heal(Config.HEALING_AMOUNT_NEAR_LEADER)
        }

        //apply healing from bonus fields
        battle.field.getUnitsForTeam(turnCycleEvent.nextPlayer.teamId)
            .mapNotNull { it.get(BonusFieldComponent::class.java)?.to(it) }.forEach { pair ->
                val field = pair.first
                val fieldUnit = pair.second
                battle.field.getUnitsOfPlayer(turnCycleEvent.nextPlayer).forEach { unit ->
                    if (unit != fieldUnit && unit.isInRange(fieldUnit.tiledX, fieldUnit.tiledY, field.range)) {
                        if (field.healing > 0)
                            unit.heal(field.healing)
                    }
                }
            }

        battle.field.getUnits().forEach { unit ->
            if (!unit.isAir) {
                val cTileEff = unit.get(TileEffectComponent::class.java)
                if (cTileEff != null && cTileEff.playerId == turnCycleEvent.nextPlayer.id) {
                    when (battle.field.getTile(unit.tiledX, unit.tiledY)?.terrain) {
                        TerrainNames.lava -> {
                            unit.dealDamage(Damage.LAVA, battle = battle)
                        }

                        TerrainNames.chasm -> {
                            unit.dealDamage(Damage.CHASM, battle = battle)
                        }

                        else -> {
                            unit.del(TileEffectComponent::class.java)
                        }
                    }
                }
            }
        }

        return false
    }

    private fun updateUnits(turnCycleEvent: TurnsCycleListener.TurnCycleEvent) {
        for (unit in battle.field.getUnits()) {
            //put ability on cooldown if it was partially cast
            val abilities = unit.cAbilities?.abilities
            if (abilities != null)
                for (ability in abilities)
                    if (ability.castAmountsLeft < ability.castAmounts) {
                        ability.castAmountsLeft = ability.castAmounts
                        ability.currentCooldown = ability.cooldown
                    }

            //update burn
            unit.get(BurningComponent::class.java)?.let {
                it.duration -= turnCycleEvent.turnCycleTime
                if (it.playerId == turnCycleEvent.nextPlayer.id)
                    unit.dealDamage(Damage.BURN, battle = battle)
                if (battle.field.getTile(unit.tiledX, unit.tiledY)?.isBurning == true)
                    it.duration = Damage.BURN_DURATION
                if (it.duration <= 0) {
                    unit.del(it)
                }
            }

            //update buffs duration
            val iterator = unit.buffs.iterator()
            while (iterator.hasNext()) {
                val buff = iterator.next()
                buff.duration -= turnCycleEvent.turnCycleTime

                if (buff.duration <= 0) {
                    iterator.remove()
                    unit.updateView()
                }
            }

            //update units of next player
            if (unit.playerId == turnCycleEvent.nextPlayer.id) {
                unit.get(MoveAfterAttackComponent::class.java)?.apply {
                    attacked = false
                    unit.cUnit.movingRange = normalRange
                }

                //update reloading
                unit.get(ReloadingComponent::class.java)?.apply {
                    currentTurns -= 1
                }

                //reset units ap
                if (unit.buffs.find { buff -> buff.name == "freeze" } == null)
                    unit.actionPoints = Config.ACTION_POINTS

                //update abilities cooldowns
                if (unit.cAbilities != null)
                    for (ability in unit.cAbilities!!.abilities) {
                        if (ability.currentCooldown > 0)
                            ability.currentCooldown -= 1
                        else
                            ability.castAmountsLeft = ability.castAmounts
                    }

                //add mana for crystals
                if (battle.gameMode == GameMode.CRYSTALS) {
                    if (unit.isLeader && unit.cAbilities != null) {
                        for (tile in battle.field.getTiles()) {
                            if (tile.terrain == "base"
                                && tile.name.contains("crystal")
                                && tile.cPlayerId?.playerId == unit.playerId
                            ) {
                                unit.mana += 5
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateTilesCaptureBuildDestroy(
        turnCycleEvent: TurnsCycleListener.TurnCycleEvent,
        nextPlayer: Player,
    ) {
        for (tile in Array.ArrayIterator(battle.field.getTiles())) {
            updateCapturing(tile, turnCycleEvent)

            updateBaseBuilding(tile, turnCycleEvent)

            updateBuilding(tile, turnCycleEvent)

            updateDestroying(tile, turnCycleEvent)

            removeBaseBuyCooldown(tile)

            garbageByNeutrals(tile, nextPlayer)
        }
    }

    private fun garbageByNeutrals(tile: TileActor, nextPlayer: Player) {
        if (tile.isCrystal) {
            val unitOnTile = battle.field.getUnit(tile.tiledX, tile.tiledY)
            if (unitOnTile?.playerId == nextPlayer.id) {
                when (unitOnTile.selfName) {
                    UnitIds.SLIME_LING, UnitIds.SLIME, UnitIds.SLIME_HUGE, UnitIds.SLIME_BIG, UnitIds.SLIME_TINY -> {
                        if (tile.selfName != Tiles.CRYSTAL_SLIME) {
                            tile.tileComponent.name = Tiles.CRYSTAL_SLIME
                            tile.del(PlayerIdComponent::class.java)
                            tile.initView()

                            battle.fireEvent(TileCapturedEvent(tile))
                        }
                    }

                    UnitIds.EVIL_TREE, UnitIds.ROOT_TREE -> {
                        if (tile.selfName != Tiles.CRYSTAL_ROOT) {
                            tile.tileComponent.name = Tiles.CRYSTAL_ROOT
                            tile.del(PlayerIdComponent::class.java)
                            tile.initView()

                            battle.fireEvent(TileCapturedEvent(tile))
                        }
                    }
                }
            }

            if (battle.field.getUnit(tile) == null) {
                when (tile.selfName) {
                    Tiles.CRYSTAL_SLIME -> {
                        battle.field.addGameActor(ObjBlob().player(8, 4).position(tile))

                        tile.tileComponent.name = Tiles.CRYSTAL
                        tile.del(PlayerIdComponent::class.java)
                        tile.initView()
                        battle.fireEvent(TileCapturedEvent(tile))
                    }

                    Tiles.CRYSTAL_ROOT -> {
                        battle.field.addGameActor(ObjRoot().player(8, 4).position(tile))

                        tile.tileComponent.name = Tiles.CRYSTAL
                        tile.del(PlayerIdComponent::class.java)
                        tile.initView()
                        battle.fireEvent(TileCapturedEvent(tile))
                    }
                }
            }
        }
    }

    private fun removeBaseBuyCooldown(tile: TileActor) {
        if (tile.isBase) {
            tile.del(ActionCooldown::class.java)
        }
    }

    private fun updateCapturing(
        tile: TileActor,
        turnCycleEvent: TurnsCycleListener.TurnCycleEvent,
    ) {
        if (tile.isCapturable && tile.cCapturing?.playerId == turnCycleEvent.nextPlayer.id) {
            val cCapturing = tile.cCapturing!!
            val unitOnTile = battle.field.getUnit(tile.tiledX, tile.tiledY)
            if (unitOnTile != null && tile.cCapturing!!.playerId == unitOnTile.playerId) {
                cCapturing.turns -= 1
                if (cCapturing.turns <= 0) {
                    unitOnTile.captureBase(tile, battle.turnManager.getPlayerById(unitOnTile.playerId))

                    if (Config.REMOVE_UNIT_AFTER_CAPTURE)
                        battle.field.removeGameActor(unitOnTile)

                    tile.components.remove(CapturingComponent::class.java)

                    battle.fireEvent(TileCapturedEvent(tile))
                } else {
                    unitOnTile.actionPoints = 0
                }

                unitOnTile.updateView()
            } else {
                tile.components.remove(CapturingComponent::class.java)
            }
        }
    }

    private fun updateDestroying(
        tile: TileActor,
        turnCycleEvent: TurnsCycleListener.TurnCycleEvent,
    ) {
        if (tile.cDestroying?.playerId == turnCycleEvent.nextPlayer.id) {
            val unitOnTile = battle.field.getUnit(tile.tiledX, tile.tiledY)
            if (unitOnTile != null && tile.cDestroying!!.playerId == unitOnTile.playerId) {
                //battle.field.removeTileSafely(tile)
                battle.field.destroyTile(tile)
            }
            tile.components.remove(DestroyingComponent::class.java)


        }
    }

    private fun updateBaseBuilding(
        tile: TileActor,
        turnCycleEvent: TurnsCycleListener.TurnCycleEvent,
    ) {
        if (tile.cBaseBuilding?.playerId == turnCycleEvent.nextPlayer.id) {
            val cBaseBuilding = tile.cBaseBuilding!!
            val unitOnTile = battle.field.getUnit(tile.tiledX, tile.tiledY)
            if (unitOnTile != null && cBaseBuilding.playerId == unitOnTile.playerId) {
                cBaseBuilding.turns -= 1
                if (cBaseBuilding.turns <= 0) {
                    val newBase =
                        TileActor(TileData.get(Tiles.PIXIE_NEST_BLUE)!!, tile.layer, tile.tiledX, tile.tiledY)
                    tile.layer -= 1
                    val tile2 = battle.field.getTile(tile.tiledX, tile.tiledY, tile.layer)
                    if (tile2 != null) {
                        tile2.layer -= 1
                        battle.field.removeGameActor(tile2)
                        battle.field.addGameActor(tile2)
                    }
                    battle.field.removeGameActor(tile)
                    battle.field.addGameActor(tile)
                    battle.field.addGameActor(newBase)
                    unitOnTile.captureBase(newBase, battle.turnManager.getPlayerById(unitOnTile.playerId))
                    battle.field.removeGameActor(unitOnTile)
                    tile.components.remove(BaseBuildingComponent::class.java)
                    //fire(BaseBuilt(tile))
                } else {
                    unitOnTile.actionPoints = 0
                }

                unitOnTile.updateView()
            } else {
                tile.components.remove(BaseBuildingComponent::class.java)
            }
        }
    }

    private fun updateBuilding(
        tile: TileActor,
        turnCycleEvent: TurnsCycleListener.TurnCycleEvent,
    ) {
        if (tile.get(BuildingComponent::class.java)?.playerId == turnCycleEvent.nextPlayer.id) {
            val cBuilding = tile.get(BuildingComponent::class.java)!!
            val unitOnTile = battle.field.getUnit(tile.tiledX, tile.tiledY)
            if (unitOnTile != null && cBuilding.playerId == unitOnTile.playerId) {
                cBuilding.turns -= 1
                if (cBuilding.turns <= 0) {
                    val buildingTile =
                        TileActor(TileData.get(cBuilding.buildingName)!!, tile.layer, tile.tiledX, tile.tiledY)
                    tile.layer -= 1
                    val tile2 = battle.field.getTile(tile.tiledX, tile.tiledY, tile.layer)
                    if (tile2 != null) {
                        tile2.layer -= 1
                        battle.field.removeGameActor(tile2)
                        battle.field.addGameActor(tile2)
                    }
                    battle.field.removeGameActor(tile)
                    tile.components.remove(BuildingComponent::class.java)
                    battle.field.addGameActor(tile)

                    battle.field.addGameActor(buildingTile)
                    buildingTile.add(PlayerIdComponent(cBuilding.playerId))
                    if (buildingTile.isCombinable) {
                        Decorator.combineTile(buildingTile)
                            battle.field.getSurroundingTiles(tile.tiledX, tile.tiledY).forEach {
                                if (buildingTile.terrain == it.terrain) //todo
                                Decorator.combineTile(it)
                            }
                    }
                } else {
                    unitOnTile.actionPoints = 0
                }

                unitOnTile.updateView()
            } else {
                tile.components.remove(BuildingComponent::class.java)
            }
        }
    }
}
