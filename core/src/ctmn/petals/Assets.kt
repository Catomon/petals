package ctmn.petals

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import ctmn.petals.tile.TileData
import ctmn.petals.unit.playerColorName
import ctmn.petals.utils.err
import ctmn.petals.utils.log
import ctmn.petals.utils.replaceColor
import ctmn.petals.utils.rgba

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

    lateinit var tilesSummerAtlas: TextureAtlas
    lateinit var tilesWinterAtlas: TextureAtlas

    inner class Atlases {
//        val textures
//            get() = Array<Texture>().apply {
//                arrayOf(
//                    unitsAtlas,
//                    effectsAtlas,
//                    tilesAtlas,
//                    guiAtlas,
//                    miscAtlas
//                ).forEach { it.textures.forEach { add(it) } }
//            }

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

//        if (done && !finishedLoading) {
//            onFinishLoading()
//            finishedLoading = true
//        }

        return done
    }

    private var finishedLoading = false

    fun onFinishLoading() {
        unitsAtlas = get(UNITS_ATLAS)
        tilesAtlas = get(TILES_ATLAS) //get("tiles_winter.atlas")
        effectsAtlas = get(EFFECTS_ATLAS)
        miscAtlas = get(MISC_ATLAS)
        guiAtlas = VisUI.getSkin().atlas

        tilesSummerAtlas = get(TILES_ATLAS)
        tilesWinterAtlas = get("tiles_winter.atlas")

        addUnitAtlases()

        //..
//        for (texture in atlases.textures)
//            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        TileData.parseTiles()
    }

    fun findUnitAtlas(playerColor: String): TextureAtlas {
        val assetName = "units_$playerColor.atlas"
        if (!contains(assetName)) {
            if (playerColor != "-1") err("Unit colors for player id not found: $playerColor")
            return unitsAtlas
        }
        return get(assetName)
    }

    fun findUnitAtlas(playerId: Int): TextureAtlas = findUnitAtlas(playerColorName(playerId))

    private fun addUnitAtlases() {
        log("Loading units atlases...")

        loadUnitsAtlasByColor("units_blue", bluePlayerColors)
        loadUnitsAtlasByColor("units_red", redPlayerColors)
        loadUnitsAtlasByColor("units_green", greenPlayerColors)
        loadUnitsAtlasByColor("units_purple", purplePlayerColors)
        loadUnitsAtlasByColor("units_yellow", yellowPlayerColors)
        loadUnitsAtlasByColor("units_orange", orangePlayerColors)
        loadUnitsAtlasByColor("units_pink", pinkPlayerColors)
        loadUnitsAtlasByColor("units_brown", brownPlayerColors)

        log("Loading units atlases V")
    }

    fun beginLoadingAll() {
        Gdx.app.debug(Assets::class.simpleName, "Loading textures...")

        //texture atlas
        load(UNITS_ATLAS, TextureAtlas::class.java)
        load(TILES_ATLAS, TextureAtlas::class.java)
        load(EFFECTS_ATLAS, TextureAtlas::class.java)
        load(MISC_ATLAS, TextureAtlas::class.java)

        load("tiles_winter.atlas", TextureAtlas::class.java)

        // images
        load("sky.png", Texture::class.java)
        load("faeries.png", Texture::class.java)
        load("bunny.png", Texture::class.java)
        load("1.png", Texture::class.java)
        load("2.png", Texture::class.java)
        load("3.png", Texture::class.java)
        load("4.png", Texture::class.java)

        Gdx.app.debug(Assets::class.simpleName, "Loading textures V")

        Gdx.app.debug(Assets::class.simpleName, "Loading sounds...")
        fun loadSound(name: String) {
            load("$soundsFolderName/$name", Sound::class.java)
        }
        loadSound("click.ogg")
        loadSound("heal_up.ogg")
        loadSound("unit_explosion.ogg")
        loadSound("unit_select.ogg")
        loadSound("unit_deselect.ogg")
        loadSound("step_grass.ogg")
        loadSound("step_water.ogg")
        loadSound("step_rock.ogg")
        loadSound("step_forest.ogg")
        loadSound("step_forest_1.ogg")
        loadSound("flee.ogg")
        loadSound("hit.ogg")
        loadSound("hit_2.ogg")
        loadSound("slime_hit.ogg")
        loadSound("bow.ogg")

        Gdx.app.debug(Assets::class.simpleName, "Loading sounds V")

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

//        VisUI.getSkin().getFont("font").data.setScale(2f)
//        VisUI.getSkin().getFont("quaver").data.setScale(2f)
//        VisUI.getSkin().getFont("font_8").data.setScale(2f)
//        VisUI.getSkin().getFont("font_5").data.setScale(2f)
//        VisUI.getSkin().getFont("default-font").data.setScale(2f)

        //VisUI.getSkin().setScale(Const.GUI_SCALE)
//        VisUI.getSkin().setScale(0.5f)
    }

    fun getTexture(name: String): Texture = get(name, Texture::class.java)

    fun findAtlasRegion(name: String): TextureAtlas.AtlasRegion =
        atlases.findRegion(name) ?: throw IllegalArgumentException("Atlas region '$name' not found.")

    fun findAtlasRegions(name: String): Array<TextureAtlas.AtlasRegion> = atlases.findRegions(name)

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

    /// /// /// /// /// // /// /// //
    ///
    ///////// // // // /  // / / / /

    private fun loadUnitsAtlasByColor(name: String, colors: kotlin.Array<Pair<Color, Color>>) {
        var atlOrNull: TextureAtlas? = null
        Gdx.app.postRunnable {
            try {
                atlOrNull = TextureAtlas(UNITS_ATLAS)
            } catch (e: Exception) {
                e.printStackTrace()
                Gdx.app.exit()
            }
        }
        while (atlOrNull == null) {
            Thread.sleep(50)
        }
        val atl = atlOrNull!!

        val textureFile = Gdx.files.internal("$name.png")
        if (textureFile.exists()) {
            atl.textures.clear()

            var texture: Texture? = null
            Gdx.app.postRunnable {
                try {
                    texture = Texture(textureFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Gdx.app.exit()
                }
            }

            while (texture == null) {
                Thread.sleep(10)
            }

            atl.textures.add(texture)
            atl.regions.forEach { it.texture = texture }
        } else if (Const.IS_DESKTOP) {
            print("Creating $name units texture")
            try {
                createAndSaveUnitsTexture(atl, colors, name)
                log(" V")
            } catch (e: Exception) {
                log(" X")
                e.printStackTrace()
            }
        } else err("$name units texture is missing and won't be created (non-desktop device).")

        addAsset("$name.atlas", TextureAtlas::class.java, atl)
    }

    private fun createAndSaveUnitsTexture(
        atl: TextureAtlas,
        colors: kotlin.Array<Pair<Color, Color>>,
        name: String,
    ): TextureAtlas {
        val texture = atl.textures.first()
        texture.textureData.prepare()
        val pixmap = texture.textureData.consumePixmap()

        colors.forEach {
            pixmap.replaceColor(it.first, it.second)
        }

        var modifiedTextureOrNull: Texture? = null
        Gdx.app.postRunnable {
            try {
                modifiedTextureOrNull = Texture(pixmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Gdx.app.exit()
            }
        }

        while (modifiedTextureOrNull == null) {
            Thread.sleep(50)
        }
        val modifiedTexture = modifiedTextureOrNull!!

        atl.textures.remove(texture)
        atl.textures.add(modifiedTexture)
        atl.regions.forEach { it.texture = modifiedTexture }

        Gdx.app.postRunnable {
            try {
                texture.dispose()
            } catch (e: Exception) {
                e.printStackTrace()
                Gdx.app.exit()
            }
        }


        try {
            PixmapIO.writePNG(Gdx.files.local("$name.png"), pixmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Gdx.app.exit()
        }


        pixmap.dispose()
        return atl
    }

    //what color of standard(blue player) replace to other color
    private val redPlayerColors = arrayOf(
        // faerie
        Color().rgba(91, 194, 237) to Color().rgba(237, 91, 119), // bright blue
        Color().rgba(72, 139, 212) to Color().rgba(212, 77, 72), // shadow blue
        Color().rgba(44, 111, 175) to Color().rgba(175, 45, 44), // dark line blue
        Color().rgba(0, 48, 100) to Color().rgba(133, 23, 0) // very dark line blue
        //goblin x
    )

    private val bluePlayerColors = arrayOf(
        //faerie x
        //goblin
        Color().rgba(115, 38, 61) to Color().rgba(44, 111, 175), // light red
        Color().rgba(91, 26, 45) to Color().rgba(26, 62, 91), // darker red
        Color().rgba(85, 11, 33) to Color().rgba(3, 74, 172), // dark red
        Color().rgba(40, 29, 43) to Color().rgba(13, 8, 44), // very dark red
    )

    private val greenPlayerColors = arrayOf(
        //faerie
        Color().rgba(91, 194, 237) to Color().rgba(91, 237, 140), // bright blue
        Color().rgba(72, 139, 212) to Color().rgba(44, 189, 129), // shadow blue
        Color().rgba(44, 111, 175) to Color().rgba(19, 148, 34), // dark line blue
        Color().rgba(0, 48, 100) to Color().rgba(0, 64, 38), // very dark line blue
        //goblin
        Color().rgba(115, 38, 61) to Color().rgba(38, 115, 84), // light red
        Color().rgba(91, 26, 45) to Color().rgba(26, 91, 65), // darker red
        Color().rgba(85, 11, 33) to Color().rgba(11, 85, 56), // dark red
        Color().rgba(40, 29, 43) to Color().rgba(19, 44, 8), // very dark red
    )

    private val purplePlayerColors = arrayOf(
        //faerie
        Color().rgba(91, 194, 237) to Color().rgba(193, 91, 237), // bright blue
        Color().rgba(72, 139, 212) to Color().rgba(146, 72, 212), // shadow blue
        Color().rgba(44, 111, 175) to Color().rgba(125, 44, 175), // dark line blue
        Color().rgba(0, 48, 100) to Color().rgba(64, 0, 100), // very dark line blue
        //goblin
        Color().rgba(115, 38, 61) to Color().rgba(104, 38, 115), // light red
        Color().rgba(91, 26, 45) to Color().rgba(77, 26, 91), // darker red
        Color().rgba(85, 11, 33) to Color().rgba(60, 11, 85), // dark red
        Color().rgba(40, 29, 43) to Color().rgba(37, 8, 44), // very dark red
    )

    private val yellowPlayerColors = arrayOf(
        //faerie
        Color().rgba(91, 194, 237) to Color().rgba(237, 229, 91), // bright blue
        Color().rgba(72, 139, 212) to Color().rgba(212, 179, 72), // shadow blue
        Color().rgba(44, 111, 175) to Color().rgba(192, 134, 46), // dark line blue
        Color().rgba(0, 48, 100) to Color().rgba(100, 60, 0), // very dark line blue
        //goblin
        Color().rgba(115, 38, 61) to Color().rgba(115, 115, 38), // light red
        Color().rgba(91, 26, 45) to Color().rgba(91, 82, 26), // darker red
        Color().rgba(85, 11, 33) to Color().rgba(85, 60, 11), // dark red
        Color().rgba(40, 29, 43) to Color().rgba(44, 29, 8), // very dark red
    )

    private val orangePlayerColors = arrayOf(
        //faerie
        Color().rgba(91, 194, 237) to Color().rgba(237, 147, 91), // bright blue
        Color().rgba(72, 139, 212) to Color().rgba(212, 107, 72), // shadow blue
        Color().rgba(44, 111, 175) to Color().rgba(148, 56, 19), // dark line blue
        Color().rgba(0, 48, 100) to Color().rgba(100, 0, 4), // very dark line blue
        //goblin
        Color().rgba(115, 38, 61) to Color().rgba(133, 48, 0), // light red
        Color().rgba(91, 26, 45) to Color().rgba(112, 28, 0), // darker red
        Color().rgba(85, 11, 33) to Color().rgba(97, 6, 0), // dark red
        Color().rgba(40, 29, 43) to Color().rgba(70, 0, 0), // very dark red
    )

    private val pinkPlayerColors = arrayOf(
        //faerie
        Color().rgba(91, 194, 237) to Color().rgba(237, 91, 213), // bright blue
        Color().rgba(72, 139, 212) to Color().rgba(212, 72, 158), // shadow blue
        Color().rgba(44, 111, 175) to Color().rgba(148, 19, 98), // dark line blue
        Color().rgba(0, 48, 100) to Color().rgba(100, 0, 61), // very dark line blue
        //goblin
        Color().rgba(115, 38, 61) to Color().rgba(153, 0, 99), // light red
        Color().rgba(91, 26, 45) to Color().rgba(117, 0, 76), // darker red
        Color().rgba(85, 11, 33) to Color().rgba(96, 0, 62), // dark red
        Color().rgba(40, 29, 43) to Color().rgba(52, 0, 34), // very dark red
    )

    private val brownPlayerColors = arrayOf(
        //faerie
        Color().rgba(91, 194, 237) to Color().rgba(209, 153, 118), // bright blue
        Color().rgba(72, 139, 212) to Color().rgba(199, 129, 85), // shadow blue
        Color().rgba(44, 111, 175) to Color().rgba(125, 74, 42), // dark line blue
        Color().rgba(0, 48, 100) to Color().rgba(75, 44, 25), // very dark line blue
        //goblin
        Color().rgba(115, 38, 61) to Color().rgba(115, 68, 38), // light red
        Color().rgba(91, 26, 45) to Color().rgba(88, 52, 29), // darker red
        Color().rgba(85, 11, 33) to Color().rgba(72, 42, 24), // dark red
        Color().rgba(40, 29, 43) to Color().rgba(39, 23, 13), // very dark red
    )
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

    fun music(name: String, slot: Int = -1): Music {
        if (currentMusic == null || slot == 0) {
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