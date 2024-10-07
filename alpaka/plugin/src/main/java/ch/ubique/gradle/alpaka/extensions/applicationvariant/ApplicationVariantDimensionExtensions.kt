package ch.ubique.gradle.alpaka.extensions.applicationvariant

import com.android.build.api.dsl.ApplicationVariantDimension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal fun <T> ApplicationVariantDimension.setProperty(key: String, value: T) {
	val extensionAware = this as ExtensionAware
	extensionAware.extraProperties[key] = value
}

@Suppress("UNCHECKED_CAST")
internal fun <T> ApplicationVariantDimension.getProperty(key: String): T {
	val extensionAware = this as ExtensionAware
	return extensionAware.extraProperties[key] as T
}
