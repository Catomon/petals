package ctmn.petals.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisImageButton
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.assets
import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.level.Level
import ctmn.petals.player.Player
import ctmn.petals.tile.Terrain
import ctmn.petals.tile.TileActor
import ctmn.petals.utils.tiled


class MapPreview(var level: Level? = null) : WidgetGroup() {

    var drawSimpleTexture = false
        set(value) { field = value; if (!drawSimpleTexture) sprite.color = Color.CLEAR }

    val actors = Array<Actor>()
    val texture = assets.findAtlasRegion("gui/white")
    val sprite = Sprite(texture)
    val bunny = assets.findAtlasRegion("gui/images/bunny")

    private var tileSize = 1f
        set(value) { field = value; sprite.setSize(tileSize, tileSize) }
    private var mapWidth = 0f
    private var mapHeight = 0f
    private var mapOffX = 0f
    private var mapOffY = 0f

    private val background = assets.getDrawable("background")

    var markButtons = Array<VisImageButton>()

    init {
        setSize(300f, 300f)

        sprite.setSize(tileSize, tileSize)

        level?.let { setPreview(it) }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.color = Color.WHITE
        background.draw(batch, x, y, width, height)

        drawMap(batch)

        super.draw(batch, parentAlpha)
    }

    fun changePlayerMark(player: Player?, labelId: Int) {
        markButtons.forEach {
            if (it.userObject == labelId) {
                if (player != null)
                    it.style.imageUp = assets.getDrawable("map_preview_player_mark_${player.id}")
                else
                    it.style.imageUp = null
            }
        }
    }

    fun createPlayerMarks() {
        markButtons.forEach { removeActor(it) }
        markButtons.clear()

        for (actor in actors) {
            if (actor is LabelActor && actor.labelName == "player") {
                val position = Vector2(mapOffX + actor.x.tiled() * tileSize, mapOffY + actor.y.tiled() * tileSize)

                val mark = newImageButton("map_preview_mark")
                mark.setPosition(position.x, position.y)
                mark.userObject = actor.data["id"].toInt()
                addActor(mark)

                markButtons.add(mark)
            }
        }
    }

    private fun drawMap(batch: Batch) {
        //Create scissors rectangle that covers my Actor.
        val scissors = Rectangle()
        val n = 6.8f
        val clipBounds = Rectangle(x + n - 0.4f, y + n, width - n * 2, height - n * 2)
        ScissorStack.calculateScissors(stage.camera, batch.transformMatrix, clipBounds, scissors)

        batch.flush() //Make sure nothing is clipped before we want it to.

        ScissorStack.pushScissors(scissors)

        //Draw the actor as usual
        if (!actors.isEmpty)
            for (actor in actors) {
                if (actor is TileActor) {
                    if (drawSimpleTexture)
                        when (actor.terrain) {
                            Terrain.grass -> sprite.color = Color.GREEN
                            Terrain.forest -> sprite.color = Color.LIME
                            Terrain.water -> sprite.color = Color.BLUE
                            Terrain.mountains -> sprite.color = Color.BROWN
                            else -> sprite.color = Color.GRAY
                        }
                    else sprite.setRegion(actor.sprite)

                    val size = actor.sprite.width / TILE_SIZE
                    sprite.setSize(tileSize * size, tileSize * size)
                    sprite.setPosition(mapOffX + x + actor.x.tiled() * tileSize - (sprite.width - tileSize) / 2, mapOffY + y + actor.y.tiled() * tileSize - (sprite.width - tileSize) / 2)
                    sprite.draw(batch)
                }
            }
        else batch.draw(bunny, x + 7f, y + 8f)

        batch.flush() //Make sure nothing is clipped before we want it to.

        //Perform the actual clipping
        ScissorStack.popScissors()
    }

    fun setPreview(level: Level?) {
        this.level = level

        actors.clear()

        if (level == null) return

        //render priority order
        val backTiles = Array<TileActor>()
        val tiles = Array<TileActor>()
        val afterTiles = Array<TileActor>()

        for (tile in level.tiles) {
            if (tile.layer == 1)
                tiles.add(tile)
            else {
                if (tile.layer < 1)
                    backTiles.add(tile)
                else afterTiles.add(tile)
            }
        }
        //add
        actors.addAll(backTiles)
        actors.addAll(tiles)
        actors.addAll(afterTiles)
        actors.addAll(level.units)
        actors.addAll(level.labels)
        //

        //calc map size, tile size and offset in pixels to make it fit in bounds of widget
        //width
        var tilesWidth = 0
        for (tile in level.tiles) {
            if (tile.tiledX > tilesWidth)
                tilesWidth = tile.tiledX
        }
        mapWidth =  tilesWidth + 1f

        //height
        var tilesHeight = 0
        for (tile in level.tiles) {
            if (tile.tiledY > tilesHeight)
                tilesHeight = tile.tiledY
        }
        mapHeight = tilesHeight + 1f

        //tile size for rendering
        val bestSize = if (mapHeight > mapWidth) mapHeight else mapWidth
        val thisLeastSize = if (this.width < this.height) this.width else this.height
        tileSize = thisLeastSize / bestSize * 0.95f

        //offset for rendering
        mapOffX = width / 2 - mapWidth / 2 * tileSize
        mapOffY = height / 2 - mapHeight / 2 * tileSize
    }
}
