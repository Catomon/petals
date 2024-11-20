package ctmn.petals.unit

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.google.gson.JsonObject
import ctmn.petals.*
import ctmn.petals.Const.ACTION_POINTS_MOVE
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.Const.TILE_SIZE_X2
import ctmn.petals.effects.Animations
import ctmn.petals.effects.MissileActor
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.GameActor
import ctmn.petals.tile.isFluid
import ctmn.petals.unit.component.*
import ctmn.petals.utils.*
import ctmn.petals.utils.serialization.Jsonable

open class UnitActor(pUnitComponent: UnitComponent? = null) : GameActor(), Jsonable {

    lateinit var unitComponent: UnitComponent
    lateinit var viewComponent: ViewComponent
    var isViewInitialized = false

    lateinit var defaultAnimation: RegionAnimation
    var attackAnimation: RegionAnimation? = null
    var abilityCastAnimation: RegionAnimation? = null
    var talkingAnimation: RegionAnimation? = null
    var airborneAnimation: RegionAnimation? = null
    var postAirborneAnimation: RegionAnimation? = null

    open val attackEffect: MissileActor? = null

    private var currentAnimationDuration = 0f

    val animationProps = AnimationProps()

    var actionPointsMove = ACTION_POINTS_MOVE

    var hitSounds: Array<String> = arrayOf("hit.ogg", "hit_2.ogg")

    class AnimationProps {
        var attackFrame = 1f
        var attackEffectFrame = 0f
    }

    var characterName = if (Const.DEBUG_MODE) this::class.simpleName.toString() else ""

    init {
        if (pUnitComponent != null) {
            this.add(pUnitComponent)
        }

        if (cLevel == null)
            this.add(LevelComponent())

        this.add(BuffsComponent())
        this.add(TerrainPropComponent(TerrainPropsPack.foot))
        this.add(TerrainPropComponent(TerrainPropsPack.foot))
        this.add(MatchUpBonusComponent())
    }

    fun setIdleAnimation(name: String) {
        val regions = findUnitTextures(selfName, playerId)
        defaultAnimation = RegionAnimation(Const.UNIT_ANIMATION_FRAME_DURATION, regions)
    }

    // should be called after setting up actor's playerID
    open fun initView(assets: Assets) {
        val regions = findUnitTextures(selfName, playerId)

        defaultAnimation = RegionAnimation(Const.UNIT_ANIMATION_FRAME_DURATION, regions)

        viewComponent = if (regions.size > 1)
            AnimationViewComponent(defaultAnimation)
        else
            SpriteViewComponent(Sprite(regions.first()))

        if (!isViewInitialized) {
            add(viewComponent)
            isViewInitialized = true
        }

        sprite!!.setSize(TILE_SIZE_X2, TILE_SIZE_X2)

        sprite?.setOriginCenter()

        setSize(TILE_SIZE, TILE_SIZE)

        positionChanged()

        isVisible = false

        loadAnimations()

        if (characterName.isEmpty()) characterName = if (Const.DEBUG_MODE) this::class.simpleName.toString() else ""
    }

    protected open fun loadAnimations() {
        attackAnimation = findAnimation("${selfName}_attack", 0.25f)
        attackAnimation = findAnimation("${selfName}_attack", 0.25f)

        showWaterEffect = !isAir
    }

    override fun add(component: Component): Component {
        if (component is UnitComponent)
            unitComponent = component

        return super.add(component)
    }

    fun setAnimation(
        animation: RegionAnimation?,
        duration: Float = animation?.animationDuration ?: defaultAnimation.animationDuration,
    ) {
        if (viewComponent !is AnimationViewComponent)
            return
        val viewComponent = viewComponent as AnimationViewComponent
        viewComponent.animation = animation ?: defaultAnimation

        animation?.stateTime = 0f

        currentAnimationDuration = duration
    }

    private var isFrozen = false

    open fun updateView() {
        if (buffs.findLast { it.name == "freeze" } != null) {
            sprite?.color = Color.BLUE.cpy()
            isFrozen = true
        } else {
            if (isFrozen) {
                sprite?.color = Color.WHITE.cpy()
                isFrozen = false
            }
        }
    }

    override fun toJsonObject(): JsonObject {
        val unitJsonObject = JsonObject()

        unitJsonObject.addProperty("name", cUnit.name)
        unitJsonObject.addProperty("id", name)

        for (component in components.components) {
            if (component is ViewComponent) continue
            if (component is MatchUpBonusComponent) continue
            if (component is TerrainPropComponent) continue
            if (component is ShopComponent) continue

            val json = gson.toJsonTree(component)
            unitJsonObject.add(component.javaClass.simpleName, json)
        }

        return unitJsonObject
    }

