package ctmn.petals.utils

import com.badlogic.gdx.Gdx
import ctmn.petals.Const
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

        val serviceInfo = ServiceInfo.create(
            Const.SERVICE_TYPE,
            Const.SERVICE_NAME,
            Const.SERVER_PORT,
            Const.SERVICE_DESCRIPTION
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

        jmdns.addServiceListener(Const.SERVICE_TYPE, object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent?) {
                jmdns.requestServiceInfo(event?.type, event?.name)
            }

            override fun serviceRemoved(event: ServiceEvent?) {}

            override fun serviceResolved(event: ServiceEvent) {
                if (event.name == Const.SERVICE_NAME) {
                    GameClient.ConnectionData.host =
                        event.info.inetAddresses.firstOrNull()?.hostAddress ?: return

                    Gdx.app.log("JmDNS", "Local server found: ${GameClient.ConnectionData.host}")

                    resultListener(true)
                }
            }
        })
    }.start()
}