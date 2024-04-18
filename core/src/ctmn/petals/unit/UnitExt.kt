package ctmn.petals.unit

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import ctmn.petals.Const
import ctmn.petals.assets
import ctmn.petals.effects.HealthChangeEffect
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.tile.isOccupied
import ctmn.petals.tile.isPassableAndFree
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.actors.Alice
import ctmn.petals.unit.component.*
import ctmn.petals.utils.tiled
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import ctmn.petals.effects.CreateEffect
import ctmn.petals.effects.FloatingLabelAnimation
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playscreen.events.UnitLevelUpEvent
import ctmn.petals.playscreen.seqactions.KillUnitAction
import ctmn.petals.playscreen.seqactions.ThrowUnitAction
import ctmn.petals.playstage.*
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.setPlayerForCapturableTile
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import java.util.*

val cUnitMapper: ComponentMapper<UnitComponent> = ComponentMapper.getFor(UnitComponent::class.java)
val cBarrierMapper: ComponentMapper<BarrierComponent> = ComponentMapper.getFor(BarrierComponent::class.java)

val Alice.summonRange get() = summonAbility.range
val Alice.credits get() = cAbilities?.mana ?: -1
val Alice.summonAbility get() = abilities.first { it is SummonAbility } as SummonAbility

val UnitActor.cSummoner get() = get(SummonerComponent::class.java)
val UnitActor.summoner get() = cSummoner!!

val UnitActor.isLeader get() = has(LeaderComponent::class.java)
val UnitActor.isFollower get() = has(FollowerComponent::class.java)

//component getters
val UnitActor.cUnit: UnitComponent get() = unitComponent
val UnitActor.cAbilities get() = get(AbilitiesComponent::class.java)
val UnitActor.cAnimationView get() = get(AnimationViewComponent::class.java)
val UnitActor.cAttack get() = get(AttackComponent::class.java)
val UnitActor.cFollower get() = get(FollowerComponent::class.java)
val UnitActor.cLeader get() = get(LeaderComponent::class.java)
val UnitActor.cMatchUp get() = get(MatchUpBonusComponent::class.java)?.bonuses
val UnitActor.cSpriteView get() = get(SpriteViewComponent::class.java)
val UnitActor.cTerrainProps get() = get(TerrainPropComponent::class.java)?.props
val UnitActor.cShop get() = get(ShopComponent::class.java)
//val UnitActor.cSummoner get() = get(SummonerComponent::class.java)
val UnitActor.cLevel get() = get(LevelComponent::class.java)
//val UnitActor.cAlice get() = get(AliceComponent::class.java)
val UnitActor.cSummonable get() = get(SummonableComponent::class.java)

//component prop getters
var UnitActor.health get() = cUnit.health; set(value) { cUnit.health = value }
var UnitActor.defense get() = cUnit.defense; set(value) { cUnit.defense = value }
var UnitActor.playerId get() = cUnit.playerID; set(value) { cUnit.playerID = value }
var UnitActor.teamId get() = cUnit.teamID; set(value) { cUnit.teamID = value }
var UnitActor.tiledX get() = cUnit.tiledX; set(value) { cUnit.tiledX = value; playStageOrNull?.updateCaches() }
var UnitActor.tiledY get() = cUnit.tiledY; set(value) { cUnit.tiledY = value; playStageOrNull?.updateCaches() }
var UnitActor.leaderID get() = cLeader?.leaderID ?: -1; set(value) { cLeader!!.leaderID = value }
var UnitActor.followerID get() = cFollower?.leaderID ?: -1; set(value) { cFollower!!.leaderID = value }
val UnitActor.minDamage get() = cAttack!!.minDamage
val UnitActor.maxDamage get() = cAttack!!.maxDamage
val UnitActor.attackRange get() = cAttack!!.attackRange
val UnitActor.movingRange get() = cUnit.movingRange
val UnitActor.viewRange get() = cUnit.viewRange
var UnitActor.actionPoints get() = cUnit.actionPoints; set(value) { cUnit.actionPoints = value }
val UnitActor.buffs get() = get(BuffsComponent::class.java)!!.buffs
val UnitActor.abilities get() = cAbilities!!.abilities
var UnitActor.mana get() = cAbilities!!.mana; set(value) { cAbilities!!.mana = value }
val UnitActor.allies get() = cUnit.allies;
val UnitActor.isLand get() = cUnit.type == UNIT_TYPE_LAND
val UnitActor.isWater get() = cUnit.type == UNIT_TYPE_WATER

