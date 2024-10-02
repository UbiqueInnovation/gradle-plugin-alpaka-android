@file:Suppress("DEPRECATION")

package ch.ubique.gradle.alpaka

import ch.ubique.gradle.alpaka.config.AlpakaPluginConfig
import ch.ubique.gradle.alpaka.extensions.applicationvariant.launcherIconLabel
import ch.ubique.gradle.alpaka.extensions.capitalize
import ch.ubique.gradle.alpaka.extensions.getMergedManifestFile
import ch.ubique.gradle.alpaka.extensions.productflavor.launcherIconLabel as flavorLauncherIconLabel
import ch.ubique.gradle.alpaka.extensions.productflavor.alpakaUploadKey
import ch.ubique.gradle.alpaka.model.UploadRequest
import ch.ubique.gradle.alpaka.task.IconTask
import ch.ubique.gradle.alpaka.task.InjectMetadataIntoManifestTask
import ch.ubique.gradle.alpaka.task.UploadToAlpakaBackendTask
import ch.ubique.gradle.alpaka.utils.GitUtils
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.File

abstract class AlpakaPlugin : Plugin<Project> {

	@Suppress("DefaultLocale")
	override fun apply(project: Project) {
		val pluginExtension = project.extensions.create("alpaka", AlpakaPluginConfig::class.java, project)
		val androidExtension = getAndroidExtension(project)

		// The build ID is a unique UUID for each build
		val buildId = project.findProperty("build_id")?.toString() ?: project.findProperty("ubappid")?.toString() ?: "localbuild"

		// The build number is the run number of a build pipeline (e.g. GitHub workflow run number)
		val buildNumber = project.findProperty("build_number")?.toString()?.toLongOrNull() ?: 0L

		// The build batch is a unique UUID across all builds of a certain batch (e.g. all flavors of a commit)
		val buildBatch = project.findProperty("build_batch")?.toString() ?: "0"

		// The build timestamp is the timestamp when the build was started
		val buildTimestamp = project.findProperty("build_timestamp")?.toString()?.toLongOrNull() ?: System.currentTimeMillis()

		// The build branch is the Git name of the branch
		val buildBranch = project.findProperty("branch")?.toString() ?: GitUtils.obtainBranch(project)

		// Enable BuildConfig
		androidExtension.buildFeatures.buildConfig = true

		// Set BuildConfig fields
		androidExtension.defaultConfig.apply {
			buildConfigField("String", "BUILD_BATCH", "\"$buildBatch\"")
			buildConfigField("String", "BUILD_ID", "\"$buildId\"")
			buildConfigField("long", "BUILD_NUMBER", "${buildNumber}L")
			buildConfigField("long", "BUILD_TIMESTAMP", "${buildTimestamp}L")
			buildConfigField("String", "BRANCH", "\"$buildBranch\"")
		}

		//specify extra properties per flavor and defaultConfig for groovy dsl
		(androidExtension.defaultConfig as ExtensionAware).apply {
			extraProperties.set("launcherIconLabel", null)
			extraProperties.set("alpakaUploadKey", null)
		}
		androidExtension.productFlavors.configureEach { flavor ->
			flavor.extraProperties.set("launcherIconLabel", null)
			flavor.extraProperties.set("alpakaUploadKey", null)
		}

		// Hook injectMetadataTask into android build process
		project.afterEvaluate {
			androidExtension.applicationVariants.configureEach { variant ->
				val variantName = variant.name
				val flavor = variant.flavorName
				val buildType = variant.buildType.name

				val injectManifestTask = project.tasks.register(
					"injectMetadataIntoManifest${variantName.capitalize()}",
					InjectMetadataIntoManifestTask::class.java
				) { manifestTask ->
					manifestTask.outputs.file(project.getMergedManifestFile(variantName))
					manifestTask.variantName = variantName
					manifestTask.flavor = flavor
					manifestTask.buildType = buildType
					manifestTask.buildId = buildId
					manifestTask.buildNumber = buildNumber
					manifestTask.buildBatch = buildBatch
					manifestTask.buildTimestamp = buildTimestamp
					manifestTask.buildBranch = buildBranch
				}

				variant.outputs.forEach { output ->
					output.processManifestProvider.get().finalizedBy(injectManifestTask)
				}
				project.tasks.named("process${variantName.capitalize()}ManifestForPackage") {
					it.dependsOn(injectManifestTask)
				}
				project.tasks.named("processApplicationManifest${variantName.capitalize()}ForBundle") {
					it.dependsOn(injectManifestTask)
				}
			}
		}

		androidExtension.productFlavors.configureEach { flavor ->
			// Add the property 'launcherIconLabel' to each flavor and set the default value to its name
			val flavorName = flavor.name
			flavor.launcherIconLabel = if (flavorName == "prod") null else flavorName
		}

		// Hook iconTask into android build process
		project.afterEvaluate {
			val labelAppIcons = pluginExtension.labelAppIcons.getOrElse(true)

			androidExtension.applicationVariants.configureEach { variant ->
				val variantName = variant.name
				val flavor = variant.flavorName
				val buildType = variant.buildType.name
				val labelValue = getLauncherIconLabel(variant, androidExtension)

				if (labelAppIcons) {
					// make sure generated sources are used by build process
					// Add generated icon path to res-SourceSet. This must be here otherwise it is too late!
					val sourceSet = androidExtension.sourceSets.maybeCreate(variantName)
					sourceSet.res.srcDir(getGeneratedIconDir(project.layout.buildDirectory, flavor, buildType))
				}

				val iconTask = project.tasks.register(
					"generateAppIcon${variantName.capitalize()}",
					IconTask::class.java
				) { iconTask ->
					iconTask.variantName = variantName
					iconTask.flavor = flavor
					iconTask.buildType = buildType
					iconTask.labelValue = if (labelAppIcons) labelValue else null
					iconTask.targetWebIconFile = getGeneratedWebIconFile(project.layout.buildDirectory, flavor, buildType)
					iconTask.generatedIconDir = getGeneratedIconDir(project.layout.buildDirectory, flavor, buildType)
					iconTask.outputs.upToDateWhen { false } // always run the task
				}

				project.tasks.named("map${variantName.capitalize()}SourceSetPaths") { it.dependsOn(iconTask) }
				project.tasks.named("generate${variantName.capitalize()}Resources") { it.dependsOn(iconTask) }
				variant.outputs.forEach { output ->
					iconTask.dependsOn(output.processManifestProvider)
				}
			}
		}

		// Hook uploadTask into android build process
		project.afterEvaluate {
			androidExtension.applicationVariants.configureEach { variant ->
				val variantName = variant.name
				val flavor = variant.flavorName
				val buildType = variant.buildType.name
				val uploadKey = getUploadKey(variant, androidExtension)
				if (buildType != "release") return@configureEach

				val packageName = variant.applicationId
				val minSdk = requireNotNull(androidExtension.defaultConfig.minSdk)
				val targetSdk = requireNotNull(androidExtension.defaultConfig.targetSdk)
				val versionName = requireNotNull(androidExtension.defaultConfig.versionName)

				project.tasks.register(
					"uploadToAlpaka${variantName.capitalize()}",
					UploadToAlpakaBackendTask::class.java
				) { uploadTask ->
					val uploadRequest = variant.outputs.first().let {
						UploadRequest(
							apk = it.outputFile,
							appIcon = getGeneratedWebIconFile(project.layout.buildDirectory, flavor, buildType),
							appName = "", // Will be set from manifest inside the task
							packageName = packageName,
							flavor = flavor,
							branch = buildBranch,
							minSdk = minSdk,
							targetSdk = targetSdk,
							usesFeature = emptyList(), // Will be set from manifest inside the task
							buildId = buildId,
							buildNumber = buildNumber,
							buildTime = buildTimestamp,
							buildBatch = buildBatch,
							changelog = "", // Will be set inside the task
							signature = "", // Will be set inside the task
							version = versionName,
						)
					}

					uploadTask.variant = variant
					uploadTask.flavor = flavor
					uploadTask.buildType = buildType
					uploadTask.uploadKey = uploadKey ?: throw GradleException("No alpakaUploadKey specified")
					uploadTask.proxy = pluginExtension.proxy.orNull
					uploadTask.commitCount = pluginExtension.changelogCommitCount.orNull
					uploadTask.uploadRequest = uploadRequest

					uploadTask.dependsOn("assemble${variantName.capitalize()}")
				}
			}
		}
	}

