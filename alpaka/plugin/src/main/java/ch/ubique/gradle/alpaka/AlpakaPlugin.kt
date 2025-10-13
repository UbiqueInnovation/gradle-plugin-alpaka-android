@file:Suppress("DEPRECATION")

package ch.ubique.gradle.alpaka

import ch.ubique.gradle.alpaka.config.AlpakaPluginConfig
import ch.ubique.gradle.alpaka.extensions.capitalize
import ch.ubique.gradle.alpaka.extensions.getMergedManifestFile
import ch.ubique.gradle.alpaka.extensions.listFilesOrEmpty
import ch.ubique.gradle.alpaka.extensions.productflavor.alpakaUploadKey
import ch.ubique.gradle.alpaka.task.*
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
import org.gradle.kotlin.dsl.launcherIconLabel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.File
import ch.ubique.gradle.alpaka.extensions.productflavor.launcherIconLabel as flavorLauncherIconLabel

abstract class AlpakaPlugin : Plugin<Project> {

	companion object {
		private val MIN_GRADLE_VERSION = GradleVersion.version("8.11")
	}

	override fun apply(project: Project) {
		GradleVersion.current().let { current ->
			if (current < MIN_GRADLE_VERSION) {
				throw GradleException("Alpaka requires at least Gradle ${MIN_GRADLE_VERSION.version}. Currently ${current.version}.")
			}
		}

		val pluginExtension = project.extensions.create("alpaka", AlpakaPluginConfig::class.java, project)
		val androidExtension = getAndroidExtension(project)

		// The build ID is a unique ID for each build
		val buildId = project.findProperty("build_id")?.toString() ?: project.findProperty("ubappid")?.toString() ?: "localbuild"

		// The build number is the run number of a build pipeline (e.g. GitHub workflow run number)
		val buildNumber = project.findProperty("build_number")?.toString()?.toLongOrNull() ?: 0L

		// The build batch is a unique ID across all builds of a certain batch (e.g. all flavors of a commit)
		val buildBatch = project.findProperty("build_batch")?.toString() ?: "0"

		// The build timestamp is the timestamp when the build was started
		val buildTimestamp = project.findProperty("build_timestamp")?.toString()?.toLongOrNull() ?: System.currentTimeMillis()

		// The build branch is the Git name of the branch
		val vcsBranch = project.findProperty("branch")?.toString() ?: GitUtils.obtainBranch(project)

		// The build commit hash is the Git hash of the commit
		val vcsCommitHash = project.findProperty("commitHash")?.toString()

		// Enable BuildConfig
		androidExtension.buildFeatures.buildConfig = true

		// Set BuildConfig fields
		androidExtension.defaultConfig.apply {
			buildConfigField("String", "BUILD_BATCH", "\"$buildBatch\"")
			buildConfigField("String", "BUILD_ID", "\"$buildId\"")
			buildConfigField("long", "BUILD_NUMBER", "${buildNumber}L")
			buildConfigField("long", "BUILD_TIMESTAMP", "${buildTimestamp}L")
			buildConfigField("String", "BRANCH", "\"$vcsBranch\"")
		}

		// Specify extra properties per flavor and defaultConfig for groovy dsl
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
				val variantNameCapitalized = variantName.capitalize()
				val flavor = variant.flavorName
				val buildType = variant.buildType.name

				val injectManifestTask = project.tasks.register(
					"injectMetadataIntoManifest$variantNameCapitalized",
					InjectMetadataIntoManifestTask::class.java
				) { manifestTask ->
					val mergedManifestFile = project.getMergedManifestFile(variantName)
					manifestTask.mergedManifestFile = mergedManifestFile
					manifestTask.variantName = variantName
					manifestTask.flavor = flavor
					manifestTask.buildType = buildType
					manifestTask.buildId = buildId
					manifestTask.buildNumber = buildNumber
					manifestTask.buildBatch = buildBatch
					manifestTask.buildTimestamp = buildTimestamp
					manifestTask.buildBranch = vcsBranch
					manifestTask.outputs.file(mergedManifestFile)
				}

				variant.outputs.forEach { output ->
					output.processManifestProvider.configure { it.finalizedBy(injectManifestTask) }
				}
				project.tasks.named("process${variantNameCapitalized}ManifestForPackage") {
					it.dependsOn(injectManifestTask)
				}
				project.tasks.named("processApplicationManifest${variantNameCapitalized}ForBundle") {
					it.dependsOn(injectManifestTask)
				}
			}
		}

		androidExtension.productFlavors.configureEach { flavor ->
			// Add the property 'launcherIconLabel' to each flavor and set the default value to its name
			val flavorName = flavor.name
			flavor.launcherIconLabel = if (flavorName == "prod") null else flavorName
		}

		project.afterEvaluate {
			// Hook labelAppIcon task into android build process
			val doLabelAppIcons = pluginExtension.labelAppIcons.getOrElse(true)
			androidExtension.applicationVariants.configureEach { variant ->
				val variantName = variant.name
				val variantNameCapitalized = variantName.capitalize()
				val flavorName = variant.flavorName
				val productFlavors = variant.productFlavors.map { it.name }
				val buildType = variant.buildType.name
				val labelValue = getLauncherIconLabel(variant, androidExtension)

				if (doLabelAppIcons) {
					// make sure generated sources are used by build process
					// Add generated icon path to res-SourceSet. This must be here otherwise it is too late!
					val sourceSet = androidExtension.sourceSets.maybeCreate(variantName)
					sourceSet.res.srcDir(getGeneratedIconDir(project.layout.buildDirectory, flavorName, buildType))
				}

				val launcherIconLabelTask = project.tasks.register(
					"labelAppIcon$variantNameCapitalized",
					LauncherIconLabelTask::class.java
				) { iconTask ->
					iconTask.variantName = variantName
					iconTask.flavorName = flavorName
					iconTask.productFlavors = productFlavors
					iconTask.buildType = buildType
					iconTask.labelValue = if (doLabelAppIcons) labelValue else null
					iconTask.sourceWebIconFile = project.provider { findWebIcon(project.projectDir, flavorName) }
					iconTask.mergedManifestFile = project.getMergedManifestFile(variantName)
					iconTask.generatedWebIcon = getGeneratedWebIconFile(project.layout.buildDirectory, flavorName, buildType)
					iconTask.generatedIconDir = getGeneratedIconDir(project.layout.buildDirectory, flavorName, buildType)
					iconTask.outputs.upToDateWhen { false } // always run the task

					iconTask.mustRunAfter(project.tasks.named("injectMetadataIntoManifest$variantNameCapitalized"))
				}

				project.tasks.named("map${variantNameCapitalized}SourceSetPaths") { it.dependsOn(launcherIconLabelTask) }
				project.tasks.named("generate${variantNameCapitalized}Resources") { it.dependsOn(launcherIconLabelTask) }
				project.tasks.named("process${variantNameCapitalized}NavigationResources") { it.dependsOn(launcherIconLabelTask) }
				project.tasks.matching { it.name == "extract${variantNameCapitalized}SupportedLocales" }
					.configureEach { it.dependsOn(launcherIconLabelTask) }
				variant.outputs.forEach { output ->
					launcherIconLabelTask.dependsOn(output.processManifestProvider)
				}
			}

			// Hook alpaka task into android build process
			androidExtension.applicationVariants.configureEach { variant ->
				val buildType = variant.buildType.name
				if (buildType != "release") return@configureEach

				val variantName = variant.name
				val variantNameCapitalized = variantName.capitalize()
				val flavor = variant.flavorName
				val uploadKey = getUploadKey(variant, androidExtension)

				val assembleTaskName = "assemble$variantNameCapitalized"

				// compileAlpakaMetadata{$variant} task to compile alpaka mata data
				val metadataTask = project.tasks.register(
					"compileAlpakaMetadata$variantNameCapitalized",
					CompileAlpakaMetadataTask::class.java
				) { metadataTask ->
					metadataTask.androidConfig = androidExtension.defaultConfig
					metadataTask.variant = variant
					metadataTask.mergedManifestFile = project.getMergedManifestFile(variantName)
					metadataTask.vcsCommitCount = pluginExtension.changelogCommitCount.orNull
					metadataTask.vcsBranch = vcsBranch
					metadataTask.vcsCommitHash = vcsCommitHash
					metadataTask.buildId = buildId
					metadataTask.buildNumber = buildNumber
					metadataTask.buildTime = buildTimestamp
					metadataTask.buildBatch = buildBatch
					metadataTask.metadataFile = getGeneratedAppMetadataFile(project.layout.buildDirectory, flavor, buildType)
				}
				project.tasks.named(assembleTaskName) { it.finalizedBy(metadataTask) }

				// publishToAlpaka{$variant} task to only publish to alpaka
				val publishToAlpakaTaskName = "publishToAlpaka$variantNameCapitalized"
				project.tasks.register(
					publishToAlpakaTaskName,
					PublishToAlpakaTask::class.java
				) { uploadTask ->
					uploadTask.uploadKey = uploadKey ?: throw GradleException("No alpakaUploadKey specified")
					uploadTask.apk = project.provider { variant.outputs.first().outputFile }
					uploadTask.webIcon = getGeneratedWebIconFile(project.layout.buildDirectory, flavor, buildType)
					uploadTask.appMetadataJsonFile = getGeneratedAppMetadataFile(project.layout.buildDirectory, flavor, buildType)
					uploadTask.proxy = pluginExtension.proxy.orNull
					// ensure that the compilation tasks are run before, IF they're run
					uploadTask.mustRunAfter(assembleTaskName, metadataTask)
				}

				// assembleAndPublishToAlpaka{$variant} task to assemble and publish to alpaka
				val assembleAndPublishToAlpakaTaskName = "assembleAndPublishToAlpaka$variantNameCapitalized"
				project.tasks.register(
					assembleAndPublishToAlpakaTaskName,
					AssembleAndPublishToAlpakaTask::class.java
				) { assembleAndPublishTask ->
					assembleAndPublishTask.dependsOn(assembleTaskName, publishToAlpakaTaskName)
				}

				// uploadToAlpaka{$variant} deprecated task to assemble and publish to alpaka, kept for backwards compatibility reasons
				val legacyUploadTaskName = "uploadToAlpaka$variantNameCapitalized"
				project.tasks.register(
					legacyUploadTaskName,
					UploadToAlpakaBackendTask::class.java
				) { uploadTask ->
					uploadTask.description = "deprecated, use $assembleAndPublishToAlpakaTaskName instead"
					uploadTask.dependsOn(assembleAndPublishToAlpakaTaskName)
					uploadTask.doFirst {
						uploadTask.logger.warn("Task '${uploadTask.name}' is deprecated. Use '$assembleAndPublishToAlpakaTaskName' instead.")
					}
				}
			}
		}
	}

	private fun getAndroidExtension(project: Project): AppExtension {
		val ext = project.extensions.findByType(AppExtension::class.java)
			?: throw GradleException("Android gradle plugin extension has not been applied before")
		return ext
	}

	private fun findWebIcon(moduleDir: File, flavor: String): File {
		val dirs = sequenceOf(File(moduleDir, "src/$flavor"), File(moduleDir, "src/main"), moduleDir)
		return dirs
			.flatMap { it.listFilesOrEmpty() }
			.find { it.name.matches(Regex(".*(web|playstore|512)\\.(png|webp)")) }
			?: throw GradleException("Must provide web icon matching (web|playstore|512).(png|webp) in one of the following locations:\n  ${dirs.joinToString()}")
	}

	private fun getGeneratedWebIconFile(buildDir: DirectoryProperty, flavor: String, buildType: String): Provider<File> {
		return buildDir.file("outputs/launcher-icon/$flavor/$buildType/web-icon.png").map { it.asFile }
	}

	private fun getGeneratedIconDir(buildDir: DirectoryProperty, flavor: String, buildType: String): Provider<Directory> {
		return buildDir.dir("generated/res/launcher-icon/$flavor/$buildType/res")
	}

	private fun getGeneratedAppMetadataFile(buildDir: DirectoryProperty, flavor: String, buildType: String): Provider<File> {
		return buildDir.file("outputs/alpaka/$flavor/$buildType/metadata.json").map { it.asFile }
	}

	private fun getUploadKey(applicationVariant: ApplicationVariant, androidExtension: AppExtension): String? {
		val productFlavor = applicationVariant.productFlavors.firstOrNull()
		return productFlavor?.alpakaUploadKey ?: androidExtension.defaultConfig.alpakaUploadKey
	}

	private fun getLauncherIconLabel(applicationVariant: ApplicationVariant, androidExtension: AppExtension): String? {
		return applicationVariant.productFlavors.firstNotNullOfOrNull { it.flavorLauncherIconLabel }
			?: androidExtension.defaultConfig.launcherIconLabel
	}

}
