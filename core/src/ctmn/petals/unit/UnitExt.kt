package ctmn.petals.unit

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const
import ctmn.petals.assets
import ctmn.petals.effects.CreateEffect
import ctmn.petals.effects.FloatingLabelAnimation
import ctmn.petals.effects.HealthChangeEffect
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playscreen.events.UnitLevelUpEvent
import ctmn.petals.playscreen.seqactions.KillUnitAction
import ctmn.petals.playscreen.seqactions.ThrowUnitAction
import ctmn.petals.playstage.*
import ctmn.petals.tile.*
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.actors.*
import ctmn.petals.unit.component.*
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.tiled
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
var UnitActor.health
    get() = cUnit.health;
    set(value) {
        cUnit.health = value
    }
var UnitActor.defense
    get() = cUnit.defense;
    set(value) {
        cUnit.defense = value
    }
var UnitActor.playerId
    get() = cUnit.playerID;
    set(value) {
        cUnit.playerID = value
    }
var UnitActor.teamId
    get() = cUnit.teamID;
    set(value) {
        cUnit.teamID = value
    }
var UnitActor.tiledX
    get() = cUnit.tiledX;
    set(value) {
        cUnit.tiledX = value; playStageOrNull?.updateCaches()
    }
var UnitActor.tiledY
    get() = cUnit.tiledY;
    set(value) {
        cUnit.tiledY = value; playStageOrNull?.updateCaches()
    }
var UnitActor.leaderID
    get() = cLeader?.leaderID ?: -1;
    set(value) {
        cLeader!!.leaderID = value
    }
var UnitActor.followerID
    get() = cFollower?.leaderID ?: -1;
    set(value) {
        cFollower!!.leaderID = value
    }
val UnitActor.minDamage get() = cAttack!!.minDamage
val UnitActor.maxDamage get() = cAttack!!.maxDamage
val UnitActor.attackRange get() = cAttack!!.attackRange
val UnitActor.movingRange get() = cUnit.movingRange
val UnitActor.viewRange get() = cUnit.viewRange
var UnitActor.actionPoints
    get() = cUnit.actionPoints;
    set(value) {
        cUnit.actionPoints = value
    }
val UnitActor.buffs get() = get(BuffsComponent::class.java)!!.buffs
val UnitActor.abilities get() = cAbilities!!.abilities
var UnitActor.mana
    get() = cAbilities!!.mana;
    set(value) {
        cAbilities!!.mana = value
    }
val UnitActor.allies get() = cUnit.allies;
val UnitActor.isLand get() = cUnit.type == UNIT_TYPE_LAND
val UnitActor.isWater get() = cUnit.type == UNIT_TYPE_WATER
val UnitActor.isAir get() = cUnit.type == UNIT_TYPE_AIR

@Deprecated("Returns cTerrainCost!!.", ReplaceWith("cTerrainCost?."), DeprecationLevel.WARNING)
val UnitActor.terrainCost get() = cTerrainProps!!

@Deprecated("Returns cMatchUp!!.", ReplaceWith("cMatchUp?."), DeprecationLevel.WARNING)
val UnitActor.matchupBonus get() = cMatchUp!!

/** @return AnimationViewComponent.sprite or SpriteViewComponent,sprite or null */
val UnitActor.sprite: Sprite?
    get() =
        when (viewComponent) {
            is AnimationViewComponent -> (viewComponent as AnimationViewComponent).sprite
            is SpriteViewComponent -> (viewComponent as SpriteViewComponent).sprite
            else -> null
        }

val UnitActor.canAttackAir get() = cAttack?.attackType == ATTACK_TYPE_ALL || cAttack?.attackType == ATTACK_TYPE_AIR

val UnitActor.isWorker get() = selfName == UnitIds.DOLL_SOWER || selfName == UnitIds.GOBLIN_PICKAXE

/*** UnitActor extensions */
//fun UnitActor.setPosition(x: Int, y: Int) {
//    setPosition((x * Const.TILE_SIZE).toFloat(), (y * Const.TILE_SIZE).toFloat())
//    tiledX = x
//    tiledY = y
//}

fun UnitActor.getAbility(name: String): Ability? {
    for (ability in cAbilities?.abilities ?: return null) {
        if (ability.name == name) return ability
    }

    return null
}

fun UnitActor.distToUnit(otherUnit: UnitActor): Int = tiledDst(tiledX, tiledY, otherUnit.tiledX, otherUnit.tiledY)

fun UnitActor.isUnitNear(unit: UnitActor, range: Int): Boolean {
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
        if (playStage.getTile(xOff, yOff)?.isOccupied == false) {
            this.x = (xOff * Const.TILE_SIZE).toFloat()
            this.y = (yOff * Const.TILE_SIZE).toFloat()
            tiledX = xOff
            tiledY = yOff

            return
        }
    }

    Gdx.app.error(
        "UnitExt.UnitActor.setPositionOrNear",
        "Tile at $x, $y is Occupied, so are tiles around at radius of 2."
    )
}

