package ctmn.petals.story

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.google.gson.Gson
import com.google.gson.JsonObject
import ctmn.petals.Const
import ctmn.petals.GamePref
import ctmn.petals.utils.*

object SavesManager {

    val savesFolder: FileHandle = GamePref.savesFolder
    const val SAVE_EXT = ".save"

    fun first(): StorySaveGson? {
        return getByFileName(savesFolder.list().firstOrNull()?.nameWithoutExtension() ?: return null)
    }

    fun isSingleSave(): Boolean {
        val folder = savesFolder
        return folder.list().size == 1
    }

    fun anyExists(): Boolean {
        val folder = savesFolder
        return folder.list().isNotEmpty()
    }

    fun getAll(): Array<StorySaveGson> {
        val saves = Array<StorySaveGson>()
        val folder = savesFolder.list()
        for (save in folder)
            saves.add(Gson().fromJson(decryptStoryToJson(save.readString()), StorySaveGson::class.java))

        return saves
    }

    @Deprecated("Don't")
    inline fun <reified T : StorySaveGson> getByFileName(fileName: String): T? {
        if (fileName == "") throw IllegalArgumentException("fileName can't be empty.")

        val string = decryptStoryToJson(savesFolder.readString())
        checkSaveJsonVer(string)

        return Gson().fromJson(decryptStoryToJson(savesFolder.child("$fileName$SAVE_EXT").readString()), T::class.java)
    }


    @Deprecated("Don't")
    inline fun <reified T : StorySaveGson> getByName(saveName: String): T? {
        val folder = savesFolder

        for (savePath in folder.list()) {
            val storySaveGson = Gson().fromJson(decryptStoryToJson(savePath.readString()), T::class.java)
            if (storySaveGson.save_name == saveName) {
                val string = decryptStoryToJson(savePath.readString())
                checkSaveJsonVer(string)
                return storySaveGson
            }
        }

        return null
    }

    fun checkSaveJsonVer(jsonString: String) : Int {
        val json = Gson().fromJson(jsonString, JsonObject::class.java)
        if (json.has("game_version")) {
            val ver = Version.compareVersions(Const.APP_VER_NAME, json.get("game_version").asString)
            if (ver > 0)
                logErr("Warn: Save version is older than the game version")

            return ver
        }

        return -1
    }

    inline fun <reified T : StorySaveGson> getByStoryId(storyId: Int): T? {
        val folder = savesFolder

        try {
            for (savePath in folder.list()) {
                val string = decryptStoryToJson(savePath.readString())
                checkSaveJsonVer(string)
                val storySaveGson = Gson().fromJson(string, StorySaveGson::class.java)
                if (storySaveGson.story_id == storyId)
                    return Gson().fromJson(string, T::class.java)
            }
        } catch (e: Exception) {
            logErr("Failed to read save file for story id: $storyId")
            e.printStackTrace()

            return null
        }

        return null
    }

    /** Parses StorySaveGson to json and saves it into existing file
     *  or creates a new file with a name of StorySaveGson::save_name */
    fun save(storySaveGson: StorySaveGson) {
        val folder = savesFolder.list()
        for (save in folder) {
            try {
                if (Gson().fromJson(decryptStoryToJson(save.readString()), StorySaveGson::class.java).save_name == storySaveGson.save_name) {
                    save.writeString(encryptData(storySaveGson.toGson(), generateSecretKey(asdfaksf)), false, Charsets.UTF_8.name())
                    return
                }
            } catch (e: Exception) {
                logErr("Failed to save story $storySaveGson to file ${save.path()}")
                e.printStackTrace()

                break
            }
        }

        save(storySaveGson, storySaveGson.save_name)
    }
    
    private val asdfaksf = "story"
    
    fun decryptStoryToJson(encryptedString: String) : String {
        return try {
            decryptData(encryptedString, generateSecretKey(asdfaksf))
        } catch (e: Exception) {
            logErr("Failed to decrypt a story save")
            e.printStackTrace()
            "{ }"
        }
    }

    /** Parses StorySaveGson to json and saves it in the file with name of fileName */
    fun save(storySaveGson: StorySaveGson, saveName: String) {
        if (storySaveGson.save_name == "") throw IllegalArgumentException("StorySaveGson::name can't be empty.")
        if (saveName == "") throw IllegalArgumentException("fileName can't be empty.")

        val fileName = "$saveName$SAVE_EXT"
        val jsonSave = Gson().toJson(storySaveGson, storySaveGson::class.java)
        
        val storyEncrypted = encryptData(jsonSave, generateSecretKey(asdfaksf))

        savesFolder.child(fileName).writeString(storyEncrypted, false, Charsets.UTF_8.name())
    }
}