    override fun fromJsonObject(json: JsonObject) {
        check(cUnit.name == json.get("name").asString)

        name = json.get("id")?.asString ?: name

        for (componentClass in Components.classes) {
            val jsonComponent = json.get(componentClass.simpleName) ?: continue

            add(gson.fromJson(jsonComponent, componentClass))
        }

        //updateView()

        setPosition(cUnit.tiledX, cUnit.tiledY)
    }

    override fun act(delta: Float) {
        super.act(delta)

        if (viewComponent is AnimationViewComponent) {
            val aniC = viewComponent as AnimationViewComponent

            if (currentAnimationDuration > 0) {
                currentAnimationDuration -= delta

                if (currentAnimationDuration <= 0) {
                    aniC.animation = defaultAnimation
                }
            }
        }

        viewComponent.update(delta)
    }

//    private fun adjustSpriteSize() {
//        val sprite = sprite ?: return
//        if (sprite.regionHeight)
//        sprite.setSize(sprite.width, (sprite.regionHeight).toFloat())
//    }

    protected var showWaterEffect = true
    private var isWater = false
    private var waterSprite = newPlaySprite(Animations.waterWaves.currentFrame)
    private var underWaterOffset = 12f

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        val sprite = sprite!!

        sprite.setAlpha(color.a)

        if (isWater) {
            sprite.regionHeight -= 12.toPlayScale()
            sprite.regionY -= 0

            viewComponent.setPosition(x + TILE_SIZE / 2, y + TILE_SIZE / 2 + 10.toPlayScale())

            waterSprite.setPosition(sprite.x, sprite.y - 11.toPlayScale())

            if (sprite.width > 32) {
                sprite.setSize(32f, 32f - 12f)
            } else {
                sprite.setSize(sprite.width, sprite.height - 12f)
            }
        } else {
            if (actions.isEmpty)
                viewComponent.setPosition(x + TILE_SIZE / 2, y + TILE_SIZE / 2)

            if (sprite.width > 32) {
                sprite.setSize(32f, 32f)
            } else {
                sprite.setSize(32f, 32f)
            }
        }

//        sprite.setSize(sprite.width, (sprite.regionHeight).toFloat())
//
//        if (sprite.width > 32) {
//            sprite.setSize(32f, sprite.height)
//            if (sprite.height > 32f)
//                sprite.setSize()
//        }

        viewComponent.draw(batch as SpriteBatch)

        if (isWater && showWaterEffect) {
            waterSprite.setRegion(Animations.waterWaves.currentFrame)
            waterSprite.draw(batch)
        }

        val barrier = cBarrierMapper.get(components)
        if (barrier != null) {
            val barrierFrame = Animations.barrier.currentFrame
            val doubleTileSize = TILE_SIZE * 2f
            batch.draw(
                barrierFrame,
                centerX - doubleTileSize / 2,
                centerY - doubleTileSize / 2,
                doubleTileSize,
                doubleTileSize
            )
        }
        val burning = get(BurningComponent::class.java)
        if (burning != null) {
            val burningFrame = Animations.burning.currentFrame
            val doubleTileSize = TILE_SIZE * 2f
            batch.draw(
                burningFrame,
                centerX - doubleTileSize / 2,
                centerY - doubleTileSize / 2,
                doubleTileSize,
                doubleTileSize
            )
        }
    }

    fun setPosition(x: Int, y: Int) {
        setPosition((x * TILE_SIZE).toFloat(), (y * TILE_SIZE).toFloat())
        tiledX = x
        tiledY = y

        if (!::viewComponent.isInitialized) return

        playStageOrNull?.let { playStage ->
            isWater = playStage.getTile(x, y)?.isFluid == true
        }
    }

    override fun positionChanged() {
        super.positionChanged()

        if (::viewComponent.isInitialized)
            viewComponent.setPosition(x + TILE_SIZE / 2, y + TILE_SIZE / 2)
    }

    override fun toString(): String {
        return "UnitActor[nm: $name, x: ${cUnit.tiledX}, y: ${cUnit.tiledY}, pl: ${cUnit.playerID}, tm: ${cUnit.teamID}]"
    }

    fun makeCopy(): UnitActor {
        val unitCopy = this::class.constructors.first().call()
        unitCopy.name = this.name
        for (component in components.components) {
            unitCopy.add(
                when (component) {
                    is CopyableComponent -> component.makeCopy()
                    else -> continue
                }
            )
        }

        return unitCopy
    }
}