package ch.ubique.linth

import ch.ubique.linth.common.capitalize
import ch.ubique.linth.model.UploadRequest
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class LinthPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		val extension = project.extensions.create("linthPlugin", LinthPluginConfig::class.java, project)

		val androidExtension = getAndroidExtension(project)

		//hook injectMetaTask into android build process
		project.afterEvaluate {
			androidExtension.applicationVariants.forEach { variant ->
				val flavor = variant.flavorName.capitalize()
				val buildType = variant.buildType.name.capitalize()
				val flavorBuild = flavor + buildType

				val injectMetaTask = project.tasks.register(
					"injectMetaDataIntoManifest$flavorBuild",
					InjectMetaIntoManifestTask::class.java
				) { manifestTask ->
					manifestTask.flavor = flavor
					manifestTask.buildType = buildType
				}

				variant.outputs.forEach { output ->
					output.processManifestProvider.get().finalizedBy(injectMetaTask)
				}
			}
		}

		//hook iconTask into android build process
		project.afterEvaluate {

			val buildDir = project.layout.buildDirectory.asFile.get()

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

			androidExtension.applicationVariants.forEach { variant ->
				val variantName = variant.name.capitalize()
				val flavor = variant.flavorName.capitalize()
				val buildType = variant.buildType.name.capitalize()
				val flavorBuild = flavor + buildType

				val iconTask = project.tasks.register("generateAppIcon$flavorBuild", IconTask::class.java) { iconTask ->
					iconTask.flavor = flavor
					iconTask.buildType = buildType
					iconTask.targetWebIcon = null
				}

				variant.outputs.forEach { output ->
					iconTask.dependsOn(output.processManifestProvider)
					project.tasks.named("generate${variantName}Resources") {
						it.dependsOn(iconTask)
					}
				}

			}
		}

		//hook uploadTask into android build process
		project.afterEvaluate {

			androidExtension.applicationVariants.forEach { variant ->
				val flavor = variant.flavorName.capitalize()
				val buildType = variant.buildType.name.capitalize()
				val flavorBuild = flavor + buildType
				if (buildType != "Release") return@forEach

				val packageName = variant.applicationId
				val minSdk = requireNotNull(androidExtension.defaultConfig.minSdk)
				val targetSdk = requireNotNull(androidExtension.defaultConfig.targetSdk)
				val versionName = requireNotNull(androidExtension.defaultConfig.versionName)

				val uploadRequest = variant.outputs.first().let {
					UploadRequest(
						apk = it.outputFile,
						appIcon = it.outputFile,
						appName = "", //will be set from manifest
						packageName = packageName,
						flavor = flavor,
						branch = "", //will be set inside uploadTask
						minSdk = minSdk,
						targetSdk = targetSdk,
						usesFeature = emptyList(), //will be set from manifest
						buildNumber = 0L,
						buildTime = 0L,
						buildBatch = "buildBatch",
						changelog = "", //will be set inside uploadTask
						signature = "someFancySignature",
						version = versionName,
					)
				}

				project.tasks.register("uploadToLinth$flavorBuild", UploadToLinthBackend::class.java) { uploadTask ->
					uploadTask.flavor = flavor
					uploadTask.buildType = buildType
					uploadTask.uploadKey = extension.uploadKey.get()
					uploadTask.proxy = extension.proxy.orNull
					uploadTask.uploadRequest = uploadRequest
				}
			}
		}

	}

	private fun getAndroidExtension(project: Project): AppExtension {
		val ext = project.extensions.findByType(AppExtension::class.java) ?: error(
			"Android gradle plugin extension has not been applied before"
		)
		return ext
	}
}