@Deprecated("Returns cTerrainCost!!.", ReplaceWith("cTerrainCost?."), DeprecationLevel.WARNING)
val UnitActor.terrainCost get() = cTerrainProps!!
@Deprecated("Returns cMatchUp!!.", ReplaceWith("cMatchUp?."), DeprecationLevel.WARNING)
val UnitActor.matchupBonus get() = cMatchUp!!

/** @return AnimationViewComponent.sprite or SpriteViewComponent,sprite or null */
val UnitActor.sprite: Sprite? get() =
    when (viewComponent) {
        is AnimationViewComponent -> (viewComponent as AnimationViewComponent).sprite
        is SpriteViewComponent -> (viewComponent as SpriteViewComponent).sprite
        else -> null
    }

/*** UnitActor extensions */
//fun UnitActor.setPosition(x: Int, y: Int) {
//    setPosition((x * Const.TILE_SIZE).toFloat(), (y * Const.TILE_SIZE).toFloat())
//    tiledX = x
//    tiledY = y
//}

fun UnitActor.getAbility(name: String) : Ability? {
    for (ability in cAbilities?.abilities ?: return null) {
        if (ability.name == name) return ability
    }

    return null
}

fun UnitActor.distToUnit(otherUnit: UnitActor) : Int = tiledDst(tiledX, tiledY, otherUnit.tiledX, otherUnit.tiledY)

fun UnitActor.isUnitNear(unit: UnitActor, range: Int) : Boolean {
    return tiledDst(unit.tiledX, unit.tiledY, tiledX, tiledY) <= range
}

/** Set unit position at [x]:[y] if tile at is not Occupied
 * otherwise, try to set unit position near with the radius of 2 tiles */
fun UnitActor.setPositionOrNear(x: Int, y: Int, playStageOrNull: PlayStage? = null) {
    setPosition(-1, -1)

    // if stage == null or stage is not PlayStage set unit position at [x], [y] and return
    if ((stage == null && playStageOrNull == null)) {
        setPosition(x, y)

        return
    }

    val playStage = when {
        playStageOrNull != null -> playStageOrNull
        stage is PlayStage -> stage as PlayStage
        else -> {
            setPosition(x, y)
            return
        }
    }

    // if tile at [x], [y] is not Occupied, set unit position at [x], [y] and return
    if (playStage.getTile(x, y)?.isOccupied == false) {
        setPosition(x, y)

        return
    }

    // if tile at [x], [y] is Occupied, try to set unit position with an of offsets
    val offsets = Array<Pair<Int, Int>>().apply {
        add(0 to 1); add(0 to -1); add(1 to 0);
        add(-1 to 0); add(-1 to 1); add(1 to -1);
        add(-1 to -1); add(1 to 1); add(0 to 2);
        add(0 to -2); add(2 to 0); add(-2 to 0);
    }

    for (offset in offsets) {
        val xOff = x + offset.first
        val yOff = y + offset.second
        if (playStage.getTile(xOff, yOff)?.isOccupied == false ) {
            this.x = (xOff * Const.TILE_SIZE).toFloat()
            this.y = (yOff * Const.TILE_SIZE).toFloat()
            tiledX = xOff
            tiledY = yOff

            return
        }
    }

    Gdx.app.error("UnitExt.UnitActor.setPositionOrNear", "Tile at $x, $y is Occupied, so are tiles around at radius of 2.")
}

fun UnitActor.inAttackRange(x: Int, y: Int) : Boolean {
    if (cAttack == null)
        return false

    val tiledDst = tiledDst(x, y, tiledX, tiledY)

    return tiledDst > cAttack!!.attackRangeBlocked && tiledDst <= cAttack!!.attackRange
}

