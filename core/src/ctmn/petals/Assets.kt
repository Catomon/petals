package ctmn.petals

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import ctmn.petals.tile.TileData
import ctmn.petals.unit.playerColorName
import ctmn.petals.utils.log
import ctmn.petals.utils.replaceColor

lateinit var assets: Assets

class Assets : AssetManager() {

    companion object {
        const val soundsFolderName = "sounds"
        const val musicFolderName = "music"

        const val UNITS_ATLAS = "units.atlas"
        const val TILES_ATLAS = "tiles.atlas"
        const val EFFECTS_ATLAS = "effects.atlas"
        const val MISC_ATLAS = "misc.atlas"
    }

    val atlases by lazy { Atlases() }
    lateinit var unitsAtlas: TextureAtlas
    lateinit var tilesAtlas: TextureAtlas
    lateinit var effectsAtlas: TextureAtlas
    lateinit var miscAtlas: TextureAtlas
    lateinit var guiAtlas: TextureAtlas

    inner class Atlases {
        val textures
            get() = Array<Texture>().apply {
                arrayOf(
                    unitsAtlas,
                    effectsAtlas,
                    tilesAtlas,
                    guiAtlas,
                    miscAtlas
                ).forEach { it.textures.forEach { add(it) } }
            }

        fun getRegion(name: String) = findRegion(name) ?: throw IllegalArgumentException("Region $name not found")

        fun findRegion(name: String) = findRegions(name).firstOrNull()

        fun findRegions(name: String): Array<TextureAtlas.AtlasRegion> {
            val folder = name.split("/").firstOrNull()
            val regionName = name.substringAfter("/")

            return when (folder) {
                "units" -> unitsAtlas.findRegions(regionName)
                "effects" -> effectsAtlas.findRegions(regionName)
                "tiles" -> tilesAtlas.findRegions(regionName)
                "gui" -> guiAtlas.findRegions(regionName)
                "misc" -> miscAtlas.findRegions(regionName)
                else -> miscAtlas.findRegions(regionName) //throw IllegalArgumentException("No atlas found for $name")
            }
        }
    }

    override fun update(): Boolean {
        val done = super.update()

        if (done && !finishedLoading) {
            onFinishLoading()
            finishedLoading = true
        }

        return done
    }

    private var finishedLoading = false

    private fun onFinishLoading() {
        unitsAtlas = get(UNITS_ATLAS)
        tilesAtlas = get(TILES_ATLAS)
        effectsAtlas = get(EFFECTS_ATLAS)
        miscAtlas = get(MISC_ATLAS)
        guiAtlas = VisUI.getSkin().atlas

        addUnitAtlases()

        //..
//        for (texture in atlases.textures)
//            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        TileData.parseTiles()
    }

    //what color of standard(blue player) replace to other color
    private val redPlayerColors = arrayOf(
        // faerie
        -10763539 to -1221769, // bright blue
        -12022828 to -2863800, // shadow blue
        -13865041 to -5296852, // dark line blue
        -16765819 to -8055040 // very dark line blue
        //goblin
    )

    private val bluePlayerColors = arrayOf(
        -9230787 to -13865041,
        -11203807 to -16561522,
    )

    fun findUnitAtlas(playerColor: String): TextureAtlas {
        val assetName = "units_$playerColor.atlas"
        if (!contains(assetName)) {
            log("Unit colors for player id not found: $playerColor")
            return unitsAtlas
        }
        return get(assetName)
    }

    fun findUnitAtlas(playerId: Int): TextureAtlas = findUnitAtlas(playerColorName(playerId))

    private fun addUnitAtlases() {
        addAsset("units_blue.atlas", TextureAtlas::class.java, createUnitsAtlas(bluePlayerColors))
        addAsset("units_red.atlas", TextureAtlas::class.java, createUnitsAtlas(redPlayerColors))
    }

    private fun createUnitsAtlas(colors: kotlin.Array<Pair<Int, Int>>): TextureAtlas {
        val atl = TextureAtlas(UNITS_ATLAS)
        val texture = atl.textures.first()
        texture.textureData.prepare()
        val pixmap = texture.textureData.consumePixmap()

        colors.forEach {
            pixmap.replaceColor(it.first, it.second)
        }

        val modifiedTexture = Texture(pixmap)

        atl.textures.remove(texture)
        atl.textures.add(modifiedTexture)
        atl.regions.forEach { it.texture = modifiedTexture }

        texture.dispose()
        pixmap.dispose()

        return atl
    }

