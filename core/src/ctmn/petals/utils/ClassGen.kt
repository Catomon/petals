package ctmn.petals.utils

import java.io.BufferedWriter
import java.io.File
import java.util.*

fun main() {
    ClassGen.generateUnitsClass()
    ClassGen.generateTilesClass()
    ClassGen.generateAbilitiesClass()
}

object ClassGen {

    fun generateTilesClass() {
        val resourcesDir = File("android/assets/textures/tiles")
        val resourceMap = mutableMapOf<String, File>()
        collectPngFiles(resourcesDir, resourceMap)

        val resourcesClass = File("core/src/ctmn/petals/tile/Tiles.kt")
        resourcesClass.parentFile.mkdirs()
        resourcesClass.bufferedWriter().use { writer ->
            writer.appendLine("package ctmn.petals.tile")
            writer.appendLine()
            writer.appendWarningMessage()
            writer.appendLine()
            writer.appendLine("object Tiles {")
            resourceMap.forEach { (name, file) ->
                writer.appendLine("    const val $name = \"${file.nameWithoutExtension}\"")
            }
            writer.appendLine("}")
        }
    }

    private fun collectPngFiles(directory: File, resourceMap: MutableMap<String, File>) {
        val resourceFiles = directory.listFiles { file -> file.isFile && file.name.endsWith(".png") }

        resourceFiles?.forEach { file ->
            if (!file.nameWithoutExtension.matches(".*_[a-z]".toRegex())) {
                val fieldName = file.nameWithoutExtension.toUpperCase(Locale.ROOT).replace("\\W".toRegex(), "_")
                resourceMap[fieldName] = file
            }
        }

        val subdirectories = directory.listFiles { file -> file.isDirectory }
        subdirectories?.forEach { subdirectory ->
            collectPngFiles(subdirectory, resourceMap)
        }
    }

    fun generateUnitsClass() {
        val resourcesDir = File("core/src/ctmn/petals/unit/actors")
        val resourceFiles = resourcesDir.listFiles { file -> file.isFile && file.name.endsWith(".kt") }

        val resourceMap = mutableMapOf<String, File>()
        resourceFiles?.forEach { file ->
            val fieldName = file.nameWithoutExtension.toUpperCase(Locale.ROOT)
            resourceMap[fieldName] = file
        }

        val resourcesClass = File("core/src/ctmn/petals/unit/Units.kt")
        resourcesClass.parentFile.mkdirs()
        resourcesClass.bufferedWriter().use { writer ->
            writer.appendLine("package ctmn.petals.unit")
            writer.appendLine()
            writer.appendLine(
                "import ctmn.petals.player.Player\n" +
                        "import ctmn.petals.playscreen.selfName\n" +
                        "import ctmn.petals.unit.actors.*\n" +
                        "import java.util.*\n" +
                        "import kotlin.collections.HashMap\n" +
                        "import kotlin.reflect.KClass"
            )
            writer.appendLine()
            writer.appendWarningMessage()
            writer.appendLine()
            writer.appendLine("object Units {")
            writer.appendLine(
                "    private val map = HashMap<String, KClass<out UnitActor>>()\n" +
                        "     val names = ArrayList<String>()"
            )
            writer.appendLine()
            writer.appendLine("    init {")

            resourceMap.forEach { (name, file) ->
                writer.appendLine("        add(${file.nameWithoutExtension}())")
            }

            writer.appendLine("    }")
            writer.appendLine(
                "\n" +
                        "    fun add(unit: UnitActor) : UnitActor {\n" +
                        "        val unitName = unit.selfName\n" +
                        "        map[unitName] = unit::class\n" +
                        "        names.add(unitName)\n" +
                        "        return unit\n" +
                        "    }\n" +
                        "\n" +
                        "    fun find(name: String, player: Player? = null) : UnitActor? {\n" +
                        "        if (map[name] == null) return null\n" +
                        "\n" +
                        "        val unitActor = map[name]!!.java.constructors.first().newInstance() as UnitActor\n" +
                        "        unitActor.playerId = player?.id ?: -1\n" +
                        "        unitActor.teamId = player?.teamId ?: -1\n" +
                        "\n" +
                        "        return unitActor\n" +
                        "    }" +
                        "\n" +
                        "    fun get(name: String, player: Player? = null) : UnitActor {\n" +
                        "        if (map[name] == null)\n" +
                        "            throw IllegalArgumentException(\"Unit with name '\$name' is not found.\")\n" +
                        "\n" +
                        "        val unitActor = map[name]!!.java.constructors.first().newInstance() as UnitActor\n" +
                        "        unitActor.playerId = player?.id ?: -1\n" +
                        "        unitActor.teamId = player?.teamId ?: -1\n" +
                        "\n" +
                        "        return unitActor\n" +
                        "    }"
            )
            writer.appendLine("}")
        }
    }

    fun generateAbilitiesClass() {
        val resourcesDir = File("core/src/ctmn/petals/unit/abilities")
        val resourceFiles = resourcesDir.listFiles { file -> file.isFile && file.name.endsWith(".kt") }

        val resourceMap = mutableMapOf<String, File>()
        resourceFiles?.forEach { file ->
            val fieldName = file.nameWithoutExtension.toUpperCase(Locale.ROOT)
            resourceMap[fieldName] = file
        }

        val resourcesClass = File("core/src/ctmn/petals/unit/Abilities.kt")
        resourcesClass.parentFile.mkdirs()
        resourcesClass.bufferedWriter().use { writer ->
            writer.appendLine("package ctmn.petals.unit")
            writer.appendLine()
            writer.appendLine(
                "import com.badlogic.gdx.utils.ArrayMap\n" +
                        "import kotlin.reflect.KClass\n" +
                        "import ctmn.petals.unit.abilities.*"
            )
            writer.appendLine()
            writer.appendWarningMessage()
            writer.appendLine()
            writer.appendLine("object Abilities : ArrayMap<String, KClass<out Ability>>() {")
            writer.appendLine()
            writer.appendLine("    init {")

            resourceMap.forEach { (name, file) ->
                writer.appendLine("        add(${file.nameWithoutExtension}())")
            }

            writer.appendLine("    }")
            writer.appendLine(
                "\n" +
                        "    private fun add(ability: Ability) {\n" +
                        "        put(ability.name, ability::class)\n" +
                        "    }" +
                        "\n" +
                        "    fun getAbility(name: String): Ability {\n" +
                        "        return get(name).java.constructors.first().newInstance() as Ability\n" +
                        "    }"
            )
            writer.appendLine("}")
        }
    }

    private fun BufferedWriter.appendWarningMessage() {
        appendLine(
            "/**\n" +
                    " * This class is auto-generated.\n" +
                    " * [ctmn.petals.utils.ClassGen.generateUnitsClass]\n" +
                    " */"
        )
    }
}