fun UnitActor.isInRange(x: Int, y: Int, range: Int) : Boolean {
    return tiledDst(x, y, tiledX, tiledY) <= range
}

fun UnitActor.haveActionPoints() : Boolean {
    return actionPoints >= 0
}

/** @return true if unit has enough AP to attack and has no Debuffs that are blocking it */
fun UnitActor.canAttack() : Boolean {
    return actionPoints > 0 && !buffs.any { it.name == "freeze" }
}

fun UnitActor.canAttack(unit: UnitActor) : Boolean {
    return canAttack() && isInAttackArea(unit)
}

/** @return true if player has enough AP to move and has no Debuffs that are blocking it */
fun UnitActor.canMove() : Boolean {
    return actionPoints >= Const.ACTION_POINTS_MOVE_MIN && !buffs.any { it.name == "freeze" }
}

fun UnitActor.canMove(tile: TileActor) : Boolean {
    return this.canMove(tile.tiledX, tile.tiledY)
}

/** @return true if player has enough AP to move and has no Debuffs that are blocking it */
fun UnitActor.canMove(tileX: Int, tileY: Int) : Boolean {
    if (this.stage == null)
        return false

    return playStage.getMovementGrid(this, true)[tileX][tileY] != 0
            && canMove()
            && playStage.getTile(tileX, tileY)?.isPassableAndFree() ?: false
}

fun UnitActor.isPlayerUnit(player: Player): Boolean {
    return this.playerId == player.id
}

fun UnitActor.isPlayerTeamUnit(player: Player): Boolean {
    return (teamId != Team.NONE && player.teamId != Team.NONE) && teamId == player.teamId
}

fun Player.isAlly(teamId: Int) : Boolean {
    return (this.teamId != Team.NONE && teamId != Team.NONE)
            && (this.teamId == teamId || allies.contains(teamId))
}

fun UnitActor.isAlly(unit: UnitActor) : Boolean {
    return isAlly(unit.teamId)
}

fun UnitActor.isAlly(player: Player) : Boolean {
    return isAlly(player.teamId)
}

fun UnitActor.isAlly(teamId: Int) : Boolean {
    return (this.teamId != Team.NONE && teamId != Team.NONE)
            && (this.teamId == teamId || allies.contains(teamId))
}

/** @return TRUE if (X, Y) is in unit's move + attack range */
fun UnitActor.isInAttackArea(checkX: Int, checkY: Int) : Boolean {
    return isInRange(checkX, checkY, attackRange)
}

fun UnitActor.isInAttackArea(unitActor: UnitActor) : Boolean {
    return isInRange(unitActor.tiledX, unitActor.tiledY, attackRange)
}

fun UnitActor.isAlive() : Boolean {
    return health > 0
}

fun UnitActor.canMoveAndAttackUnit(unit: UnitActor) : Boolean = isInAttackArea(unit.tiledX, unit.tiledY)

fun UnitActor.addAlly(teamId: Int) {
    allies.add(teamId)
}

fun UnitActor.removeAlly(teamId: Int) {
    allies.remove(teamId)
}

fun <T : UnitActor> T.player(player: Player) : T {
    playerId = player.id
    teamId = player.teamId
    allies.addAll(player.allies)
    initView(assets)

    return this
}

fun UnitActor.team(teamId: Int) : UnitActor {
    this.teamId = teamId

    return this
}

fun UnitActor.level(lvl: Int) : UnitActor {
    cLevel ?: add(LevelComponent())
    cLevel!!.lvl = lvl
    levelUp()

    return this
}

fun UnitActor.addToStage(stage: PlayStage) : UnitActor {
    stage.addActor(this)

    return this
}

fun UnitActor.position(x: Int, y: Int) : UnitActor {
    setPositionOrNear(x, y)

    return this
}

fun UnitActor.position(label: LabelActor) : UnitActor {
    position(label.x.tiled(), label.y.tiled())

    return this
}

