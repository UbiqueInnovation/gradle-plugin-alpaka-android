package ch.ubique.gradle.linth.extensions.applicationvariant

import com.android.build.api.dsl.ApplicationVariantDimension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.extraProperties

private fun <T> ApplicationVariantDimension.setProperty(key: String, value: T) {
	val extensionAware = this as ExtensionAware
	extensionAware.extraProperties[key] = value
}

var ApplicationVariantDimension.launcherIconLabel: String?
	get() = error("only setter")
	set(value) = setProperty("launcherIconLabel", value)

var ApplicationVariantDimension.linthUploadKey: String?
	get() = error("only setter")
	set(value) = setProperty("linthUploadKey", value)