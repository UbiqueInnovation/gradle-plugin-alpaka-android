package ch.ubique.gradle.alpaka.extensions.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.variant.Variant

internal fun Variant.getProductFlavors(androidExtension: ApplicationExtension): List<ApplicationProductFlavor> {
	val flavorNames = productFlavorNames
	return androidExtension.productFlavors.filter { it.name in flavorNames }
}

@Suppress("UnusedVariable")
internal val Variant.productFlavorNames: List<String>
	get() = productFlavors.map { (dimension, flavor) -> flavor }.distinct()

internal fun Variant.requireFlavorName(): String {
	return requireNotNull(flavorName) { "Missing flavor name for variant $name"}
}

internal fun Variant.requireBuildType(): String {
	return requireNotNull(buildType) { "Missing build type for variant $name"}
}