fun UnitActor.followerOf(leader: UnitActor, dieWithLeader: Boolean = false) : UnitActor {
    check(leader.cLeader != null)

    followerOf(leader.leaderID, dieWithLeader)

    return this
}

fun UnitActor.followerOf(leaderId: Int, dieWithLeader: Boolean = false) : UnitActor {
    cFollower?.let {
        it.leaderID = leaderId
        it.dieWithLeader = dieWithLeader
    } ?: add(FollowerComponent(leaderId, dieWithLeader))

    return this
}

fun UnitActor.leader(leaderId: Int, leaderRange: Int = 1, killUnitsOnDeath: Boolean = false) : UnitActor {
    cLeader ?: add(LeaderComponent())
    this.leaderID = leaderId
    cLeader!!.leaderRange = leaderRange
    cLeader!!.killUnitsOnDeath = killUnitsOnDeath

    return this
}

fun UnitActor.dealDamage(damage: Int, attacker: UnitActor, playScreen: PlayScreen, killIfZero: Boolean = true) : UnitActor {
    var damage = damage

    get(BarrierComponent::class.java)?.let {
        val oldAmount = it.amount
        it.amount -= damage

        if (it.amount <= 0) {
            del(it)
        }

        playStageOrNull?.addActor(HealthChangeEffect(this, -damage).apply { color = Color.ORANGE })

        damage -= oldAmount
    }

    if (damage <= 0) return this

    health -= damage

    playStageOrNull?.addActor(HealthChangeEffect(this, -damage))

    if (killIfZero && this.health < 1) {
        this.killedBy(attacker, playScreen)
    }

    return this
}

fun UnitActor.heal(amount: Int) {
    val healing = if (health <= unitComponent.baseHealth - amount) amount else unitComponent.baseHealth - health

    health += healing
    playStage.addActor(HealthChangeEffect(this, healing))
    if (health > unitComponent.baseHealth)
        health = unitComponent.baseHealth
}

fun UnitActor.killedBy(killer: UnitActor, playScreen: PlayScreen) {
    val thisUnit = this
    val leader = if (killer.isLeader) killer else playScreen.playStage.getLeadUnit(killer.followerID)
    if (!thisUnit.isAlly(killer) && leader != null) {
        val exp = (if (thisUnit.isLeader) Const.EXP_GAIN_LEADER else Const.EXP_GAIN) * (thisUnit.cLevel?.lvl ?: 1)
        leader.grantExp(exp, playScreen)
    } //else  playStage.commandManager.execute(GrantXpCommand(unitAttacker, unitDefender))

    thisUnit.die(playScreen, killer)

    if (thisUnit.isLeader && thisUnit.cLeader?.killUnitsOnDeath == true)
        for (unit in playScreen.playStage.getUnitsForLeader(thisUnit.leaderID, true)) {
            if (!unit.isAlly(killer) && leader != null) {
                val exp = (if (unit.isLeader) Const.EXP_GAIN_LEADER else Const.EXP_GAIN) * (unit.cLevel?.lvl ?: 1)
                leader.grantExp(exp, playScreen)
            }

            unit.die(playScreen, killer)
        }
}

fun UnitActor.grantExp(amount: Int, playScreen: PlayScreen) {
    val unit = this

    val cLevel = unit.cLevel ?: return
    cLevel.exp += amount

    val label = FloatingLabelAnimation("+$amount XP", "font_5")
    label.color = Color.BLUE
    label.setPosition(unit.centerX - label.width / 4, unit.centerY)
    playScreen.playStage.addActor(label)

    val oldLvl = cLevel.lvl

    //if enough exp to level up
    while (cLevel.exp >= xpToLevelUp(cLevel.lvl) && cLevel.lvl < Const.MAX_LVL) {
        cLevel.exp -= xpToLevelUp(cLevel.lvl)
        cLevel.lvl++

        if (cLevel.lvl >= Const.MAX_LVL)
            cLevel.exp = xpToLevelUp(Const.MAX_LVL - 1)

        val label = FloatingLabelAnimation("LEVEL UP!", "font_5")
        label.color = Color.YELLOW
        label.setPosition(unit.centerX - label.width / 4, unit.centerY)
        playScreen.playStage.addActor(label)
    }

    if(oldLvl != cLevel.lvl) {
        if (playScreen.gameType == GameType.STORY)
            unit.levelUp()
        //TODO level up for other modes

        playScreen.fireEvent(UnitLevelUpEvent(unit))
    }
}

