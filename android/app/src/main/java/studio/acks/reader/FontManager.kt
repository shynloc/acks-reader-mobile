package studio.acks.reader

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Determines the best font loading source for the current network environment.
 *
 * Priority:
 *   1. Offline            → "local"   (bundled WOFF2 in assets/fonts/)
 *   2. Google Fonts reachable → "google"
 *   3. Google blocked (CN) → "cn_mirror" (fonts.loli.net)
 *   4. Both fail           → "local"
 *
 * Result is cached for the app session. Call [invalidate] on network change.
 */
object FontManager {

    const val GOOGLE    = "google"
    const val CN_MIRROR = "cn_mirror"
    const val LOCAL     = "local"

    private const val GOOGLE_HOST    = "fonts.googleapis.com"
    private const val CN_MIRROR_HOST = "fonts.loli.net"
    private const val CONNECT_PORT   = 443
    private const val TIMEOUT_MS     = 2500

    @Volatile private var cached: String? = null
    @Volatile private var override: String = "auto"  // "auto"|"local"|"cn_mirror"

    /**
     * Returns the font source string to pass into ACKS.render() opts.
     * Respects manual override ("local" or "cn_mirror") before auto-detecting.
     */
    suspend fun resolve(ctx: Context): String {
        if (override != "auto") return override
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            val source = when {
                !hasInternet(ctx)            -> LOCAL
                isReachable(GOOGLE_HOST)     -> GOOGLE
                isReachable(CN_MIRROR_HOST)  -> CN_MIRROR
                else                         -> LOCAL
            }
            cached = source
            source
        }
    }

    /** Set a manual override. Pass "auto" to re-enable auto-detection. */
    fun setOverride(value: String) {
        override = value
        cached = null
    }

    /** Returns what source is currently active (cached result or override). */
    fun currentSource(): String = if (override != "auto") override else cached ?: "auto"

    /** Call when ConnectivityManager detects a network change. */
    fun invalidate() { cached = null }

    // ── internals ────────────────────────────────────────────────────────────

    private fun hasInternet(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun isReachable(host: String): Boolean = try {
        Socket().use { sock ->
            sock.connect(InetSocketAddress(host, CONNECT_PORT), TIMEOUT_MS)
            true
        }
    } catch (_: Exception) { false }
}
