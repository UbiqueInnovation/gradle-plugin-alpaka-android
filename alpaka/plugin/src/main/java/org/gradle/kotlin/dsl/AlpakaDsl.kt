package org.gradle.kotlin.dsl

import ch.ubique.gradle.alpaka.extensions.applicationvariant.getProperty
import ch.ubique.gradle.alpaka.extensions.applicationvariant.setProperty
import com.android.build.api.dsl.ApplicationVariantDimension

var ApplicationVariantDimension.launcherIconLabel: String?
	get() = getProperty("launcherIconLabel")
	set(value) = setProperty("launcherIconLabel", value)

var ApplicationVariantDimension.alpakaUploadKey: String?
	get() = getProperty("alpakaUploadKey")
	set(value) = setProperty("alpakaUploadKey", value)
