package ctmn.petals.unit

import com.badlogic.ashley.core.Component
import ctmn.petals.Assets
import ctmn.petals.Const
import ctmn.petals.unit.component.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.google.gson.JsonObject
import ctmn.petals.effects.Animations
import ctmn.petals.newPlaySprite
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playstage.GameActor
import ctmn.petals.playscreen.selfName
import ctmn.petals.tile.TerrainNames
import ctmn.petals.utils.*
import ctmn.petals.utils.serialization.Jsonable
import java.util.*

open class UnitActor(pUnitComponent: UnitComponent? = null) : GameActor(), Jsonable {

    lateinit var unitComponent: UnitComponent
    lateinit var viewComponent: ViewComponent
    var isViewInitialized = false

    lateinit var defaultAnimation: RegionAnimation
    var abilityCastAnimation: RegionAnimation? = null
    var talkingAnimation: RegionAnimation? = null
    var airborneAnimation: RegionAnimation? = null
    var postAirborneAnimation: RegionAnimation? = null

    private var currentAnimationDuration = 0f

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

        if (sprite!!.width < Const.TILE_SIZE)
            sprite!!.setSize(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat())
        if (sprite!!.width > Const.TILE_SIZE * 2)
            sprite!!.setSize(Const.TILE_SIZE * 2f, Const.TILE_SIZE * 2f)

        sprite?.setOriginCenter()

        setSize(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat())

        positionChanged()

        isVisible = false
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

        currentAnimationDuration = duration
    }

    open fun updateView() {
        if (buffs.findLast { it.name == "freeze" } != null) {
            sprite?.color = Color.BLUE.cpy()
        } else
            sprite?.color = Color.WHITE.cpy()
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

    private var isWater = false
    private var waterSprite = newPlaySprite(Animations.waterWaves.currentFrame)

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        val sprite = sprite!!

        sprite.setAlpha(color.a)

        if (isWater) {
            sprite.regionHeight -= 48
            sprite.regionY -= 0

            viewComponent.setPosition(x + Const.TILE_SIZE / 2, y + Const.TILE_SIZE / 2 + 5)

            waterSprite.setPosition(sprite.x, sprite.y - 7 - 4)
        } else {
            if (actions.isEmpty)
                viewComponent.setPosition(x + Const.TILE_SIZE / 2, y + Const.TILE_SIZE / 2)
        }

        sprite.setSize(sprite.width, (sprite.regionHeight / 4).toFloat())

        viewComponent.draw(batch as SpriteBatch)

        if (isWater) {
            waterSprite.setRegion(Animations.waterWaves.currentFrame)
            waterSprite.draw(batch)
        }

        val barrier = cBarrierMapper.get(components)
        if (barrier != null) {
            val barrierFrame = Animations.barrier.currentFrame
            val doubleTileSize = Const.TILE_SIZE * 2f
            batch.draw(
                barrierFrame,
                centerX - doubleTileSize / 2,
                centerY - doubleTileSize / 2,
                doubleTileSize,
                doubleTileSize
            )
        }
    }

    fun setPosition(x: Int, y: Int) {
        setPosition((x * Const.TILE_SIZE).toFloat(), (y * Const.TILE_SIZE).toFloat())
        tiledX = x
        tiledY = y

        if (!::viewComponent.isInitialized) return

        playStageOrNull?.let { playStage ->
            val terrain = playStage.getTile(x, y)?.terrain
            isWater = terrain == TerrainNames.water || terrain == TerrainNames.deepwater
        }
    }

    override fun positionChanged() {
        super.positionChanged()

        if (::viewComponent.isInitialized)
            viewComponent.setPosition(x + Const.TILE_SIZE / 2, y + Const.TILE_SIZE / 2)
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