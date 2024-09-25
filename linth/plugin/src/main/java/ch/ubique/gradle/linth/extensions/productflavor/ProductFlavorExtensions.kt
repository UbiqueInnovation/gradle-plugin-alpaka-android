package ch.ubique.gradle.linth.extensions.productflavor

import com.android.build.gradle.internal.api.ReadOnlyProductFlavor
import com.android.builder.model.ProductFlavor
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.extensibility.DefaultExtraPropertiesExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties

@Suppress("UNCHECKED_CAST")
private fun <T> ProductFlavor.getProperty(key: String): T {
	return when {
		this is ExtensionAware -> extraProperties.properties[key] as T
		else -> {
			val flavor = this as ReadOnlyProductFlavor
			val extras = flavor.getProperty("ext") as DefaultExtraPropertiesExtension
			extras.properties[key] as T
		}
	}
}

private fun <T> ProductFlavor.setProperty(key: String, value: T) {
	val extensionAware = this as ExtensionAware
	extensionAware.extraProperties.properties[key] = value
}

internal var ProductFlavor.launcherIconLabel: String?
	get() = getProperty("launcherIconLabel")
	set(value) = setProperty("launcherIconLabel", value)

internal val ProductFlavor.linthUploadKey: String?
	get() = getProperty("linthUploadKey")