fun UnitActor.die(playScreen: PlayScreen, killer: UnitActor? = null) {
    if (health > 0)
        health = 0

    if (stage == null) return

    if (!isViewInitialized) {
        remove()

        return
    }

    cAnimationView?.isAnimate = false

    playScreen.queueAction(KillUnitAction(this))

    playScreen.playStage.root.fire(UnitDiedEvent(this, killer))
}

fun UnitActor.captureBase(base: TileActor, player: Player? = null) {
    setPlayerForCapturableTile(base, playerId, player?.species)
}

fun UnitActor.getClosestTileInMoveRange(destX: Int, destY: Int, pTiles: Array<TileActor>? = null, includeUnitPosTile: Boolean = false) : TileActor? {
    playStageOrNull ?: return null

    var closestTile: TileActor? = null
    val tiles = pTiles ?: playStage.getTiles()
    for (tile in tiles) {
        if (closestTile == null) {
            if (this.canMove(tile.tiledX, tile.tiledY))
                closestTile = tile
        } else {
            if ((this.canMove(tile.tiledX, tile.tiledY) || (includeUnitPosTile && tile.tiledX == tiledX && tile.tiledY == tiledY))
                && (tiledDst(destX, destY, tile.tiledX, tile.tiledY) < tiledDst(destX, destY, closestTile.tiledX, closestTile.tiledY))) {
                closestTile = tile
            }
        }
    }

    return closestTile
}

fun UnitActor.throwAction(sourceUnit: UnitActor, throwX: Int, throwY: Int, damage: Int, playScreen: PlayScreen) {
    if (this.isAlly(sourceUnit) && !playScreen.friendlyFire)
        return

    playScreen.addAction(ThrowUnitAction(this, throwX, throwY)).addOnCompleteTrigger { _ ->
        CreateEffect.damageUnit(this.dealDamage(damage, sourceUnit, playScreen))
    }
}

fun playerColorName(playerId: Int) =
    when (playerId) {
        1 -> "blue"
        2 -> "red"
        3 -> "green"
        4 -> "purple"
        5 -> "yellow"
        6 -> "orange"
        7 -> "pink"
        8 -> "brown"
        else -> ""
    }

fun UnitActor.createAnimation(regionName: String, frameDuration: Float = Const.UNIT_ANIMATION_FRAME_DURATION) : RegionAnimation {
    assets.textureAtlas.findRegions("units/${playerColorName(playerId)}/$regionName").also { teamFrames ->
        if (teamFrames.isEmpty) {
            assets.textureAtlas.findRegions("units/$regionName").also { defFrames ->
                if (defFrames.isEmpty) {
                    throw IllegalArgumentException("Textures not found")
                } else {
                    return RegionAnimation(frameDuration, defFrames)
                }
            }
        } else {
            return RegionAnimation(frameDuration, teamFrames)
        }
    }
}

fun xpToLevelUp(curLvl: Int) = Const.EXP_MOD_LEVEL_UP * curLvl

fun findUnitTextures(unitName: String, playerId: Int): Array<TextureAtlas.AtlasRegion> {
    var regions = assets.textureAtlas.findRegions("units/${playerColorName(playerId)}/${unitName.lowercase(Locale.ROOT)}")
    if (regions.isEmpty) regions = assets.textureAtlas.findRegions("units/${unitName.lowercase(Locale.ROOT)}")
    if (regions.isEmpty) {
        regions.add(assets.textureAtlas.findRegion("units/unit"))
        Gdx.app.log("UnitActor.initView", "Unit textures not found: units/${playerColorName(playerId)}/$unitName")
    }

    return regions
}