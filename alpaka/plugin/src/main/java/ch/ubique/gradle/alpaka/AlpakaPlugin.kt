package ch.ubique.gradle.alpaka

import ch.ubique.gradle.alpaka.config.AlpakaPluginConfig
import ch.ubique.gradle.alpaka.config.AlpakaProperties
import ch.ubique.gradle.alpaka.extensions.android.getProductFlavors
import ch.ubique.gradle.alpaka.extensions.android.requireBuildType
import ch.ubique.gradle.alpaka.extensions.android.requireFlavorName
import ch.ubique.gradle.alpaka.extensions.android.requireRes
import ch.ubique.gradle.alpaka.extensions.capitalize
import ch.ubique.gradle.alpaka.extensions.gradle.flattened
import ch.ubique.gradle.alpaka.extensions.listFilesOrEmpty
import ch.ubique.gradle.alpaka.model.AndroidBuildConfigData
import ch.ubique.gradle.alpaka.model.AndroidSigningConfigData
import ch.ubique.gradle.alpaka.sources.BuildTimestampValueSource
import ch.ubique.gradle.alpaka.sources.GitBranchValueSource
import ch.ubique.gradle.alpaka.sources.GitCommitLogValueSource
import ch.ubique.gradle.alpaka.sources.SignatureValueSource
import ch.ubique.gradle.alpaka.task.*
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.alpakaUploadKey
import org.gradle.kotlin.dsl.launcherIconLabel
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.File