fun UnitActor.inAttackRange(x: Int, y: Int): Boolean {
    if (cAttack == null)
        return false

    val tiledDst = tiledDst(x, y, tiledX, tiledY)

    return tiledDst > cAttack!!.attackRangeBlocked && tiledDst <= cAttack!!.attackRange
}

fun UnitActor.isInRange(x: Int, y: Int, range: Int): Boolean {
    return tiledDst(x, y, tiledX, tiledY) <= range
}

fun UnitActor.haveActionPoints(): Boolean {
    return actionPoints >= 0
}

/** @return true if unit has enough AP to attack and has no Debuffs that are blocking it */
fun UnitActor.canAttackNow(): Boolean {
    return actionPoints > 0 && !buffs.any { it.name == "freeze" }
}

fun UnitActor.canAttackNow(unit: UnitActor): Boolean {
    //if (!isAir && unit.isAir && attackRange < 2) return false
    when {
        cAttack?.attackType == ATTACK_TYPE_AIR && !unit.isAir -> return false
        cAttack?.attackType == ATTACK_TYPE_GROUND && unit.isAir -> return false
    }
    return canAttackNow() && isInAttackArea(unit) && canAttack(unit)
}

fun UnitActor.canAttack(unit: UnitActor): Boolean {
    when {
        cAttack?.attackType == ATTACK_TYPE_AIR && !unit.isAir -> return false
        cAttack?.attackType == ATTACK_TYPE_GROUND && unit.isAir -> return false
        //cAttack?.attackType == ATTACK_TYPE_ALL && isAir && unit.isAir &&  -> return false
        (get(ReloadingComponent::class.java)?.currentTurns ?: 0) > 0 -> return false
        get(MoveAfterAttackComponent::class.java)?.attacked == true -> return false
    }

    return true
}

/** @return true if player has enough AP to move and has no Debuffs that are blocking it */
fun UnitActor.canMove(): Boolean {
    return actionPoints >= Const.ACTION_POINTS_MOVE_MIN && !buffs.any { it.name == "freeze" }
}

fun UnitActor.canMove(tile: TileActor): Boolean {
    return this.canMove(tile.tiledX, tile.tiledY)
}

/** @return true if player has enough AP to move and has no Debuffs that are blocking it */
fun UnitActor.canMove(tileX: Int, tileY: Int): Boolean {
    if (this.stage == null)
        return false

    return playStage.getMovementGrid(this, true)[tileX][tileY] != 0
            && canMove()
            && playStage.getTile(tileX, tileY)?.isPassableAndFree() ?: false
}

fun UnitActor.addAttackDamage(amount: Int) {
    cAttack?.apply {
        minDamage += amount
        maxDamage += amount
    }
}

fun UnitActor.canBuildBase(): Boolean {
    return (selfName == UnitIds.DOLL_SOWER || selfName == UnitIds.GOBLIN_PICKAXE)
}

fun UnitActor.canBuildBase(tile: TileActor): Boolean {
    return canBuildBase() && tile.terrain == TerrainNames.grass || tile.terrain == TerrainNames.water
}

fun UnitActor.canDestroy(tile: TileActor, tileTeamId: Int? = null): Boolean {
    return canDestroy() && tile.isBase && tile.cPlayerId?.playerId != playerId && this.teamId != tileTeamId
}

fun UnitActor.canDestroy(): Boolean {
    return (selfName != UnitIds.DOLL_SOWER
            && selfName != UnitIds.GOBLIN_PICKAXE
//            && selfName != UnitIds.GOBLIN_GIANT
//            && selfName != UnitIds.HUNTER
            && selfName != UnitIds.GOBLIN_SCOUT
            && selfName != UnitIds.PIXIE
            && selfName != UnitIds.CUCUMBER
            && selfName != UnitIds.GOBLIN_CATAPULT
            && selfName != UnitIds.DOLL_CANNON
            && !isLeader
            && !(isAir && cAttack?.attackType == ATTACK_TYPE_GROUND)
            )
}

fun UnitActor.canCapture(): Boolean {
    if (Const.EXPERIMENTAL) {
        return selfName == UnitIds.DOLL_SOWER || selfName == UnitIds.GOBLIN_PICKAXE
    }

    return (selfName != UnitIds.GOBLIN_GIANT
            && selfName != UnitIds.HUNTER
            && selfName != UnitIds.GOBLIN_SCOUT
            && selfName != UnitIds.PIXIE
            && selfName != UnitIds.CUCUMBER
            && selfName != UnitIds.GOBLIN_CATAPULT
            && selfName != UnitIds.DOLL_CANNON
            && !isLeader
            && !(isAir && cAttack?.attackType == ATTACK_TYPE_GROUND)
            )
}

