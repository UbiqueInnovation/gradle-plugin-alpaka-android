package ch.ubique.gradle.linth

import ch.ubique.gradle.linth.config.LinthPluginConfig
import ch.ubique.gradle.linth.extensions.capitalize
import ch.ubique.gradle.linth.extensions.getMergedManifestFile
import ch.ubique.gradle.linth.model.UploadRequest
import ch.ubique.gradle.linth.task.IconTask
import ch.ubique.gradle.linth.task.InjectMetadataIntoManifestTask
import ch.ubique.gradle.linth.task.UploadToLinthBackendTask
import ch.ubique.gradle.linth.utils.GitUtils
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

abstract class LinthPlugin : Plugin<Project> {

	companion object {
		private const val DEBUG_KEYSTORE_SIGNATURE_MD5 = "87:73:90:5A:BB:B4:58:47:CB:E5:9E:53:D3:7A:71:19"
	}

	override fun apply(project: Project) {
		val pluginExtension = project.extensions.create("linthPlugin", LinthPluginConfig::class.java, project)
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

				project.tasks.named("process${variantName.capitalize()}ManifestForPackage") { it.dependsOn(injectManifestTask) }
				variant.outputs.forEach { output ->
					output.processManifestProvider.get().finalizedBy(injectManifestTask)
				}
			}
		}

		// Hook iconTask into android build process
		project.afterEvaluate {
			val buildDir = project.getBuildDirectory()

			//make sure generated sources are used by build process
			androidExtension.productFlavors.configureEach { flavor ->
				// Add the property 'launcherIconLabel' to each product flavor and set the default value to its name
				//flavor.set("launcherIconLabel", flavor.name)
				//flavor.ext.set("launcherIconLabelEnabled", (Boolean) null)

				// Add generated icon path to res-SourceSet. This must be here otherwise it is too late!
				val sourceSet = androidExtension.sourceSets.maybeCreate(flavor.name)
				sourceSet.res {
					srcDir("$buildDir/generated/res/launcher-icon/${flavor.name}/")
				}
			}

			androidExtension.applicationVariants.configureEach { variant ->
				val variantName = variant.name
				val flavor = variant.flavorName
				val buildType = variant.buildType.name

				val iconTask = project.tasks.register(
					"generateAppIcon${variantName.capitalize()}",
					IconTask::class.java
				) { iconTask ->
					iconTask.variantName = variantName
					iconTask.flavor = flavor
					iconTask.buildType = buildType
					iconTask.targetWebIconPath = getWebIconPath(buildDir, flavor)
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
			val buildDir = project.getBuildDirectory()

			androidExtension.applicationVariants.configureEach { variant ->
				val variantName = variant.name
				val flavor = variant.flavorName
				val buildType = variant.buildType.name
				if (buildType != "release") return@configureEach

				val packageName = variant.applicationId
				val minSdk = requireNotNull(androidExtension.defaultConfig.minSdk)
				val targetSdk = requireNotNull(androidExtension.defaultConfig.targetSdk)
				val versionName = requireNotNull(androidExtension.defaultConfig.versionName)

				val uploadRequest = variant.outputs.first().let {
					UploadRequest(
						apk = it.outputFile,
						appIcon = File(getWebIconPath(buildDir, flavor)),
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
						signature = pluginExtension.signature.getOrElse(DEBUG_KEYSTORE_SIGNATURE_MD5),
						version = versionName,
					)
				}

				project.tasks.register("uploadToLinth${variantName.capitalize()}", UploadToLinthBackendTask::class.java) { uploadTask ->
					uploadTask.dependsOn("assemble${variantName.capitalize()}")
					uploadTask.variantName = variantName
					uploadTask.flavor = flavor
					uploadTask.buildType = buildType
					uploadTask.uploadKey = pluginExtension.uploadKey.get()
					uploadTask.proxy = pluginExtension.proxy.orNull
					uploadTask.commitCount = pluginExtension.changelogCommitCount.orNull
					uploadTask.uploadRequest = uploadRequest
				}
			}
		}
	}

	private fun getAndroidExtension(project: Project): AppExtension {
		val ext = project.extensions.findByType(AppExtension::class.java)
			?: throw GradleException("Android gradle plugin extension has not been applied before")
		return ext
	}

	private fun Project.getBuildDirectory(): File {
		return project.layout.buildDirectory.asFile.get()
	}

	private fun getWebIconPath(buildDir: File, flavor: String): String {
		return "$buildDir/generated/res/launcher-icon/${flavor.lowercase()}/web-icon.png"
	}
}