abstract class AlpakaPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		Requirements.validateGradleVersion()

		val androidExtension = project.getAndroidExtension()
		val androidComponentExtension = project.getAndroidComponentsExtension()
		Requirements.validateAgpVersion(androidComponentExtension)

		val pluginExtension = project.extensions.create("alpaka", AlpakaPluginConfig::class.java, project)

		// Check if local build optimization is enabled
		val optimizeForLocalBuild = project.findLocalProperty(AlpakaProperties.OPTIMIZE_FOR_LOCAL_BUILD)?.toBoolean() == true
		if (optimizeForLocalBuild) {
			project.logger.lifecycle("Alpaka: local build optimization enabled (${AlpakaProperties.OPTIMIZE_FOR_LOCAL_BUILD}=true).")
		}

		// The build ID is a unique ID for each build
		val buildId = project.findProperty("build_id")?.toString() ?: project.findProperty("ubappid")?.toString() ?: "localbuild"

		// The build number is the run number of a build pipeline (e.g. GitHub workflow run number)
		val buildNumber = project.findProperty("build_number")?.toString()?.toLongOrNull() ?: 0L

		// The build batch is a unique ID across all builds of a certain batch (e.g. all flavors of a commit)
		val buildBatch = project.findProperty("build_batch")?.toString() ?: "0"

		// The build timestamp is the timestamp when the build was started
		val buildTimestampFromProperty = project.findProperty("build_timestamp")?.toString()?.toLongOrNull()
		val buildTimestampProvider = if (buildTimestampFromProperty != null) {
			project.provider { buildTimestampFromProperty }
		} else if (optimizeForLocalBuild) {
			project.getLocalBuildTimestampProvider()
		} else {
			project.getBuildTimestampProvider()
		}

		// The build branch is the Git name of the branch
		val vcsBranchProvider = project.findProperty("branch")
			?.toString()
			?.let { project.provider { it } }
			?: project.getGitBranchProvider()

		// The build commit hash is the Git hash of the commit
		val vcsCommitHash = project.findProperty("commitHash")?.toString()

		// Enable BuildConfig
		androidExtension.buildFeatures.buildConfig = true

		// Set BuildConfig fields
		androidExtension.defaultConfig.apply {
			buildConfigField("String", "BUILD_BATCH", "\"$buildBatch\"")
			buildConfigField("String", "BUILD_ID", "\"$buildId\"")
			buildConfigField("long", "BUILD_NUMBER", "${buildNumber}L")
			buildConfigField("long", "BUILD_TIMESTAMP", "${buildTimestampProvider.get()}L")
			buildConfigField("String", "BRANCH", "\"${vcsBranchProvider.get()}\"")
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

		val buildLogicFiles = arrayOf(
			project.file("build.gradle"),
			project.file("build.gradle.kts"),
			project.rootProject.file("build.gradle"),
			project.rootProject.file("build.gradle.kts"),
			project.rootProject.file("settings.gradle"),
			project.rootProject.file("settings.gradle.kts"),
			project.rootProject.file("gradle/libs.versions.toml"),
		)

		// Hook compileAlpakaMetadataManifest into android build process
		androidComponentExtension.onVariants { variant ->
			val variantName = variant.name
			val variantNameCapitalized = variantName.capitalize()
			val flavor = variant.requireFlavorName()
			val buildType = variant.requireBuildType()

			val metadataManifestTask = project.tasks.register(
				"compileAlpakaMetadataManifest$variantNameCapitalized",
				MetadataManifestTask::class.java
			) { manifestTask ->
				manifestTask.variantName = variantName
				manifestTask.flavor = flavor
				manifestTask.buildType = buildType
				manifestTask.buildId = buildId
				manifestTask.buildNumber = buildNumber
				manifestTask.buildBatch = buildBatch
				manifestTask.buildTimestamp = buildTimestampProvider
				manifestTask.buildBranch = vcsBranchProvider
			}

			variant.sources.manifests.addGeneratedManifestFile(metadataManifestTask, MetadataManifestTask::manifestFile)
		}

		androidExtension.productFlavors.configureEach { flavor ->
			// Add the property 'launcherIconLabel' to each flavor and set the default value to its name
			val flavorName = flavor.name
			flavor.launcherIconLabel = if (flavorName == "prod") null else flavorName
		}

		// Hook labelAppIcon task into android build process
		androidComponentExtension.onVariants { variant ->
			val variantName = variant.name
			val variantNameCapitalized = variantName.capitalize()
			val labelValue = variant.getLauncherIconLabel(androidExtension)

			val doLabelAppIcons = pluginExtension.labelAppIcons.getOrElse(true)
			if (doLabelAppIcons) {
				val launcherIconLabelTask = project.tasks.register(
					"labelLauncherIcon$variantNameCapitalized",
					LauncherIconLabelTask::class.java
				) { iconTask ->
					iconTask.labelValue = labelValue
					iconTask.resDirs = variant.sources.requireRes().static.flattened()
					iconTask.manifestFiles = variant.sources.manifests.all
					iconTask.buildLogicFiles.from(*buildLogicFiles)
				}
				variant.sources.requireRes().addGeneratedSourceDirectory(launcherIconLabelTask, LauncherIconLabelTask::generatedIconDir)
			}
		}

		// Hook alpaka tasks into android build process
		androidComponentExtension.onVariants { variant ->
			val buildType = variant.buildType
			if (buildType != "release") return@onVariants

			val isDryRun = project.findProperty(AlpakaProperties.DRY_RUN)?.toString()?.toBoolean() ?: false

			val variantName = variant.name
			val variantNameCapitalized = variantName.capitalize()
			val flavorName = variant.requireFlavorName()
			val uploadKey = variant.getUploadKey(androidExtension)

			val assembleTaskName = "assemble$variantNameCapitalized"

			// compileAlpakaMetadata{$variant} task to compile alpaka mata data
			val metadataTask = project.tasks.register(
				"compileAlpakaMetadata$variantNameCapitalized",
				CompileAlpakaMetadataTask::class.java
			) { metadataTask ->
				metadataTask.onlyIf { !optimizeForLocalBuild }
				metadataTask.applicationId = variant.applicationId
				metadataTask.flavorName = flavorName
				metadataTask.androidConfig = AndroidBuildConfigData(
					minSdk = requireNotNull(androidExtension.defaultConfig.minSdk),
					targetSdk = requireNotNull(androidExtension.defaultConfig.targetSdk),
					versionName = requireNotNull(androidExtension.defaultConfig.versionName),
					versionCode = androidExtension.defaultConfig.versionCode?.toLong() ?: 0L,
				)
				metadataTask.signature = project.getSignatureProvider(variant, androidExtension)

				metadataTask.resDirs = variant.sources.requireRes().all.flattened()

				val mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
				metadataTask.mergedManifestFile.set(mergedManifest)
				val commitCount = pluginExtension.gitCommitCount.getOrElse(10)
				val allowGitFetch = pluginExtension.gitFetchAllowed.getOrElse(false)
				val vcsCommitHistoryProvider =
					if (optimizeForLocalBuild) project.provider { "" }
					else project.getGitCommitLogProvider(commitCount, allowGitFetch)
				metadataTask.vcsCommitHistory = vcsCommitHistoryProvider
				metadataTask.vcsBranch = vcsBranchProvider
				metadataTask.vcsCommitHash = vcsCommitHash
				metadataTask.buildId = buildId
				metadataTask.buildNumber = buildNumber
				metadataTask.buildTime = buildTimestampProvider
				metadataTask.buildBatch = buildBatch
				metadataTask.metadataFile = project.getGeneratedAppMetadataFile(flavorName, buildType)
			}
			project.afterEvaluate {
				project.tasks.named(assembleTaskName) { it.finalizedBy(metadataTask) }
			}

			val doLabelAppIcons = pluginExtension.labelAppIcons.getOrElse(true)
			val labelValue = variant.getLauncherIconLabel(androidExtension)

			val webIconLabelTask = project.tasks.register(
				"labelWebIcon$variantNameCapitalized",
				WebIconLabelTask::class.java
			) { iconTask ->
				iconTask.labelValue = if (doLabelAppIcons) labelValue else null
				iconTask.sourceWebIconFile = project.findWebIcon(flavorName)
				iconTask.generatedWebIcon = project.getGeneratedWebIconFile(flavorName, buildType)
			}

			// publishToAlpaka{$variant} task to only publish to alpaka
			val publishToAlpakaTaskName = "publishToAlpaka$variantNameCapitalized"
			project.tasks.register(
				publishToAlpakaTaskName,
				PublishToAlpakaTask::class.java
			) { uploadTask ->
				uploadTask.uploadKey = uploadKey ?: throw GradleException("No alpakaUploadKey specified")
				// not using variant.artifacts.get(SingleArtifact.APK) since that creates an implicit task dependency on build
				uploadTask.apkDir = project.layout.buildDirectory.dir("outputs/apk/$flavorName/$buildType")
				uploadTask.webIcon = project.getGeneratedWebIconFile(flavorName, buildType)
				uploadTask.appMetadataJsonFile = project.getGeneratedAppMetadataFile(flavorName, buildType)
				uploadTask.proxy = pluginExtension.proxy.orNull
				uploadTask.dryrun = isDryRun
				uploadTask.dependsOn(webIconLabelTask)
				// ensure that the compilation tasks are run before, IF they're run
				uploadTask.mustRunAfter(assembleTaskName, metadataTask)

				uploadTask.doFirst {
					if (optimizeForLocalBuild) {
						throw GradleException("Cannot publish to Alpaka with ${AlpakaProperties.OPTIMIZE_FOR_LOCAL_BUILD} set to true")
					}
				}
			}

			// assembleAndPublishToAlpaka{$variant} task to assemble and publish to alpaka
			val assembleAndPublishToAlpakaTaskName = "assembleAndPublishToAlpaka$variantNameCapitalized"
			project.tasks.register(
				assembleAndPublishToAlpakaTaskName,
				AssembleAndPublishToAlpakaTask::class.java
			) { assembleAndPublishTask ->
				assembleAndPublishTask.dryrun = isDryRun
				assembleAndPublishTask.dependsOn(assembleTaskName, publishToAlpakaTaskName)
			}

			// uploadToAlpaka{$variant} deprecated task to assemble and publish to alpaka, kept for backwards compatibility reasons
			val legacyUploadTaskName = "uploadToAlpaka$variantNameCapitalized"
			@Suppress("DEPRECATION")
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

	private fun Project.getAndroidExtension(): ApplicationExtension {
		return extensions.findByType(ApplicationExtension::class.java)
			?: throw GradleException("Android Gradle Plugin has not been applied before")
	}

	private fun Project.getAndroidComponentsExtension(): ApplicationAndroidComponentsExtension {
		return extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
			?: throw GradleException("Android Gradle Plugin has not been applied before")
	}

	private fun Project.getBuildTimestampProvider(): Provider<Long> {
		return providers.of(BuildTimestampValueSource::class.java) {}
	}

	private fun Project.getLocalBuildTimestampProvider(): Provider<Long> {
		return project.provider {
			// Peru's first National Alpaca Day
			1343824962042L
		}
	}

	private fun Project.getSignatureProvider(variant: ApplicationVariant, androidExtension: ApplicationExtension): Provider<String> {
		return providers.of(SignatureValueSource::class.java) {
			it.parameters.signingConfig = provider {
				val buildTypeName = variant.buildType ?: return@provider null
				val buildType = androidExtension.buildTypes.findByName(buildTypeName)
				buildType?.signingConfig?.run {
					AndroidSigningConfigData(storeType, storeFile, storePassword, keyAlias, keyPassword)
				}
			}
		}
	}

	private fun Project.getGitBranchProvider(): Provider<String> {
		return providers.of(GitBranchValueSource::class.java) {
			it.parameters.projectDirProvider = provider { rootProject.projectDir }
		}
	}

	private fun Project.getGitCommitLogProvider(numOfCommits: Int, allowFetch: Boolean): Provider<String> {
		return providers.of(GitCommitLogValueSource::class.java) {
			it.parameters.projectDirProvider = provider { rootProject.projectDir }
			it.parameters.numOfCommits = provider { numOfCommits }
			it.parameters.allowFetch = provider { allowFetch }
		}
	}

	private fun Project.findWebIcon(flavor: String): Provider<File> {
		return project.provider {
			val moduleDir = project.projectDir
			val dirs = sequenceOf(File(moduleDir, "src/$flavor"), File(moduleDir, "src/main"), moduleDir)
			dirs.flatMap { it.listFilesOrEmpty() }
				.find { it.name.matches(Regex(".*(web|playstore|512)\\.(png|webp)")) }
				?: throw GradleException("Must provide web icon matching (web|playstore|512).(png|webp) in one of the following locations:\n  ${dirs.joinToString()}")
		}
	}

	private fun Project.getGeneratedWebIconFile(flavor: String, buildType: String): Provider<File> {
		return layout.buildDirectory.file("outputs/launcher-icon/$flavor/$buildType/web-icon.png").map { it.asFile }
	}

	private fun Project.getGeneratedAppMetadataFile(flavor: String, buildType: String): Provider<File> {
		return layout.buildDirectory.file("outputs/alpaka/$flavor/$buildType/metadata.json").map { it.asFile }
	}

	private fun ApplicationVariant.getUploadKey(androidExtension: ApplicationExtension): String? {
		val productFlavor = getProductFlavors(androidExtension).firstOrNull()
		return productFlavor?.alpakaUploadKey ?: androidExtension.defaultConfig.alpakaUploadKey
	}

	private fun ApplicationVariant.getLauncherIconLabel(androidExtension: ApplicationExtension): String? {
		val productFlavors = getProductFlavors(androidExtension)
		val flavorLabel = productFlavors.firstNotNullOfOrNull { it.launcherIconLabel }
		return flavorLabel ?: androidExtension.defaultConfig.launcherIconLabel
	}

	/**
	 * Finds a property by checking Gradle project properties first (CLI -P, gradle.properties,
	 * ~/.gradle/gradle.properties), then falling back to local.properties in the root project
	 * directory (which Gradle does not load automatically, unlike the Android Gradle Plugin).
	 */
	private fun Project.findLocalProperty(key: String): String? {
		findProperty(key)?.toString()?.let { return it }
		val localProperties = rootProject.file("local.properties")
		if (localProperties.exists()) {
			val props = java.util.Properties()
			localProperties.inputStream().use { props.load(it) }
			props.getProperty(key)?.let { return it }
		}
		return null
	}

}
