package ch.ubique.gradle.alpaka.sources

import ch.ubique.gradle.alpaka.model.AndroidSigningConfigData
import ch.ubique.gradle.alpaka.utils.SigningConfigUtils
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

internal abstract class SignatureValueSource : ValueSource<String, SignatureValueSource.Parameters> {

	private val logger = Logging.getLogger(SignatureValueSource::class.java)

	interface Parameters : ValueSourceParameters {
		var signingConfig : Provider<AndroidSigningConfigData>
	}

	override fun obtain(): String {
		return parameters.signingConfig.orNull?.let {
			SigningConfigUtils(logger).getSignature(it) ?: "invalid"
		} ?: "unsigned"
	}

}