fun UnitActor.canCapture(tile: TileActor): Boolean {
    return tile.isCapturable && canCapture() && tile.cPlayerId?.playerId != playerId
}

fun UnitActor.isPlayerUnit(player: Player): Boolean {
    return this.playerId == player.id
}

fun UnitActor.isPlayerTeamUnit(player: Player): Boolean {
    return (teamId != Team.NONE && player.teamId != Team.NONE) && teamId == player.teamId
}

fun Player.isAllyId(playerId: Int): Boolean {
    return playerId != Player.NONE && id == playerId
}

fun Player.isAlly(teamId: Int): Boolean {
    return (this.teamId != Team.NONE && teamId != Team.NONE)
            && (this.teamId == teamId || allies.contains(teamId))
}

fun UnitActor.isAlly(unit: UnitActor): Boolean {
    return isAlly(unit.teamId)
}

fun UnitActor.isAlly(player: Player): Boolean {
    return isAlly(player.teamId)
}

fun UnitActor.isAlly(teamId: Int): Boolean {
    return (this.teamId != Team.NONE && teamId != Team.NONE)
            && (this.teamId == teamId || allies.contains(teamId))
}

/** @return TRUE if (X, Y) is in unit's move + attack range */
fun UnitActor.isInAttackArea(checkX: Int, checkY: Int): Boolean {
    return isInRange(checkX, checkY, attackRange)
}

fun UnitActor.isInAttackArea(unitActor: UnitActor): Boolean {
    return isInRange(unitActor.tiledX, unitActor.tiledY, attackRange)
}

fun UnitActor.isAlive(): Boolean {
    return health > 0
}

fun UnitActor.canMoveAndAttackUnit(unit: UnitActor): Boolean = isInAttackArea(unit.tiledX, unit.tiledY)

fun UnitActor.addAlly(teamId: Int) {
    allies.add(teamId)
}

fun UnitActor.removeAlly(teamId: Int) {
    allies.remove(teamId)
}

fun <T : UnitActor> T.player(player: Player): T {
    playerId = player.id
    teamId = player.teamId
    allies.addAll(player.allies)
    initView(assets)

    return this
}

fun <T : UnitActor> T.player(playerId: Int, teamId: Int = playerId): T {
    this.playerId = playerId
    this.teamId = teamId
    initView(assets)

    return this
}

fun UnitActor.team(teamId: Int): UnitActor {
    this.teamId = teamId

    return this
}

fun UnitActor.level(lvl: Int): UnitActor {
    cLevel ?: add(LevelComponent())
    cLevel!!.lvl = lvl
    levelUp()

    return this
}

fun UnitActor.addToStage(stage: PlayStage): UnitActor {
    stage.addActor(this)

    return this
}

fun UnitActor.position(x: Int, y: Int): UnitActor {
    setPositionOrNear(x, y)

    return this
}

fun UnitActor.position(label: LabelActor): UnitActor {
    position(label.x.tiled(), label.y.tiled())

    return this
}

fun UnitActor.position(tile: TileActor): UnitActor {
    position(tile.tiledX, tile.tiledY)

    return this
}

fun UnitActor.followerOf(leader: UnitActor, dieWithLeader: Boolean = false): UnitActor {
    check(leader.cLeader != null)

    followerOf(leader.leaderID, dieWithLeader)

    return this
}

fun UnitActor.followerOf(leaderId: Int, dieWithLeader: Boolean = false): UnitActor {
    cFollower?.let {
        it.leaderID = leaderId
        it.dieWithLeader = dieWithLeader
    } ?: add(FollowerComponent(leaderId, dieWithLeader))

    return this
}

fun UnitActor.leader(leaderId: Int, leaderRange: Int = 1, killUnitsOnDeath: Boolean = false): UnitActor {
    cLeader ?: add(LeaderComponent())
    this.leaderID = leaderId
    cLeader!!.leaderRange = leaderRange
    cLeader!!.killUnitsOnDeath = killUnitsOnDeath

    return this
}

