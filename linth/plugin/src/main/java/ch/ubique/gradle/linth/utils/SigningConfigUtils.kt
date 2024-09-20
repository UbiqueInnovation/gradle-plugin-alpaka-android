package ch.ubique.gradle.linth.utils

import com.android.builder.model.SigningConfig
import org.gradle.api.logging.Logger
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.Certificate

/**
 * Taken from
 * https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/internal/tasks/SigningReportTask.java;l=145
 * and
 * https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:sdk-common/src/main/java/com/android/ide/common/signing/KeystoreHelper.java;l=217
 */
class SigningConfigUtils(private val logger: Logger) {

	fun getSignature(signingConfig: SigningConfig): String? {
		return try {
			val certificate = getCertificate(signingConfig)
			getFingerprint(certificate, "MD5")
		} catch (e: Exception) {
			logger.info("Failed to get MD5 signature for certificate", e)
			null
		}
	}

	private fun getCertificate(signingConfig: SigningConfig): Certificate {
		val keystore = KeyStore.getInstance(signingConfig.storeType ?: KeyStore.getDefaultType())
		signingConfig.storeFile?.inputStream()?.use {
			keystore.load(it, signingConfig.storePassword?.toCharArray())
		}

		val keyPayyword = signingConfig.keyPassword?.toCharArray()
		val entry = keystore.getEntry(signingConfig.keyAlias, KeyStore.PasswordProtection(keyPayyword)) as KeyStore.PrivateKeyEntry
		return entry.certificate
	}

	private fun getFingerprint(certificate: Certificate, hashAlgorithm: String): String? {
		val digest = MessageDigest.getInstance(hashAlgorithm)
		return digest.digest(certificate.encoded).toHexadecimalString()
	}

	private fun ByteArray.toHexadecimalString(): String {
		val sb = StringBuilder()
		val len = size
		for (i in 0 until len) {
			val num = (this[i].toInt()) and 0xff
			if (num < 0x10) {
				sb.append('0')
			}
			sb.append(Integer.toHexString(num))
			if (i < len - 1) {
				sb.append(':')
			}
		}
		return sb.toString().uppercase()
	}

}