package ctmn.petals.utils

import com.badlogic.gdx.Gdx
import ctmn.petals.Config
import ctmn.petals.multiplayer.client.GameClient
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

fun startJmDNSAsClient(resultListener: (Boolean) -> Unit) {
    Thread {
        try {
        createJmDNS()
        } catch (e: SocketException) {
            e.printStackTrace()

            resultListener(false)

            return@Thread
        }

        val jmdns = ctmn.petals.utils.jmdns!!

        jmdns.addServiceListener(Config.SERVICE_TYPE, object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent?) {
                jmdns.requestServiceInfo(event?.type, event?.name)
            }

            override fun serviceRemoved(event: ServiceEvent?) {}

            override fun serviceResolved(event: ServiceEvent) {
                if (event.name == Config.SERVICE_NAME) {
                    GameClient.ConnectionData.host =
                        event.info.inetAddresses.firstOrNull()?.hostAddress ?: return
                    // Follow the advertised port rather than assuming the default one, so a room
                    // that had to move off the LAN port is still reachable.
                    GameClient.ConnectionData.port = event.info.port

                    Gdx.app.log("JmDNS", "Local server found: ${GameClient.ConnectionData.host}")

                    resultListener(true)
                }
            }
        })
    }.start()
}