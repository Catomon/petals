package ctmn.petals.web

import com.github.xpenatan.gdx.backends.teavm.config.AssetFileHandle
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuildConfiguration
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuilder
import com.github.xpenatan.gdx.backends.teavm.config.plugins.TeaReflectionSupplier
import com.github.xpenatan.gdx.backends.teavm.gen.SkipClass
import org.teavm.vm.TeaVMOptimizationLevel
import java.io.File

/** Builds the TeaVM/HTML application. */
@SkipClass
object WebBuilder {

    /**
     * Types the skin JSON names by their full class name. Short names are resolved by libGDX
     * itself, but anything with a package goes through Class.forName, which TeaVM only serves
     * for registered classes.
     */
    private val skinReflection = listOf(
        "com.kotcrab.vis.ui.Sizes",
        "com.kotcrab.vis.ui.util.form.SimpleFormValidator\$FormValidatorStyle",
        "com.kotcrab.vis.ui.widget.BusyBar\$BusyBarStyle",
        "com.kotcrab.vis.ui.widget.color.ColorPickerStyle",
        "com.kotcrab.vis.ui.widget.color.ColorPickerWidgetStyle",
        "com.kotcrab.vis.ui.widget.file.FileChooserStyle",
        "com.kotcrab.vis.ui.widget.LinkLabel\$LinkLabelStyle",
        "com.kotcrab.vis.ui.widget.ListViewStyle",
        "com.kotcrab.vis.ui.widget.Menu\$MenuStyle",
        "com.kotcrab.vis.ui.widget.MenuBar\$MenuBarStyle",
        "com.kotcrab.vis.ui.widget.MenuItem\$MenuItemStyle",
        "com.kotcrab.vis.ui.widget.MultiSplitPane\$MultiSplitPaneStyle",
        "com.kotcrab.vis.ui.widget.PopupMenu\$PopupMenuStyle",
        "com.kotcrab.vis.ui.widget.Separator\$SeparatorStyle",
        "com.kotcrab.vis.ui.widget.spinner.Spinner\$SpinnerStyle",
        "com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane\$TabbedPaneStyle",
        "com.kotcrab.vis.ui.widget.Tooltip\$TooltipStyle",
        "com.kotcrab.vis.ui.widget.VisCheckBox\$VisCheckBoxStyle",
        "com.kotcrab.vis.ui.widget.VisImageButton\$VisImageButtonStyle",
        "com.kotcrab.vis.ui.widget.VisImageTextButton\$VisImageTextButtonStyle",
        "com.kotcrab.vis.ui.widget.VisSplitPane\$VisSplitPaneStyle",
        "com.kotcrab.vis.ui.widget.VisTextButton\$VisTextButtonStyle",
        "com.kotcrab.vis.ui.widget.VisTextField\$VisTextFieldStyle",
    )

    /** Types read and written through Gson. */
    private val gsonReflection = listOf(
        "ctmn.petals.Strings",
        "ctmn.petals.Strings\$General",
        "ctmn.petals.Strings\$Units",
        "ctmn.petals.UserSave",
        "ctmn.petals.playscreen.gui.widgets.Book\$BookSave",
        "ctmn.petals.story.StorySaveGson",
        "ctmn.petals.story.Progress",
        "ctmn.petals.story.LevelProgress",
        "ctmn.petals.story.alissa.AlissaStorySave",
        "ctmn.petals.story.faecampaign.FaerieStorySave",
        "ctmn.petals.editor.MapSave",
        "ctmn.petals.editor.LayerSave",
        "ctmn.petals.editor.TileSave",
    )

    @JvmStatic
    fun main(arguments: Array<String>) {
        BuildStubs.install()

        val configuration = TeaBuildConfiguration().apply {
            assetsPath.add(AssetFileHandle("build/webassets"))
            webappPath = File("build/dist").canonicalPath
            additionalClasspath.add(File("build/classes/java/classlibExt").canonicalFile.toURI().toURL())
        }

        for (name in skinReflection + gsonReflection) {
            TeaReflectionSupplier.addReflectionClass(name)
        }

        val tool = TeaBuilder.config(configuration)
        tool.mainClass = "ctmn.petals.web.WebLauncher"
        tool.optimizationLevel = TeaVMOptimizationLevel.SIMPLE
        tool.setObfuscated(false)
        TeaBuilder.build(tool)
    }
}

