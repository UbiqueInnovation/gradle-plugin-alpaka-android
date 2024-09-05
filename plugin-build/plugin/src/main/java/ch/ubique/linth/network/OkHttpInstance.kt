package ch.ubique.linth.network

import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


object OkHttpInstance {

	private var preconfiguredClient: OkHttpClient? = null
	private var proxy: Proxy? = null

	//internal var userAgentAndLanguageInterceptor: UserAgentAndAcceptLanguageInterceptor? = null

	fun getPreconfiguredClient(): OkHttpClient {
		return preconfiguredClient ?: synchronized(this) {
			preconfiguredClient ?: init().also { preconfiguredClient = it }
		}
	}

	fun reset() {
		preconfiguredClient = null
	}

	fun setProxy(hostname: String?, port: Int = 0) {
		proxy = hostname?.let {
			Proxy(Proxy.Type.HTTP, InetSocketAddress(it, port))
		}
		reset()
	}

	private fun init() = OkHttpClient().newBuilder().apply {
		val timeoutSeconds = 60L
		readTimeout(timeoutSeconds, TimeUnit.SECONDS)
		writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
		connectTimeout(timeoutSeconds, TimeUnit.SECONDS)

		if (proxy != null) {
			val trustManager = createCouldntCareLessTrustManager()
			SSLContext.getInstance("TLS").apply {
				init(null, arrayOf(trustManager), null)
			}.socketFactory.let {
				sslSocketFactory(it, trustManager)
			}
		}
		proxy(proxy)

		val userAgent = "LinthPlugin TODO Version"
		/*
		userAgentAndLanguageInterceptor = UserAgentAndAcceptLanguageInterceptor(userAgent, languageTag).also {
			addInterceptor(it)
		}
		*/

		/*
		val appToken = AppPreferences.getInstance(context).appToken
		addInterceptor(UserAppTokenInterceptor(appToken))
		*/
	}.build()

	@Throws(CertificateException::class, KeyStoreException::class, NoSuchAlgorithmException::class, IllegalStateException::class)
	private fun createCouldntCareLessTrustManager(): X509TrustManager {
		return object : X509TrustManager {

			override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
				// ignore
			}

			override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
				//ignore
			}

			override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
		}
	}
}