package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.strings
import ctmn.petals.unit.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.tiled
import ctmn.petals.widgets.newLabel
import ctmn.petals.unit.xpToLevelUp
import com.badlogic.gdx.Gdx
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.unit.UnitActor

class UnitPanel(val guiStage: PlayGUIStage) : VisTable() {

    var drawBackground = false

    val table = VisTable()

    var unitActor: UnitActor? = null

    private var unitName = newLabel("UnitName", "default")

    private val hpIcon = VisImage(VisUI.getSkin().getDrawable("hp_ic"))
    private val hp = newLabel("HP: X", "default")

    private val mgIcon = VisImage(VisUI.getSkin().getDrawable("mg_ic"))
    private val mg = newLabel("MG: X", "default")

    private val atkIcon = VisImage(VisUI.getSkin().getDrawable("sword_icon"))
    private val atk = newLabel("ATK: X", "default")

    private val defIcon = VisImage(VisUI.getSkin().getDrawable("shield_icon"))
    private val def = newLabel("DEF: X", "default")

    private val levelIcon = VisImage(VisUI.getSkin().getDrawable("lvl_ic"))
    private val level = newLabel("LVL: X", "default")

    private val expIcon = VisImage(VisUI.getSkin().getDrawable("exp_ic"))
    private val exp = newLabel("XP: X/X", "default")

    init {
        if (drawBackground)
            setBackground("unit_panel_background")

        name = "unit_panel"

//        add(unitName).top().left().padBottom(6f * 3f).padTop(1f * 3f).padRight(1f * 3f).padLeft(1f * 3f)
//        row()
        add(VisTable().apply {
            add(hpIcon).left().size(5f * 3f).padBottom(1f * 3f).padRight(2f * 3f)
            add(hp).left().bottom().width(15f * 3f)

            add(mgIcon).left().size(5f * 3f).padBottom(1f * 3f).padRight(2f * 3f)
            add(mg).left().bottom().width(15f * 3f)

            add(levelIcon).left().size(5f * 3f).padBottom(1f * 3f).padRight(2f * 3f)
            add(level).left().bottom()
        }).padBottom(1f * 3f).left()
        row()
        add(VisTable().apply {
            add(atkIcon).left().size(5f * 3f).padBottom(1f * 3f).padRight(2f * 3f)
            add(atk).left().bottom().width(15f * 3f)

            add(defIcon).left().size(5f * 3f).padBottom(1f * 3f).padRight(2f * 3f)
            add(def).left().bottom().width(15f * 3f)

            add(expIcon).left().size(5f * 3f).padBottom(1f * 3f).padRight(2f * 3f)
            add(exp).left().bottom()
        }).left().padBottom(2f * 3f)

        pack()

        table.add(this)
        table.setFillParent(true)
        table.left().bottom()
    }

    override fun act(delta: Float) {
        super.act(delta)

        val focusedUnit = guiStage.playStage.getUnit(
            guiStage.tileSelectionDrawer.hoveringSprite.centerX().tiled(),
            guiStage.tileSelectionDrawer.hoveringSprite.centerY().tiled())

        if (focusedUnit != null)
            forUnit(focusedUnit)
        else
            forUnit(guiStage.selectedUnit)

        if (Gdx.graphics.width > Gdx.graphics.height) {
            table.left().bottom().pad(0f).padLeft(1f * 3f)
        } else {
            table.center().bottom().pad(0f).padBottom(26f * 3f)
        }
    }

    private fun forUnit(unitActor: UnitActor?) {
        this.unitActor = unitActor

        if (unitActor == null) {
            isVisible = false

            return
        } else
            isVisible = true

        val textUnitName = unitActor.characterName
        unitName.setText(textUnitName)
        //HP: MP: AT: DF:
        hp.setText("" + unitActor.health)
        mg.setText("" + (unitActor.cAbilities?.mana ?: 0))
        atk.setText("" + unitActor.maxDamage)
        def.setText("" + unitActor.defense)
        level.setText("" + (unitActor.cLevel?.lvl ?: "N"))

        if (unitActor.cLevel != null) {
            val cLevel = unitActor.cLevel!!
            val lvl = cLevel.lvl
            if (lvl < Const.MAX_LVL)
                exp.setText("" + (cLevel.exp) + "/" + (xpToLevelUp((cLevel.lvl))))
            else exp.setText("" + (cLevel.exp) + "/" + (xpToLevelUp((cLevel.lvl - 1))))
        }
        else exp.setText("N")

        pack()
    }
}
