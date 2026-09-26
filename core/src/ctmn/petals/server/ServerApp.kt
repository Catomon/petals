package ctmn.petals.server

import com.badlogic.gdx.ApplicationAdapter

/**
 * ApplicationListener used by the headless dedicated server. It runs [DedicatedServer] with
 * no window and no GL context.
 */
class ServerApp(val config: ServerConfig) : ApplicationAdapter() {

    private val server = DedicatedServer(config)

    override fun create() {
        server.start()
    }

    override fun render() {
        server.update(com.badlogic.gdx.Gdx.graphics.deltaTime)
    }

    override fun dispose() {
        server.stop()
    }
}
