package ctmn.petals.utils

import ctmn.petals.Strings
import com.badlogic.gdx.files.FileHandle

// create strings.json file in the assets folder
fun main() {
    val fileHande = FileHandle("android/assets/lang/strings.json")
    fileHande.writeString(Strings().toPrettyGson(), false, Charsets.UTF_8.name())
}