package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.assets
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.abilities.FireboltAbility
import ctmn.petals.unit.abilities.HealingTouchAbility
import ctmn.petals.unit.abilities.PersonalBarrierAbility
import ctmn.petals.unit.cAbilities
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newIconButton
import ctmn.petals.widgets.newLabel

class LevelUpWindow(val unitActor: UnitActor) : VisWindow("${unitActor.selfName.first().toUpperCase()}${unitActor.selfName.substring(1)} Level Up") {

    private val unitIcon = VisImage(assets.textureAtlas.findRegions("units/${unitActor.selfName}").first()).apply { setSize(24f, 24f) }

    private val fontScale = 0.16f

    private val unitNameLabel = newLabel("${unitActor.selfName.first().toUpperCase()}${unitActor.selfName.substring(1)}", "font_5")

    private val captureListener =  object : EventListener {
        override fun handle(event: Event?): Boolean {
            if (isVisible && event is InputEvent) {

                if (event.target.firstAscendant(LevelUpWindow::class.java) != null)
                    let { }
                else
                    event.stop()

                return true
            }

            return false
        }
    }

    private var selectedAbility: Ability? = null

    init {
        add(VisTable().apply {
            add(unitIcon).size(128f)

            add(VisTable().apply {
                add(newLabel("AT: +1", "font_5"))
                add(newLabel("DF: +1", "font_5")).padLeft(10f)
                row().height(10f)
                add(newLabel("MP: +1", "font_5"))
                add(newLabel("AT: +1", "font_5")).padLeft(10f)
            })
        }).left()

        row()
        add(AbilitiesSelectTable(
            unitActor,
            object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                unitActor.cAbilities?.abilities?.add(actor.userObject as Ability)?.let {

                    (stage as PlayGUIStage).abilitiesPanel.updateAbilities()
                }

                this@LevelUpWindow.remove()
            }
        }).apply {
            background = null
            abilities.removeAll { it.name == "summon" }

            setForUnit(unitActor, false) // todo

            unlock(HealingTouchAbility().name)
            unlock(FireboltAbility().name)
            unlock(PersonalBarrierAbility().name)
        })

        closeOnEscape()
        row()
        add(newIconButton("cancel").addChangeListener { remove() }).padTop(8f)

        pack()
    }

    override fun setStage(stage: Stage?) {
        check(stage is PlayGUIStage?)

        if (stage == null && this.stage != null) {
            this.stage.removeCaptureListener(captureListener)
            (this.stage as PlayGUIStage).inputManager.playScreenInputProcessor.isStopped = false
        }

        super.setStage(stage)

        stage?.addCaptureListener(captureListener)
        stage?.inputManager?.playScreenInputProcessor?.isStopped = true

        if (stage != null && unitActor.cAbilities == null) {
            this@LevelUpWindow.remove()
        }
    }
}