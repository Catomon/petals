package ctmn.petals.web

import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationLogger
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.LifecycleListener
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.backends.headless.HeadlessFiles
import com.badlogic.gdx.utils.Clipboard

/**
 * Static initializers of the game classes read preferences and files. The builder loads those
 * classes to register them for reflection, so give it just enough of an application to survive.
 */
object BuildStubs {

    fun install() {
        Gdx.files = HeadlessFiles()
        Gdx.app = StubApplication()
    }

    private class StubApplication : Application {
        private val preferences = HashMap<String, Preferences>()

        override fun getType() = Application.ApplicationType.Desktop
        override fun getVersion() = 0
        override fun getJavaHeap() = 0L
        override fun getNativeHeap() = 0L
        override fun getApplicationListener(): com.badlogic.gdx.ApplicationListener? = null
        override fun getPreferences(name: String): Preferences =
            preferences.getOrPut(name) { StubPreferences() }

        override fun getFiles() = Gdx.files
        override fun getNet(): com.badlogic.gdx.Net? = null
        override fun getAudio(): com.badlogic.gdx.Audio? = null
        override fun getInput(): com.badlogic.gdx.Input? = null
        override fun getGraphics(): com.badlogic.gdx.Graphics? = null
        override fun getClipboard(): Clipboard? = null

        override fun log(tag: String?, message: String?) {}
        override fun log(tag: String?, message: String?, exception: Throwable?) {}
        override fun error(tag: String?, message: String?) {}
        override fun error(tag: String?, message: String?, exception: Throwable?) {}
        override fun debug(tag: String?, message: String?) {}
        override fun debug(tag: String?, message: String?, exception: Throwable?) {}
        override fun setLogLevel(logLevel: Int) {}
        override fun getLogLevel() = Application.LOG_NONE
        override fun setApplicationLogger(applicationLogger: ApplicationLogger?) {}
        override fun getApplicationLogger(): ApplicationLogger? = null

        override fun postRunnable(runnable: Runnable?) {
            runnable?.run()
        }

        override fun exit() {}
        override fun addLifecycleListener(listener: LifecycleListener?) {}
        override fun removeLifecycleListener(listener: LifecycleListener?) {}
    }

    private class StubPreferences : Preferences {
        private val values = HashMap<String, Any>()

        override fun putBoolean(key: String, value: Boolean): Preferences = put(key, value)
        override fun putLong(key: String, value: Long): Preferences = put(key, value)
        override fun putInteger(key: String, value: Int): Preferences = put(key, value)
        override fun putFloat(key: String, value: Float): Preferences = put(key, value)
        override fun putString(key: String, value: String): Preferences = put(key, value)
        override fun put(values: MutableMap<String, *>?): Preferences {
            values?.forEach { (key, value) -> this.values[key] = value ?: return@forEach }
            return this
        }

        private fun put(key: String, value: Any): Preferences {
            values[key] = value
            return this
        }

        override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue
        override fun getInteger(key: String, defValue: Int) = values[key] as? Int ?: defValue
        override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue
        override fun getString(key: String, defValue: String) = values[key] as? String ?: defValue
        override fun getString(key: String) = values[key] as? String ?: ""
        override fun getBoolean(key: String) = values[key] as? Boolean ?: false
        override fun getLong(key: String) = values[key] as? Long ?: 0L
        override fun getInteger(key: String) = values[key] as? Int ?: 0
        override fun getFloat(key: String) = values[key] as? Float ?: 0f
        override fun get(): MutableMap<String, *> = values
        override fun contains(key: String) = values.containsKey(key)
        override fun clear() = values.clear()
        override fun remove(key: String) {
            values.remove(key)
        }

        override fun flush() {}
    }
}
