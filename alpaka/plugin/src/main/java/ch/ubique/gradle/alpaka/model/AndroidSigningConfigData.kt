package ch.ubique.gradle.alpaka.model

import java.io.File
import java.io.Serializable

internal data class AndroidSigningConfigData(
	val storeType: String?,
	val storeFile: File?,
	val storePassword: String?,
	val keyAlias: String?,
	val keyPassword: String?,
) : Serializable