fun UnitActor.dealDamage(
    damage: Int,
    attacker: UnitActor? = null,
    playScreen: PlayScreen,
    killIfZero: Boolean = true,
): UnitActor {
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
        if (attacker != null)
            this.killedBy(attacker, playScreen)
        else
            this.die(playScreen)
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

    if (oldLvl != cLevel.lvl) {
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

val Terrain.atkPlusDf get() = attackBonus + defenseBonus

fun UnitActor.getClosestTileInMoveRange(
    destX: Int,
    destY: Int,
    pTiles: Array<TileActor>? = null,
    includeUnitPosTile: Boolean = false,
): TileActor? {
    playStageOrNull ?: return null

    var closestTile: TileActor? = null
    val tiles = pTiles ?: playStage.getTiles()
    for (tile in tiles) {
        if (closestTile == null) {
            if (this.canMove(tile.tiledX, tile.tiledY))
                closestTile = tile
        } else {
            if ((this.canMove(
                    tile.tiledX,
                    tile.tiledY
                ) || (includeUnitPosTile && tile.tiledX == tiledX && tile.tiledY == tiledY))
                && (tiledDst(destX, destY, tile.tiledX, tile.tiledY) < tiledDst(
                    destX,
                    destY,
                    closestTile.tiledX,
                    closestTile.tiledY
                ))
            ) {
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

fun playerIdByColor(colorName: String) =
    when (colorName) {
        "blue" -> 1
        "red" -> 2
        "green" -> 3
        "purple" -> 4
        "yellow" -> 5
        "orange" -> 6
        "pink" -> 7
        "brown" -> 8
        else -> -1
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
        else -> playerId.toString()
    }

/** Crates looped animation.
 * @throws IllegalArgumentException if unit texture region [regionName] not found */
fun UnitActor.createAnimation(
    regionName: String,
    frameDuration: Float = Const.UNIT_ANIMATION_FRAME_DURATION,
): RegionAnimation {
    val unitAtlas = assets.findUnitAtlas(playerId)
    unitAtlas.findRegions(regionName).also { teamFrames ->
        if (teamFrames.isEmpty) {
            unitAtlas.findRegions(regionName).also { defFrames ->
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

/** Crates looped animation or normal if [loop] is false.
 * @returns null if unit texture region [regionName] not found */
fun UnitActor.findAnimation(
    regionName: String,
    frameDuration: Float = Const.UNIT_ANIMATION_FRAME_DURATION,
    loop: Boolean = false,
): RegionAnimation? {
    val unitAtlas = assets.findUnitAtlas(playerId)
    unitAtlas.findRegions(regionName).also { teamFrames ->
        if (teamFrames.isEmpty) {
            unitAtlas.findRegions(regionName).also { defFrames ->
                if (defFrames.isEmpty) {
                    return null
                } else {
                    return RegionAnimation(frameDuration, defFrames).apply {
                        if (!loop) playMode = Animation.PlayMode.NORMAL
                    }
                }
            }
        } else {
            return RegionAnimation(frameDuration, teamFrames).apply {
                if (!loop) playMode = Animation.PlayMode.NORMAL
            }
        }
    }
}

fun xpToLevelUp(curLvl: Int) =
    if (Const.NEED_MORE_EXP_PER_LVL) Const.EXP_TO_LEVEL_UP * curLvl else Const.EXP_TO_LEVEL_UP

fun findUnitTextures(unitName: String, playerId: Int): Array<TextureAtlas.AtlasRegion> {
    val unitAtlas = assets.findUnitAtlas(playerId)
    val regions = unitAtlas.findRegions(unitName.lowercase(Locale.ROOT))
    if (regions.isEmpty) {
        regions.add(unitAtlas.findRegion("unit"))
        Gdx.app.log("UnitsExt.findUnitTextures()", "Unit textures not found: $unitName")
    }

    return regions
}

fun playerIdByUnitSpecies(unit: UnitActor): Int =
    when (unit) {
        is Alice -> 1
        is FairyAxe -> 1
        is FairyBomber -> 1
        is FairyBow -> 1
        is FairyCannon -> 1
        is FairyCucumber -> 1
        is FairyHealer -> 1
        is FairyHunter -> 1
        is FairyPike -> 1
        is FairyPixie -> 1
        is FairyScout -> 1
        is FairyShield -> 1
        is FairySword -> 1
        is FairyWaterplant -> 1
        is FairyGlaive -> 1
        is FairySower -> 1
        is FairyArmorSword -> 1

        is Goblin -> 2
        is GoblinBoar -> 2
        is GoblinBow -> 2
        is GoblinCatapult -> 2
        is GoblinDuelist -> 2
        is GoblinGalley -> 2
        is GoblinGiant -> 2
        is GoblinHealer -> 2
        is GoblinLeader -> 2
        is GoblinPike -> 2
        is GoblinScout -> 2
        is GoblinShip -> 2
        is GoblinSword -> 2
        is GoblinWolf -> 2
        is GoblinWyvern -> 2
        is GoblinPickaxe -> 2

        is Cherie -> 3
        is CherieSpearman -> 3

        is Slime -> 4
        is PinkSlimeLing -> 4
        is SlimeLing -> 4
        is SlimeBig -> 4
        is SlimeHuge -> 4
        is SlimeTiny -> 4
        else -> if (unit.playerId == Player.NONE) 4 else unit.playerId
    }