	private fun getAndroidExtension(project: Project): AppExtension {
		val ext = project.extensions.findByType(AppExtension::class.java)
			?: throw GradleException("Android gradle plugin extension has not been applied before")
		return ext
	}

	private fun getGeneratedWebIconFile(buildDir: DirectoryProperty, flavor: String, buildType: String): Provider<File> {
		return getGeneratedIconDir(buildDir, flavor, buildType).map { it.file("web-icon.png").asFile }
	}

	private fun getGeneratedIconDir(buildDir: DirectoryProperty, flavor: String, buildType: String): Provider<Directory> {
		return buildDir.dir("generated/res/launcher-icon/$flavor/$buildType/res")
	}

	private fun getUploadKey(applicationVariant: ApplicationVariant, androidExtension: AppExtension): String? {
		val productFlavor = applicationVariant.productFlavors.firstOrNull()
		return productFlavor?.alpakaUploadKey ?: androidExtension.defaultConfig.alpakaUploadKey
	}

	private fun getLauncherIconLabel(applicationVariant: ApplicationVariant, androidExtension: AppExtension): String? {
		val productFlavor = applicationVariant.productFlavors.firstOrNull()
		return productFlavor?.flavorLauncherIconLabel ?: androidExtension.defaultConfig.launcherIconLabel
	}

}
