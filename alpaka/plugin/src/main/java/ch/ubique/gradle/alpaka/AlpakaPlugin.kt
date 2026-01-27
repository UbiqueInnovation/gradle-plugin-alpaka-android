package ch.ubique.gradle.alpaka

import ch.ubique.gradle.alpaka.config.AlpakaPluginConfig
import ch.ubique.gradle.alpaka.extensions.android.*
import ch.ubique.gradle.alpaka.extensions.capitalize
import ch.ubique.gradle.alpaka.extensions.getResDirs
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
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
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

		// The build ID is a unique ID for each build
		val buildId = project.findProperty("build_id")?.toString() ?: project.findProperty("ubappid")?.toString() ?: "localbuild"

		// The build number is the run number of a build pipeline (e.g. GitHub workflow run number)
		val buildNumber = project.findProperty("build_number")?.toString()?.toLongOrNull() ?: 0L

		// The build batch is a unique ID across all builds of a certain batch (e.g. all flavors of a commit)
		val buildBatch = project.findProperty("build_batch")?.toString() ?: "0"

		// The build timestamp is the timestamp when the build was started
		val buildTimestampProvider = project.findProperty("build_timestamp")
			?.toString()
			?.toLongOrNull()
			?.let { project.provider { it } }
			?: project.getBuildTimestampProvider()

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

		// Hook injectMetadataTask into android build process
		androidComponentExtension.onVariants { variant ->
			val variantName = variant.name
			val variantNameCapitalized = variantName.capitalize()
			val flavor = variant.requireFlavorName()
			val buildType = variant.requireBuildType()

			val injectManifestTask = project.tasks.register(
				"injectMetadataIntoManifest$variantNameCapitalized",
				InjectMetadataIntoManifestTask::class.java
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

			variant.artifacts.use(injectManifestTask)
				.wiredWithFiles(InjectMetadataIntoManifestTask::inputManifest, InjectMetadataIntoManifestTask::outputManifest)
				.toTransform(SingleArtifact.MERGED_MANIFEST)
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
			val flavorName = variant.requireFlavorName()
			val productFlavors = variant.productFlavors.map { (dimension, flavor) -> flavor }
			val buildType = variant.requireBuildType()
			val labelValue = getLauncherIconLabel(variant, androidExtension)

			val doLabelAppIcons = pluginExtension.labelAppIcons.getOrElse(true)

			val mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)

			val launcherIconLabelTask = project.tasks.register(
				"labelAppIcon$variantNameCapitalized",
				LauncherIconLabelTask::class.java
			) { iconTask ->
				iconTask.variantName = variantName
				iconTask.buildType = buildType
				iconTask.labelValue = if (doLabelAppIcons) labelValue else null
				iconTask.sourceWebIconFile = project.provider { findWebIcon(project.projectDir, flavorName) }
				iconTask.mergedManifestFile.set(mergedManifest)
				iconTask.generatedWebIcon = getGeneratedWebIconFile(project.layout.buildDirectory, flavorName, buildType)
				iconTask.generatedIconDir.set(project.getGeneratedIconDir(flavorName, buildType))

				val flavorNames = setOf(flavorName) + productFlavors
				iconTask.resDirs.from(project.getResDirs(flavorNames))

				iconTask.buildLogicFiles.from(
					project.file("build.gradle"),
					project.file("build.gradle.kts"),
					project.rootProject.file("build.gradle"),
					project.rootProject.file("build.gradle.kts"),
					project.rootProject.file("settings.gradle"),
					project.rootProject.file("settings.gradle.kts"),
					project.rootProject.file("gradle/libs.versions.toml"),
				)

				iconTask.outputs.upToDateWhen { false } // always run the task

				iconTask.mustRunAfter(project.tasks.named("injectMetadataIntoManifest$variantNameCapitalized"))
			}

			if (doLabelAppIcons) {
				// TODO: what happens of the generatedIconDir value is not set manually
				variant.sources.requireRes().addGeneratedSourceDirectory(launcherIconLabelTask, LauncherIconLabelTask::generatedIconDir)
			}

			project.afterEvaluate {
				// TODO: dependencies no longer needed?
				project.tasks.named("map${variantNameCapitalized}SourceSetPaths") { it.dependsOn(launcherIconLabelTask) }
				project.tasks.named("generate${variantNameCapitalized}Resources") { it.dependsOn(launcherIconLabelTask) }
				project.tasks.named("process${variantNameCapitalized}NavigationResources") { it.dependsOn(launcherIconLabelTask) }
				project.tasks.matching { it.name == "extract${variantNameCapitalized}SupportedLocales" }
					.configureEach { it.dependsOn(launcherIconLabelTask) }
			}
		}

		// Hook alpaka task into android build process
		androidComponentExtension.onVariants { variant ->
			val buildType = variant.buildType
			if (buildType != "release") return@onVariants

			val isDryRun = project.findProperty("alpakaDryrun")?.toString()?.toBoolean() ?: false

			val variantName = variant.name
			val variantNameCapitalized = variantName.capitalize()
			val flavorName = variant.requireFlavorName()
			val uploadKey = getUploadKey(variant, androidExtension)

			val assembleTaskName = "assemble$variantNameCapitalized"

			// compileAlpakaMetadata{$variant} task to compile alpaka mata data
			val metadataTask = project.tasks.register(
				"compileAlpakaMetadata$variantNameCapitalized",
				CompileAlpakaMetadataTask::class.java
			) { metadataTask ->
				metadataTask.applicationId = variant.applicationId
				metadataTask.flavorName = flavorName
				metadataTask.androidConfig = AndroidBuildConfigData(
					minSdk = requireNotNull(androidExtension.defaultConfig.minSdk),
					targetSdk = requireNotNull(androidExtension.defaultConfig.targetSdk),
					versionName = requireNotNull(androidExtension.defaultConfig.versionName),
					versionCode = androidExtension.defaultConfig.versionCode?.toLong() ?: 0L,
				)

				metadataTask.signature = project.getSignatureProvider(variant, androidExtension)

				val resDirs = project.getResDirs(flavorName) +
						project.layout.buildDirectory.file("generated/res/resValues/$flavorName/$buildType").get().asFile
				metadataTask.resDirs.from(resDirs)

				val mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
				metadataTask.mergedManifestFile.set(mergedManifest)
				val commitCount = pluginExtension.changelogCommitCount.orElse(10).get()
				metadataTask.vcsCommitHistory = project.getGitCommitLogProvider(commitCount)
				metadataTask.vcsBranch = vcsBranchProvider
				metadataTask.vcsCommitHash = vcsCommitHash
				metadataTask.buildId = buildId
				metadataTask.buildNumber = buildNumber
				metadataTask.buildTime = buildTimestampProvider
				metadataTask.buildBatch = buildBatch
				metadataTask.metadataFile = getGeneratedAppMetadataFile(project.layout.buildDirectory, flavorName, buildType)
			}
			project.afterEvaluate {
				project.tasks.named(assembleTaskName) { it.finalizedBy(metadataTask) }
			}

			// publishToAlpaka{$variant} task to only publish to alpaka
			val publishToAlpakaTaskName = "publishToAlpaka$variantNameCapitalized"
			project.tasks.register(
				publishToAlpakaTaskName,
				PublishToAlpakaTask::class.java
			) { uploadTask ->
				uploadTask.uploadKey = uploadKey ?: throw GradleException("No alpakaUploadKey specified")
				uploadTask.apk = variant.artifacts.getApk()
				uploadTask.webIcon = getGeneratedWebIconFile(project.layout.buildDirectory, flavorName, buildType)
				uploadTask.appMetadataJsonFile = getGeneratedAppMetadataFile(project.layout.buildDirectory, flavorName, buildType)
				uploadTask.proxy = pluginExtension.proxy.orNull
				uploadTask.dryrun = isDryRun
				// ensure that the compilation tasks are run before, IF they're run
				uploadTask.mustRunAfter(assembleTaskName, metadataTask)
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

	private fun Project.getGitCommitLogProvider(numOfCommits: Int): Provider<String> {
		return providers.of(GitCommitLogValueSource::class.java) {
			it.parameters.projectDirProvider = provider { rootProject.projectDir }
			it.parameters.numOfCommits = provider { numOfCommits }
		}
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

	private fun Project.getGeneratedIconDir(flavor: String, buildType: String): Provider<Directory> {
		return layout.buildDirectory.dir("generated/res/launcher-icon/$flavor/$buildType/res")
	}

	private fun getGeneratedAppMetadataFile(buildDir: DirectoryProperty, flavor: String, buildType: String): Provider<File> {
		return buildDir.file("outputs/alpaka/$flavor/$buildType/metadata.json").map { it.asFile }
	}

	private fun getUploadKey(variant: ApplicationVariant, androidExtension: ApplicationExtension): String? {
		val productFlavor = variant.getProductFlavors(androidExtension).firstOrNull()
		return productFlavor?.alpakaUploadKey ?: androidExtension.defaultConfig.alpakaUploadKey
	}

	private fun getLauncherIconLabel(variant: ApplicationVariant, androidExtension: ApplicationExtension): String? {
		val productFlavors = variant.getProductFlavors(androidExtension)
		val flavorLabel = productFlavors.firstNotNullOfOrNull { it.launcherIconLabel }
		return flavorLabel ?: androidExtension.defaultConfig.launcherIconLabel
	}

}
