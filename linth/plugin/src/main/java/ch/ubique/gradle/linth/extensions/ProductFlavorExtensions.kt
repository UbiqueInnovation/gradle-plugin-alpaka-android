package ch.ubique.gradle.linth.extensions

import com.android.build.gradle.internal.api.ReadOnlyProductFlavor
import com.android.builder.model.ProductFlavor
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.extensibility.DefaultExtraPropertiesExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties

@Suppress("UNCHECKED_CAST")
fun <T> ProductFlavor.getProperty(key: String): T {
	val flavor = this as ReadOnlyProductFlavor
	val extras = flavor.getProperty("ext") as DefaultExtraPropertiesExtension
	return extras.properties[key] as T
}

fun <T> ProductFlavor.setProperty(key: String, value: T) {
	val extensionAware = this as ExtensionAware
	extensionAware.extraProperties.properties[key] = value
}