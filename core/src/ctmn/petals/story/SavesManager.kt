package ctmn.petals.story

import ctmn.petals.utils.toGson
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import com.google.gson.Gson

object SavesManager {

    val savesPath = Gdx.files.local("saves").path()
    const val saveExt = ".save"

    //todo gson(gson(saves).get(save))

    fun first() : StorySaveGson? {
        return getByFileName(Gdx.files.local(savesPath).list().firstOrNull()?.nameWithoutExtension() ?: return null)
    }

    fun isSingleSave() : Boolean {
        val folder = Gdx.files.local(savesPath)
        return folder.list().size == 1
    }

    fun anyExists() : Boolean {
        val folder = Gdx.files.local(savesPath)
        return folder.list().isNotEmpty()
    }

    fun getAll() : Array<StorySaveGson> {
        val saves = Array<StorySaveGson>()
        val folder = Gdx.files.local(savesPath).list()
        for (save in folder)
            saves.add(Gson().fromJson(save.readString(), StorySaveGson::class.java))

        return saves
    }

    inline fun <reified T: StorySaveGson> getByFileName(fileName: String) : T? {
        if (fileName == "") throw IllegalArgumentException("fileName can't be empty.")

        val filePath = "$savesPath/$fileName$saveExt"

        return Gson().fromJson(Gdx.files.local(filePath).readString(), T::class.java)
    }

    inline fun <reified T: StorySaveGson> getByName(saveName: String) : T? {
        val folder = Gdx.files.local(savesPath)

        for (savePath in folder.list()) {
            val storySaveGson = Gson().fromJson(savePath.readString(), T::class.java)
            if (storySaveGson.save_name == saveName)
                return storySaveGson
        }

        return null
    }

    inline fun <reified T: StorySaveGson> getByStoryId(storyId: Int) : T? {
        for (storySaveGson in getAll()) {
            if (storySaveGson.story_id == storyId)
                return storySaveGson as T
        }

        return null
    }

    /** Parses StorySaveGson to json and saves it into existing file
     *  or creates a new file with a name of StorySaveGson::save_name */
    fun save(storySaveGson: StorySaveGson) {
        val folder = Gdx.files.local(savesPath).list()
        for (save in folder) {
            if (Gson().fromJson(save.readString(), StorySaveGson::class.java).save_name == storySaveGson.save_name) {
                save.writeString(storySaveGson.toGson(), false, Charsets.UTF_8.name())
                return
            }
        }

        save(storySaveGson, storySaveGson.save_name)
    }

    /** Parses StorySaveGson to json and saves it in the file with name of fileName */
    fun save(storySaveGson: StorySaveGson, fileName: String) {
        if (storySaveGson.save_name == "") throw IllegalArgumentException("StorySaveGson::name can't be empty.")
        if (fileName == "") throw IllegalArgumentException("fileName can't be empty.")

        val filePath = "$savesPath/$fileName$saveExt"
        val jsonSave = Gson().toJson(storySaveGson, storySaveGson::class.java)

        Gdx.files.local(filePath).writeString(jsonSave, false, Charsets.UTF_8.name())
    }
}
