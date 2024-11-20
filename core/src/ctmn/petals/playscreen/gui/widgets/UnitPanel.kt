package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.Const
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.tiled
import ctmn.petals.widgets.newLabel

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

    private val unitIconDrawer = object : Actor() {
        private val sprite = Sprite()

        init {
            setSize(32f, 32f)
        }

        override fun draw(batch: Batch?, parentAlpha: Float) {
            if (unitActor?.isViewInitialized == true) {
                sprite.setSize(width * 2, height * 2)
                sprite.setPosition(x, y)
                sprite.setRegion(unitActor!!.sprite!!)
                sprite.draw(batch)
            }
        }
    }

    init {
        if (drawBackground)
            setBackground("unit_panel_background")

        name = "unit_panel"

        setupHorizontally()

        table.add(this)
        table.setFillParent(true)
        table.left().bottom().pad(0f).padLeft(3f)

        forUnit(null)
    }

    private fun setupHorizontally() {
        add(unitIconDrawer).size(32f, 32f).left().padBottom(-12f)
        row()
        add(unitName).top().left().padRight(3f).padLeft(3f).padBottom(3f)
        row()
        add(VisTable().apply {
            add(hpIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(hp).left().bottom().width(45f)

            add(mgIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(mg).left().bottom().width(45f)

            add(levelIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(level).left().bottom()
        }).padBottom(3f).left()
        row()
        add(VisTable().apply {
            add(atkIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(atk).left().bottom().width(45f)

            add(defIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(def).left().bottom().width(45f)

            add(expIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(exp).left().bottom()
        }).left().padBottom(6f)
        pack()
    }

    private fun setupVertically() {
        add(unitIconDrawer).size(32f, 32f).left().padBottom(-12f)
        row()
        add(unitName).top().left().padRight(3f).padLeft(3f).padBottom(3f)
        row()
        add(VisTable().apply {
            add(hpIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(hp).left().bottom().width(45f)
            row()
            add(atkIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(atk).left().bottom().width(45f)
            row()
            add(defIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(def).left().bottom().width(45f)
            row()
            add(mgIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(mg).left().bottom().width(45f)
            row()
            add(levelIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(level).left().bottom()
            row()
            add(expIcon).left().size(15f).padBottom(3f).padRight(6f)
            add(exp).left().bottom()
        }).padBottom(3f).left()
        pack()
    }

    fun onScreenResize(width: Int, height: Int) {
        clear()

        if (width > height) {
            setupHorizontally()
        } else {
            setupVertically()
            //table.center().bottom().pad(0f).padBottom(26f * 3f)
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        val focusedUnit = guiStage.playStage.getUnit(
            guiStage.tileSelectionDrawer.hoveringSprite.centerX().tiled(),
            guiStage.tileSelectionDrawer.hoveringSprite.centerY().tiled()
        )

        if (!guiStage.buyMenu.menuOpened) {
            if (focusedUnit != null) {
                if (focusedUnit != unitActor)
                    forUnit(focusedUnit)
            } else {
                if (guiStage.selectedUnit != unitActor)
                    forUnit(guiStage.selectedUnit)
            }
        }
    }

    fun forUnit(unitActor: UnitActor?) {
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
        } else exp.setText("N")

        pack()
    }
}
