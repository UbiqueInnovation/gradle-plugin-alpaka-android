package ch.ubique.gradle.alpaka.extensions.android

import com.android.build.api.variant.SourceDirectories
import com.android.build.api.variant.Sources

internal fun Sources.requireRes(): SourceDirectories.Layered {
	return requireNotNull(res) { "Sources.res directory is not available" }
}
