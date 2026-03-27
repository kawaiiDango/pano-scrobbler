package com.arn.scrobble.pref

import android.net.nsd.DiscoveryRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.PatternMatcher
import android.os.ext.SdkExtensions
import com.arn.scrobble.utils.AndroidStuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

actual class Mdns actual constructor(
    private val namePrefix: String,
) : NsdManager.RegistrationListener,
    NsdManager.DiscoveryListener {
    private val serviceType = "_http._tcp."
    private val _status = MutableStateFlow<MdnsStatus>(MdnsStatus.None)
    actual val status = _status.asStateFlow()

    private var nsdServiceInfoCallback: NsdManager.ServiceInfoCallback? = null

    private val nsdManager by lazy { AndroidStuff.applicationContext.getSystemService(NsdManager::class.java)!! }

    actual fun register(ip: String, port: Int, suffix: String) {
        val serviceInfo = NsdServiceInfo().also {
            it.serviceName = namePrefix + suffix
            it.serviceType = serviceType
            it.port = port
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, this)
    }

    actual fun discover() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 22
        ) {
            val discoveryRequest =
                DiscoveryRequest.Builder(serviceType)
                    .setFlags(DiscoveryRequest.FLAG_SHOW_PICKER)
                    .setServiceNameFilter(PatternMatcher(namePrefix, PatternMatcher.PATTERN_PREFIX))
                    .build()

            nsdServiceInfoCallback = object : NsdManager.ServiceInfoCallback {
                override fun onServiceInfoCallbackRegistrationFailed(p0: Int) {
                    _status.value = MdnsStatus.DiscoveryError
                }

                override fun onServiceInfoCallbackUnregistered() {
                    // set this from stop()
                    // otherwise status changes from DiscoveredAndConfirmed to None too fast for compose to pick up
//                    _status.value = MdnsStatus.None
                }

                override fun onServiceLost() {
                }

                override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                    val name = serviceInfo.serviceName
                    val ip = serviceInfo.host.hostAddress ?: return
                    val port = serviceInfo.port

                    val service = MdnsStatus.ServiceInfo(name, ip, port)
                    _status.value = MdnsStatus.DiscoveredAndConfirmed(service)
                }
            }.also {
                nsdManager.registerServiceInfoCallback(
                    discoveryRequest,
                    Executors.newCachedThreadPool(),
                    it
                )
            }
        } else {
            nsdManager.discoverServices(
                serviceType,
                NsdManager.PROTOCOL_DNS_SD,
                this
            )
        }
    }

    override fun onRegistrationFailed(p0: NsdServiceInfo?, p1: Int) {
        _status.value = MdnsStatus.RegistrationError
    }

    override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
        _status.value = MdnsStatus.Registered(serviceInfo?.serviceName.orEmpty())
    }

    override fun onServiceUnregistered(p0: NsdServiceInfo?) {
        _status.value = MdnsStatus.None
    }

    override fun onUnregistrationFailed(p0: NsdServiceInfo?, p1: Int) {
        _status.value = MdnsStatus.UnregistrationError
    }

    override fun onDiscoveryStarted(p0: String?) {
        _status.value = MdnsStatus.Discovering
    }

    override fun onDiscoveryStopped(p0: String?) {
        _status.value = MdnsStatus.None
    }

    override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo?.serviceName?.startsWith(namePrefix) == true) {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {

                override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {
                    _status.value = MdnsStatus.DiscoveryError
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                    serviceInfo?.let {
                        val name = it.serviceName
                        val ip = it.host.hostAddress ?: return
                        val port = it.port
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
            })
        }
    }

    override fun onServiceLost(p0: NsdServiceInfo?) {
    }

    override fun onStartDiscoveryFailed(p0: String?, p1: Int) {
        _status.value = MdnsStatus.DiscoveryError
    }

    override fun onStopDiscoveryFailed(p0: String?, p1: Int) {
        _status.value = MdnsStatus.DiscoveryError
    }

    actual fun stop() {
        if (status.value != MdnsStatus.None) {
            if (status.value is MdnsStatus.Registration) {
                nsdManager.unregisterService(this)
            } else if (status.value is MdnsStatus.Discovery) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 22
                ) {
                    nsdServiceInfoCallback?.let { nsdManager.unregisterServiceInfoCallback(it) }
                    nsdServiceInfoCallback = null
                    _status.value = MdnsStatus.None
                } else {
                    nsdManager.stopServiceDiscovery(this)
                }
            }
        }
    }

}