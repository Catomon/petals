package playScreen

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const
import ctmn.petals.player.Player
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.TileCapturedEvent
import ctmn.petals.playscreen.listeners.TurnsCycleListener
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.*
import ctmn.petals.tile.*
import ctmn.petals.tile.components.*
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.ObjBlob
import ctmn.petals.unit.actors.ObjRoot
import ctmn.petals.unit.component.*

class PlayTurnCycleListener(private val playScreen: PlayScreen) : EventListener {
    override fun handle(event: Event): Boolean {
        val turnCycleEvent = if (event is TurnsCycleListener.TurnCycleEvent) event else return false
        val nextPlayer = turnCycleEvent.nextPlayer

        // calculate income
        if (!nextPlayer.isOutOfGame) {
            //passive income
            if (nextPlayer.creditsPassiveReserve > 0) {
                nextPlayer.creditsPassiveReserve -= playScreen.creditsPassiveIncome
                if (nextPlayer.creditsPassiveReserve < 0) {
                    nextPlayer.credits += playScreen.creditsPassiveIncome - nextPlayer.creditsPassiveReserve
                    nextPlayer.creditsPassiveReserve = 0
                } else {
                    nextPlayer.credits += playScreen.creditsPassiveIncome
                }
            }

            //capturables income
            for (capturable in playScreen.playStage.getCapturablesOf(nextPlayer)) {
                if (capturable.isCrystal) {
                    val cCrystals = capturable.get(CrystalsComponent::class.java) ?: CrystalsComponent().also {
                        capturable.add(it)
                    }

                    if (cCrystals.amount > 0) {
                        cCrystals.amount -= playScreen.creditsPerCluster
                        if (cCrystals.amount < 0) {
                            nextPlayer.credits += playScreen.creditsPerCluster - cCrystals.amount
                            cCrystals.amount = 0
                        } else {
                            nextPlayer.credits += playScreen.creditsPerCluster
                        }
                    }

                    if (cCrystals.amount <= 0) {
                        playScreen.playStage.removeTileSafely(capturable)
                    }
                }
            }
        }

        updateUnits(turnCycleEvent)

        updateTilesCaptureBuildDestroy(turnCycleEvent, nextPlayer)

        //summoner component
        for (unit in playScreen.playStage.getUnitsOfPlayer(turnCycleEvent.lastPlayer)) {
            unit.cSummoner?.giveAP = false
        }

        //heal units that are near their leader
        for (unit in playScreen.playStage.getUnitsOfPlayer(turnCycleEvent.nextPlayer)) {
            if (unit.isUnitNear(playScreen.playStage.getLeadUnit(unit.followerID) ?: continue, 1))
                if (unit.health < unit.unitComponent.baseHealth)
                    unit.heal(Const.HEALING_AMOUNT_NEAR_LEADER)
        }

        //apply healing from bonus fields
        playScreen.playStage.getUnitsForTeam(turnCycleEvent.nextPlayer.teamId)
            .mapNotNull { it.get(BonusFieldComponent::class.java)?.to(it) }.forEach { pair ->
                val field = pair.first
                val fieldUnit = pair.second
                playScreen.playStage.getUnitsOfPlayer(turnCycleEvent.nextPlayer).forEach { unit ->
                    if (unit != fieldUnit && unit.isInRange(fieldUnit.tiledX, fieldUnit.tiledY, field.range)) {
                        if (field.healing > 0)
                            unit.heal(field.healing)
                    }
                }
            }

        playScreen.playStage.getUnits().forEach { unit ->
            if (!unit.isAir) {
                val cTileEff = unit.get(TileEffectComponent::class.java)
                if (cTileEff != null && cTileEff.playerId == turnCycleEvent.nextPlayer.id) {
                    when (playScreen.playStage.getTile(unit.tiledX, unit.tiledY)?.terrain) {
                        TerrainNames.lava -> {
                            unit.dealDamage(Damage.LAVA, playScreen = playScreen)
                        }

                        TerrainNames.chasm -> {
                            unit.dealDamage(Damage.CHASM, playScreen = playScreen)
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
        for (unit in playScreen.playStage.getUnits()) {
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
                    unit.dealDamage(Damage.BURN, playScreen = playScreen)
                if (playScreen.playStage.getTile(unit.tiledX, unit.tiledY)?.isBurning == true)
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
                    unit.actionPoints = Const.ACTION_POINTS

                //update abilities cooldowns
                if (unit.cAbilities != null)
                    for (ability in unit.cAbilities!!.abilities) {
                        if (ability.currentCooldown > 0)
                            ability.currentCooldown -= 1
                        else
                            ability.castAmountsLeft = ability.castAmounts
                    }

                //add mana for crystals
                if (playScreen.gameMode == GameMode.CRYSTALS) {
                    if (unit.isLeader && unit.cAbilities != null) {
                        for (tile in playScreen.playStage.getTiles()) {
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
        for (tile in Array.ArrayIterator(playScreen.playStage.getTiles())) {
            if (tile.isCapturable && tile.cCapturing?.playerId == turnCycleEvent.nextPlayer.id) {
                val cCapturing = tile.cCapturing!!
                val unitOnTile = playScreen.playStage.getUnit(tile.tiledX, tile.tiledY)
                if (unitOnTile != null && tile.cCapturing!!.playerId == unitOnTile.playerId) {
                    cCapturing.turns -= 1
                    if (cCapturing.turns <= 0) {
                        unitOnTile.captureBase(tile, playScreen.turnManager.getPlayerById(unitOnTile.playerId))

                        if (Const.REMOVE_UNIT_AFTER_CAPTURE)
                            unitOnTile.remove()

                        tile.components.remove(CapturingComponent::class.java)

                        playScreen.playStage.root.fire(TileCapturedEvent(tile))
                    } else {
                        unitOnTile.actionPoints = 0
                    }
                } else {
                    tile.components.remove(CapturingComponent::class.java)
                }
            }

            if (tile.cBaseBuilding?.playerId == turnCycleEvent.nextPlayer.id) {
                val cBaseBuilding = tile.cBaseBuilding!!
                val unitOnTile = playScreen.playStage.getUnit(tile.tiledX, tile.tiledY)
                if (unitOnTile != null && cBaseBuilding.playerId == unitOnTile.playerId) {
                    cBaseBuilding.turns -= 1
                    if (cBaseBuilding.turns <= 0) {
                        val newBase =
                            TileActor(TileData.get(Tiles.PIXIE_NEST_BLUE)!!, tile.layer, tile.tiledX, tile.tiledY)
                        tile.layer -= 1
                        val tile2 = playScreen.playStage.getTile(tile.tiledX, tile.tiledY, tile.layer)
                        if (tile2 != null) {
                            tile2.layer -= 1
                            tile2.remove()
                            playScreen.playStage.addActor(tile2)
                        }
                        tile.remove()
                        playScreen.playStage.addActor(tile)
                        playScreen.playStage.addActor(newBase)
                        unitOnTile.captureBase(newBase, playScreen.turnManager.getPlayerById(unitOnTile.playerId))
                        unitOnTile.remove()
                        tile.components.remove(BaseBuildingComponent::class.java)
                        //playStage.root.fire(BaseBuilt(tile))
                    } else {
                        unitOnTile.actionPoints = 0
                    }
                } else {
                    tile.components.remove(BaseBuildingComponent::class.java)
                }
            }

            if (tile.cDestroying?.playerId == turnCycleEvent.nextPlayer.id) {
                val unitOnTile = playScreen.playStage.getUnit(tile.tiledX, tile.tiledY)
                if (unitOnTile != null && tile.cDestroying!!.playerId == unitOnTile.playerId) {
                    playScreen.playStage.removeTileSafely(tile)
                    //todo debris
                }
                tile.components.remove(DestroyingComponent::class.java)
            }

            // remove cooldown
            if (tile.isBase) {
                tile.del(ActionCooldown::class.java)
            }

            //
            if (tile.isCrystal) {
                val unitOnTile = playScreen.playStage.getUnit(tile.tiledX, tile.tiledY)
                if (unitOnTile?.playerId == nextPlayer.id) {
                    when (unitOnTile.selfName) {
                        UnitIds.SLIME_LING, UnitIds.SLIME, UnitIds.SLIME_HUGE, UnitIds.SLIME_BIG, UnitIds.SLIME_TINY -> {
                            if (tile.selfName != Tiles.CRYSTAL_SLIME) {
                                tile.tileComponent.name = Tiles.CRYSTAL_SLIME
                                tile.del(PlayerIdComponent::class.java)
                                tile.initView()

                                playScreen.fireEvent(TileCapturedEvent(tile))
                            }
                        }

                        UnitIds.EVIL_TREE, UnitIds.ROOT_TREE -> {
                            if (tile.selfName != Tiles.CRYSTAL_ROOT) {
                                tile.tileComponent.name = Tiles.CRYSTAL_ROOT
                                tile.del(PlayerIdComponent::class.java)
                                tile.initView()

                                playScreen.fireEvent(TileCapturedEvent(tile))
                            }
                        }
                    }
                }

                if (playScreen.playStage.getUnit(tile) == null) {
                    when (tile.selfName) {
                        Tiles.CRYSTAL_SLIME -> {
                            playScreen.playStage.addActor(ObjBlob().player(8, 4).position(tile))

                            tile.tileComponent.name = Tiles.CRYSTAL
                            tile.del(PlayerIdComponent::class.java)
                            tile.initView()
                            playScreen.fireEvent(TileCapturedEvent(tile))
                        }

                        Tiles.CRYSTAL_ROOT -> {
                            playScreen.playStage.addActor(ObjRoot().player(8, 4).position(tile))

                            tile.tileComponent.name = Tiles.CRYSTAL
                            tile.del(PlayerIdComponent::class.java)
                            tile.initView()
                            playScreen.fireEvent(TileCapturedEvent(tile))
                        }
                    }
                }
            }
        }
    }
}