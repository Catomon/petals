package ctmn.petals.tile

import com.badlogic.ashley.core.ComponentMapper
import ctmn.petals.Assets
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.setPositionByCenter
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.google.gson.JsonObject
import ctmn.petals.playstage.Decorator
import ctmn.petals.playstage.GameActor
import ctmn.petals.tile.components.*
import ctmn.petals.unit.component.*
import ctmn.petals.utils.gson
import ctmn.petals.utils.serialization.Jsonable
import java.io.FileNotFoundException
import java.util.*
import kotlin.random.Random

open class TileActor(
    tileName: String = "",
    terrain: String = "",
    layer: Int = 1,
    tiledX: Int = 0,
    tiledY: Int = 0,
    val assets: Assets = ctmn.petals.assets,
) : GameActor(), Jsonable {

    private val cViewMapper: ComponentMapper<TileViewComponent> = ComponentMapper.getFor(TileViewComponent::class.java)
    private val cTileMapper: ComponentMapper<TileComponent> = ComponentMapper.getFor(TileComponent::class.java)

    val tileComponent by lazy { cTileMapper.get(components) }
    val tileViewComponent get() = cViewMapper.get(components)

    val tileName: String get() = tileComponent.name
    val terrain: String get() = tileComponent.terrain
    var layer: Int
        get() = tileComponent.layer
        set(value) {
            tileComponent.layer = value
        }

    var tiledX: Int
        get() = tileComponent.tiledX
        set(value) {
            tileComponent.tiledX = value
        }
    var tiledY: Int
        get() = tileComponent.tiledY
        set(value) {
            tileComponent.tiledY = value
        }

    val sprite: Sprite get() = tileViewComponent!!.sprite

    val animation: RegionAnimation? get() = tileViewComponent?.animation

    val isCombinable get() = Decorator.isTileCombinable(this)
        //assets.atlases.findRegion("tiles/${terrain}/" + tileName + "_combinable") != null

    constructor(tileData: Tile, layer: Int, tileX: Int, tileY: Int) : this(
        tileData.name,
        tileData.terrain,
        layer,
        tileX,
        tileY
    )

    init {
        components.add(TileComponent(tileName, terrain, layer, tiledX, tiledY))

        // set actor size
        width = TILE_SIZE
        height = TILE_SIZE

        //
        val health = when {
            terrain == TerrainNames.forest -> 150
            terrain == TerrainNames.building -> 200
            terrain == TerrainNames.crystals -> 400
            terrain == TerrainNames.walls -> 300
            terrain == TerrainNames.tower -> 200
            tileName.startsWith(Tiles.BRIDGE) -> 150
            tileName.startsWith(Tiles.WATERLILY) -> 150
            else -> 0
        }

        if (health > 0) {
            add(HealthComponent(health))
        }
    }

    fun initView() {
        // load tile texture
        var textures = assets.atlases.findRegions("tiles/$tileName")

        var nameNoSuffix = tileName.lowercase(Locale.ROOT)
        "abcdefghijklmnop".forEach { nameNoSuffix = nameNoSuffix.removeSuffix("_$it") }

        // if tile texture is not found, try to find it in terrain folder
        // if not found, try to remove last letter from tile name that indicates combination
        if (textures.isEmpty) {
            textures = assets.atlases.findRegions("tiles/${terrain}/" + tileName)

            if (textures.isEmpty) {
                textures = assets.atlases.findRegions("tiles/$nameNoSuffix")

                if (textures.isEmpty) throw FileNotFoundException("Tile texture not found: $nameNoSuffix")
            }
        }

        // make animation
        // MARK: Change tile animation time
        if (textures.size > 1) {
            val frameDuration = when (terrain) {
                "forest" -> 0.5f
                "grass" -> 1f
                else -> 0.4f
            }
            val animation = RegionAnimation(frameDuration, textures)
            add(TileViewComponent(Sprite(animation.currentFrame), animation))
        } else {
            if (!isCombinable) {
                if (terrain != TerrainNames.roads) {
                    fun findRegAndAdd(suffix: String) {
                        assets.atlases.findRegion("tiles/${terrain}/" + nameNoSuffix + suffix)?.let {
                            textures.add(it)
                        }
                    }
                    findRegAndAdd("_a")
                    findRegAndAdd("_b")
                    findRegAndAdd("_c")
                    findRegAndAdd("_d")
                    findRegAndAdd("_e")
                    findRegAndAdd("_f")
                }
            }

            add(TileViewComponent(Sprite(textures.random())))
        }

        if (tileViewComponent == null) {
            throw IllegalStateException("Tile cView has not been initialized. tiles/${terrain}/$tileName")
        }

        // resize sprite if it is 24x24
        if (sprite.height == 24f)
            sprite.setSize(16f, 16f)
        else
            if (sprite.width == 24f)
                sprite.setSize(16f, 16f)

//        if (sprite.width < TILE_SIZE)
//            sprite.setSize(TILE_SIZE.toFloat(), TILE_SIZE.toFloat())
        if (sprite.width == 64f)
            sprite.setSize(TILE_SIZE * 1f, TILE_SIZE * 1f)

        if (sprite.width == 128f)
            sprite.setSize(TILE_SIZE * 2f, TILE_SIZE * 2f)

        if (sprite.width == 48f)
            sprite.setSize(32f, 32f)

        sprite.setOriginCenter()

        setPosition(tiledX, tiledY)
    }

    override fun act(delta: Float) {
        super.act(delta)

        animation?.update(delta)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        if (animation != null) {
            sprite.setRegion(animation!!.currentFrame)
        }

        sprite.draw(batch)
    }

    override fun toJsonObject(): JsonObject {
        val unitJsonObject = JsonObject()

        unitJsonObject.addProperty("name", tileComponent.name)
        unitJsonObject.addProperty("id", name)

        for (component in components.components) {
            if (component is ViewComponent) continue
            if (component is TileViewComponent) continue

            val json = gson.toJsonTree(component)
            unitJsonObject.add(component.javaClass.simpleName, json)
        }

        return unitJsonObject
    }

    override fun fromJsonObject(json: JsonObject) {
        //tileComponent.name = json.get("name").asString
        name = json.get("id")?.asString ?: name

        for ((name, value) in json.entrySet()) {
            when (name) {
                "TileComponent" -> components.add(gson.fromJson(value, TileComponent::class.java))
                "CapturingComponent" -> components.add(gson.fromJson(value, CapturingComponent::class.java))
                "LifeTimeComponent" -> components.add(gson.fromJson(value, LifeTimeComponent::class.java))
                "PlayerIdComponent" -> components.add(gson.fromJson(value, PlayerIdComponent::class.java))
                "ReplaceWithComponent" -> components.add(gson.fromJson(value, ReplaceWithComponent::class.java))
                "ActionCooldown" -> components.add(gson.fromJson(value, ActionCooldown::class.java))
                "BaseBuildingComponent" -> components.add(gson.fromJson(value, BaseBuildingComponent::class.java))
                "BuildingComponent" -> components.add(gson.fromJson(value, BuildingComponent::class.java))
                "DestroyingComponent" -> components.add(gson.fromJson(value, DestroyingComponent::class.java))
                "CrystalsComponent" -> components.add(gson.fromJson(value, CrystalsComponent::class.java))
                "HealthComponent" -> components.add(gson.fromJson(value, HealthComponent::class.java))
            }
        }

        setPosition(tiledX, tiledY)
    }

    fun setPosition(tileX: Int, tileY: Int) {
        val x = tileX.unTiled()
        val y = tileY.unTiled()

        if (x == this.x && y == this.y)
            positionChanged()
        else
            setPosition(x, y)
    }

    override fun positionChanged() {
        super.positionChanged()

        tiledX = (x / TILE_SIZE).toInt()
        tiledY = (y / TILE_SIZE).toInt()

        if (tileViewComponent != null) {
            sprite.setPositionByCenter(x + TILE_SIZE / 2, y + TILE_SIZE / 2)

            if (terrain == TerrainNames.forest) {
                sprite.x += Random.nextInt(-2, 2)
                sprite.y += Random.nextInt(-2, 2)
            }
        }
    }

    override fun toString(): String {
        return "TileActor[nm: $tileName, terrain: $terrain, x: $tiledX, y: $tiledY, l: $layer]"
    }

    fun makeCopy(): TileActor {
        val tileCopy = TileActor()
        tileCopy.name = this.name
        for (component in components.components) {
            tileCopy.add(
                when (component) {
                    is TileComponent -> component.copy()
                    is CapturingComponent -> component.copy()
                    is LifeTimeComponent -> component.copy()
                    is PlayerIdComponent -> component.copy()
                    is ReplaceWithComponent -> component.copy()
                    is BaseBuildingComponent -> component.copy()
                    is DestroyingComponent -> component.copy()
                    is BuildingComponent -> component.copy()
                    is HealthComponent -> component.copy()
                    else -> continue
                }
            )
        }

        return tileCopy
    }
}
