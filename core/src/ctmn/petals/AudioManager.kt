package ctmn.petals

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

object AudioManager {

    class GameMusic(val name: String, val music: Music) {
        fun play() {
            music.play()
        }

        fun pause() {
            music.pause()
        }
    }

    var soundVolume = GamePref.soundVolume

    var musicVolume = GamePref.musicVolume
        set(value) {
            field = value
            music?.music?.volume = value
        }

    var ambienceVolume = GamePref.musicVolume
        set(value) {
            field = value
            ambience?.music?.volume = value
        }

    var music: GameMusic? = null
    var ambience: GameMusic? = null

    fun createMusic(name: String): GameMusic {
        if (music?.name == name) return music!!;
        music = GameMusic(name, Gdx.audio.newMusic(Gdx.files.internal("music/$name")))
        music?.music?.volume = musicVolume
        music?.music?.isLooping = true
        return music!!
    }

    fun createAmbience(name: String): GameMusic {
        if (ambience?.name == name) return ambience!!;
        ambience = GameMusic(name, Gdx.audio.newMusic(Gdx.files.internal("music/$name")))
        ambience?.music?.volume = musicVolume * 2f
        music?.music?.isLooping = true
        return ambience!!
    }

    fun disposeMusic() {
        ambience?.music?.dispose()
    }

    fun disposeAmbience() {
        music?.music?.dispose()
    }

    fun playSound(name: String): Long {
        return playSound(assets.getSound("$name.ogg"))
    }

    private fun playSound(sound: Sound): Long {
        return sound.play(soundVolume)
    }
}