    fun beginLoadingAll() {
        Gdx.app.debug(Assets::class.simpleName, "Loading textures...")

        //texture atlas
        load(UNITS_ATLAS, TextureAtlas::class.java)
        load(TILES_ATLAS, TextureAtlas::class.java)
        load(EFFECTS_ATLAS, TextureAtlas::class.java)
        load(MISC_ATLAS, TextureAtlas::class.java)

        // images
        load("sky.png", Texture::class.java)
        load("faeries.png", Texture::class.java)
        load("bunny.png", Texture::class.java)
        load("1.png", Texture::class.java)
        load("2.png", Texture::class.java)
        load("3.png", Texture::class.java)
        load("4.png", Texture::class.java)

        Gdx.app.debug(Assets::class.simpleName, "Loading textures... Done")

        Gdx.app.debug(Assets::class.simpleName, "Loading sounds...")
        fun loadSound(name: String) {
            load("$soundsFolderName/$name", Sound::class.java)
        }
        loadSound("click.ogg")
        loadSound("heal_up.ogg")
        loadSound("hit.ogg")
        loadSound("unit_explosion.ogg")
        loadSound("unit_select.ogg")
        loadSound("unit_deselect.ogg")

        Gdx.app.debug(Assets::class.simpleName, "Loading sounds... Done")

//FileHandle.list() not working in jar and isDirectory returns false even if it's a directory
//        for (file in Gdx.files.internal(soundsFolderName).list()) {
//            if (file.extension() != "ogg")
//                continue
//
//            load("$soundsFolderName/${file.name()}", Sound::class.java)
//            Gdx.app.log(Assets::class.simpleName, "Loading $soundsFolderName/${file.name()}")
//        }
    }

    fun loadUI() {
        when (GamePref.locale) {
            //"ru" -> VisUI.load(Gdx.files.internal("skin/wafer-ui_ru.json"))
            else -> VisUI.load(Gdx.files.internal("skin/wafer-ui.json"))
        }

        //VisUI.getSkin().getFont("font").data.setScale(0.3f)
        //VisUI.getSkin().getFont("font_12").data.setScale(0.22f)
//        VisUI.getSkin().getFont("font").data.setScale(0.22f)
//        VisUI.getSkin().getFont("font_8").data.setScale(0.5f)
//        VisUI.getSkin().getFont("font_5").data.setScale(0.5f) //0.35

        //VisUI.getSkin().setScale(Const.GUI_SCALE)
//        VisUI.getSkin().setScale(0.5f)
    }

    fun getTexture(name: String): Texture = get(name, Texture::class.java)

    fun findAtlasRegion(name: String): TextureAtlas.AtlasRegion = atlases.findRegion(name)!!
    fun findAtlasRegions(name: String): Array<TextureAtlas.AtlasRegion> = atlases.findRegions(name)!!

    fun getSound(name: String): Sound = get("$soundsFolderName/$name")

    fun getMusic(name: String): Music = get("$musicFolderName/$name")

    fun getDrawable(name: String): Drawable = VisUI.getSkin().getDrawable(name)

    fun generateFont(name: String = "Pixel.ttf", size: Int = 16, color: Color = Color.WHITE): BitmapFont {
        val font: BitmapFont
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/$name"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = size
        parameter.minFilter = Texture.TextureFilter.Linear
        parameter.magFilter = Texture.TextureFilter.Linear
        font = generator.generateFont(parameter)
        font.color = color
        font.setUseIntegerPositions(false)
        generator.dispose()
        return font
    }
}

object AudioManager {

    var soundVolume = GamePref.soundVolume

    var musicVolume = GamePref.musicVolume
        set(value) {
            field = value
            currentMusic?.volume = value
            currentMusic2?.volume = value
        }

    var currentMusic: Music? = null
    var currentMusic2: Music? = null

    fun music(name: String): Music {
        if (currentMusic == null) {
            currentMusic = Gdx.audio.newMusic(Gdx.files.internal("music/$name"))
            currentMusic?.volume = musicVolume
            return currentMusic!!
        } else {
            currentMusic2 = Gdx.audio.newMusic(Gdx.files.internal("music/$name"))
            currentMusic2?.volume = musicVolume
            return currentMusic2!!
        }
    }

    fun disposeMusic() {
        currentMusic?.dispose()
        currentMusic2?.dispose()
    }

    fun sound(name: String): Long {
        return sound(assets.getSound("$name.ogg"))
    }

    private fun sound(sound: Sound): Long {
        return sound.play(soundVolume)
    }
}