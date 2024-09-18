package ctmn.petals.story.alissa

import com.badlogic.gdx.utils.Array
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.queueAddUnitAction
import ctmn.petals.playscreen.selfName
import ctmn.petals.story.*
import ctmn.petals.unit.*
import ctmn.petals.unit.Abilities
import ctmn.petals.unit.actors.fairies.FairyPike
import ctmn.petals.unit.UnitActor

abstract class AlissaScenario(name: String, levelFileName: String) : Scenario(name, levelFileName) {

    protected val alicePlayer = AlicePlayer
    protected val alice = CreateUnit.alice.player(alicePlayer)

    protected val unitsToSaveProgressOf = Array<UnitActor>()

    override fun loadFrom(storySaveGson: StorySaveGson) {
        check(storySaveGson is AlissaStorySave)

        alice.level(storySaveGson.alissa.level)
        alice.cLevel?.exp = storySaveGson.alissa.exp

        storySaveGson.alissa.faeries.forEach { alice.summoner.units.add(it) }

        storySaveGson.alissa.abilities.forEach { alice.abilities.add(Abilities.getAbility(it)) }

        for (character in storySaveGson.characters) {
            unitsToSaveProgressOf.firstOrNull { it.selfName == character.name }?.let {  unit ->
                unit.level(character.level)
                unit.cLevel?.exp = character.exp
                unit.cAbilities?.abilities?.let {  abilities ->
                    character.abilities.forEach { abilities.add(Abilities.getAbility(it)) }
                }
            } ?: continue
        }
    }

    override fun saveTo(storySaveGson: StorySaveGson) {
        check(storySaveGson is AlissaStorySave)

        alice.cLevel?.let {
            storySaveGson.alissa.level = it.lvl
            storySaveGson.alissa.exp = it.exp
        }

        alice.summoner.units.forEach {
            if (!storySaveGson.alissa.faeries.contains(it))
                storySaveGson.alissa.faeries.add(it)
        }

        alice.abilities.forEach {
            storySaveGson.alissa.abilities.add(it.name)
        }

        for (unit in unitsToSaveProgressOf) {
            val level = unit.cLevel?.lvl ?: 1
            val exp = unit.cLevel?.exp ?: 0

            storySaveGson.characters.firstOrNull { it.name == unit.selfName }?.let { karSave ->
                karSave.level = level
                karSave.exp = exp
                unit.cAbilities?.abilities?.forEach { karSave.abilities.add(it.name) }
            } ?: let {
                storySaveGson.characters.add(CharacterSave(unit.selfName, level, exp).apply {
                    unit.cAbilities?.abilities?.forEach { abilities.add(it.name) }
                })
            }
        }
    }

    override fun scenarioCreated(playScreen: PlayScreen) {
        "1234".forEach { _ ->
            val faerie = FairyPike().player(alicePlayer).followerOf(alice)
            faerie.setPositionOrNear(alice.tiledX, alice.tiledY)

            playScreen.queueAddUnitAction(faerie)
        }
    }
}