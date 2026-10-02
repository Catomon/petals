package ctmn.petals.utils

import com.badlogic.gdx.Gdx
import ctmn.petals.Config
import java.net.SocketException
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

private var jmdns: JmDNS? = null

private fun createJmDNS() {
    jmdns?.close()
    jmdns = null

    jmdns = JmDNS.create()
}

fun closeJmDNS() {
    Thread {
        jmdns?.close()
    }.start()
}

fun startJmDNSAsServer(resultListener: (Boolean) -> Unit) {
    Thread {
        try {
            createJmDNS()
        } catch (e: SocketException) {
            e.printStackTrace()

            resultListener(false)

            return@Thread
        }

        // A LAN room listens on the LAN port, so that is the port to advertise. Advertising the
        // dedicated server's port sent joiners to whichever server happened to hold it.
        val serviceInfo = ServiceInfo.create(
            Config.SERVICE_TYPE,
            Config.SERVICE_NAME,
            Config.LAN_PORT,
            Config.SERVICE_DESCRIPTION
        )
        jmdns!!.registerService(serviceInfo)

        resultListener(true)
    }.start()
}

/**
 * Looks for a room hosted on the local network. [resultListener] gets the address of the first
 * room found, or null when discovery could not start at all.
 *
 * Rooms listen on [Config.LAN_PORT], so the address is all the browser needs.
 */
fun startJmDNSAsClient(resultListener: (String?) -> Unit) {
    Thread {
        try {
        createJmDNS()
        } catch (e: SocketException) {
            e.printStackTrace()

            resultListener(null)

            return@Thread
        }

        val jmdns = ctmn.petals.utils.jmdns!!

        jmdns.addServiceListener(Config.SERVICE_TYPE, object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent?) {
                jmdns.requestServiceInfo(event?.type, event?.name)
            }

            override fun serviceRemoved(event: ServiceEvent?) {}

            override fun serviceResolved(event: ServiceEvent) {
                if (event.name != Config.SERVICE_NAME) return

                val host = event.info.inetAddresses.firstOrNull()?.hostAddress ?: return

                Gdx.app.log("JmDNS", "Local server found: $host")

                resultListener(host)
            }
        })
    }.start()
}