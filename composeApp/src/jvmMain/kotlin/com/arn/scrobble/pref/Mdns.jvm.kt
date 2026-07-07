package com.arn.scrobble.pref

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener


actual class Mdns actual constructor(
    private val namePrefix: String,
) : ServiceListener {
    private val serviceType = "_http._tcp.local."
    private val _status = MutableStateFlow<MdnsStatus>(MdnsStatus.None)
    actual val status = _status.asStateFlow()

    private var jmdns: JmDNS? = null

    actual fun register(ip: String, port: Int, suffix: String) {
        try {
            val jmdns = JmDNS.create(InetAddress.getByName(ip)).also { this.jmdns = it }

            val serviceInfo =
                ServiceInfo.create(serviceType, namePrefix + suffix, port, "")
            jmdns.registerService(serviceInfo)
            _status.value = MdnsStatus.Registered(serviceInfo.name)
        } catch (e: IOException) {
            _status.value = MdnsStatus.RegistrationError
        }
    }

    actual fun discover() {
        val jmdns = JmDNS.create(InetAddress.getLocalHost(), null, 100)
            .also { this.jmdns = it }

        jmdns.addServiceListener(serviceType, this)
        _status.value = MdnsStatus.Discovering
    }


    override fun serviceAdded(p0: ServiceEvent?) {
    }

    override fun serviceRemoved(p0: ServiceEvent?) {
    }

    override fun serviceResolved(serviceEvent: ServiceEvent?) {
        if (serviceEvent?.name?.startsWith(namePrefix) == true) {
            serviceEvent.let {
                val name = it.name
                val info = it.info
                val ip = info.inetAddresses.firstOrNull()?.hostAddress ?: return
                val port = info.port
                val service = MdnsStatus.ServiceInfo(name, ip, port)
                if (_status.value is MdnsStatus.Discovered) {
                    val currentServices = (_status.value as MdnsStatus.Discovered).services
                    // replace the service with the same name if it exists
                    val newList = (listOf(service) + currentServices).distinctBy { it.name }
                    _status.value = MdnsStatus.Discovered(newList)
                } else {
                    _status.value = MdnsStatus.Discovered(listOf(service))
                }
            }
        }
    }

    actual fun stop() {
        val jmdns = jmdns ?: return
        if (status.value != MdnsStatus.None) {
            if (status.value is MdnsStatus.Registration) {
                jmdns.unregisterAllServices()
            } else if (status.value is MdnsStatus.Discovery) {
                jmdns.removeServiceListener(serviceType, this)
            }
            jmdns.close()
            this.jmdns = null
            _status.value = MdnsStatus.None
        }
    }
}