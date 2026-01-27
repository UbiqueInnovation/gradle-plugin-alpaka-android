package ch.ubique.gradle.alpaka

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.GradleException
import org.gradle.util.GradleVersion

internal object Requirements {

	private val MIN_GRADLE_VERSION = GradleVersion.version("9.1")
	private val MIN_AGP_VERSION = AndroidPluginVersion(9, 0)

	fun validateGradleVersion() {
		GradleVersion.current().let { current ->
			if (current < MIN_GRADLE_VERSION) {
				throw GradleException("Alpaka requires at least Gradle ${MIN_GRADLE_VERSION.version}. Currently ${current.version}.")
			}
		}
	}

	fun validateAgpVersion(androidComponentExtension: AndroidComponentsExtension<*, *, *>) {
		androidComponentExtension.pluginVersion.let { current ->
			if (current < MIN_AGP_VERSION) {
				throw GradleException("Alpaka requires at least AGP ${MIN_AGP_VERSION.version}. Currently ${current.version}.")
			}
		}
	}

}
