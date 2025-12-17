package ch.ubique.gradle.alpaka.sources

import ch.ubique.gradle.alpaka.utils.SigningConfigUtils
import com.android.builder.model.SigningConfig
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

abstract class SignatureValueSource : ValueSource<String, SignatureValueSource.Parameters> {

	private val logger = Logging.getLogger(SignatureValueSource::class.java)

	interface Parameters : ValueSourceParameters {
		var signingConfig : Provider<SigningConfig?>
	}

	override fun obtain(): String {
		return parameters.signingConfig.orNull?.let {
			SigningConfigUtils(logger).getSignature(it) ?: "invalid"
		} ?: "unsigned"
	}

}