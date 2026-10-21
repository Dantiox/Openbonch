package com.autocheck.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.util.concurrent.ConcurrentHashMap

/**
 * Следит за физическими сетями устройства (Wi‑Fi, мобильная, Ethernet) и выбирает нужную
 * под [TrafficRoute]. Сокет, привязанный к такой сети, не использует VPN.
 */
class NetworkRouter(context: Context) {
    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val networks = ConcurrentHashMap<Network, NetworkCapabilities>()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            networks[network] = capabilities
        }

        override fun onLost(network: Network) {
            networks.remove(network)
        }
    }

    @Suppress("DEPRECATION")
    fun start() {
        // Сразу заполняем список, не дожидаясь первых колбэков
        cm.allNetworks.forEach { network ->
            cm.getNetworkCapabilities(network)?.let { networks[network] = it }
        }
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            callback,
        )
    }

    fun stop() {
        runCatching { cm.unregisterNetworkCallback(callback) }
        networks.clear()
    }

    /** @return сеть для привязки сокетов или null (для SYSTEM и когда подходящей сети нет). */
    fun resolve(route: TrafficRoute): Network? {
        val physical = networks.filter { (_, caps) ->
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        fun best(vararg transports: Int): Network? = physical.entries
            .filter { (_, caps) -> transports.any { caps.hasTransport(it) } }
            .sortedByDescending { it.value.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }
            .firstOrNull()?.key

        return when (route) {
            TrafficRoute.SYSTEM -> null
            TrafficRoute.WIFI -> best(NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_ETHERNET)
            TrafficRoute.CELLULAR -> best(NetworkCapabilities.TRANSPORT_CELLULAR)
            TrafficRoute.DIRECT ->
                best(NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_ETHERNET)
                    ?: best(NetworkCapabilities.TRANSPORT_CELLULAR)
                    ?: physical.keys.firstOrNull()
        }
    }

    fun label(network: Network): String {
        val caps = networks[network] ?: return "неизвестная сеть"
        val name = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "мобильная сеть"
            else -> "другая сеть"
        }
        return "$name, без VPN"
    }
}
