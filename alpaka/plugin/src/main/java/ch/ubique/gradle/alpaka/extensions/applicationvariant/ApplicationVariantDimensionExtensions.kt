package ch.ubique.gradle.alpaka.extensions.applicationvariant

import com.android.build.api.dsl.ApplicationVariantDimension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.extraProperties

private fun <T> ApplicationVariantDimension.setProperty(key: String, value: T) {
	val extensionAware = this as ExtensionAware
	extensionAware.extraProperties[key] = value
}

private fun <T> ApplicationVariantDimension.getProperty(key: String): T {
	val extensionAware = this as ExtensionAware
	return extensionAware.extraProperties[key] as T
}

var ApplicationVariantDimension.launcherIconLabel: String?
	get() = getProperty("launcherIconLabel")
	set(value) = setProperty("launcherIconLabel", value)

var ApplicationVariantDimension.alpakaUploadKey: String?
	get() = getProperty("alpakaUploadKey")
	set(value) = setProperty("alpakaUploadKey", value)
