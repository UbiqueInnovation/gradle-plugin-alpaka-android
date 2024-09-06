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

		//hook manifestTask into android build process
		val injectMetaTask = project.tasks.register("injectMetaDataIntoManifest", InjectMetaIntoManifestTask::class.java) { manifestTask ->

			val flavorAndBuildType = mutableSetOf<Pair<String, String>>()

			androidExtension.applicationVariants.forEach { variant ->
				val flavor = variant.flavorName.capitalize()
				val buildType = variant.buildType.name.capitalize()
				flavorAndBuildType.add(flavor to buildType)
			}
			manifestTask.flavorAndBuildType = flavorAndBuildType
		}

		//hook Manifest into android build process
		project.afterEvaluate {
			androidExtension.applicationVariants.forEach { variant ->
				variant.outputs.forEach { output ->
					output.processManifestProvider.get().finalizedBy(injectMetaTask)
				}
			}
		}

		val iconTask = project.tasks.register("generateAppIcon", IconTask::class.java) { iconTask ->

			val flavors = mutableSetOf<String>()

			androidExtension.applicationVariants.forEach { variant ->
				val flavor = variant.flavorName.capitalize()
				flavors.add(flavor)
			}

			iconTask.flavors = flavors
		}

		//hook iconTask into android build process
		project.afterEvaluate {
			androidExtension.applicationVariants.forEach { variant ->
				val variantName = variant.name.capitalize()
				variant.outputs.forEach { output ->
					iconTask.dependsOn(output.processManifestProvider)
					project.tasks.named("generate${variantName}Resources") {
						it.dependsOn(iconTask)
					}
				}
			}
		}



		project.tasks.register("uploadToUbDiag", UploadToUbDiagTask::class.java) { uploadTask ->

			uploadTask.uploadKey = extension.uploadKey.get()
			uploadTask.proxy = extension.proxy.orNull

			// setup upload task
			val uploadRequests = mutableListOf<UploadRequest>()
			val uploadFlavors = extension.flavors.orNull?.split(",")?.map { it.trim() }

			val minSdk = requireNotNull(androidExtension.defaultConfig.minSdk)
			val targetSdk = requireNotNull(androidExtension.defaultConfig.targetSdk)
			val versionName = requireNotNull(androidExtension.defaultConfig.versionName)

			androidExtension.applicationVariants.forEach { variant ->
				val flavor = variant.flavorName.capitalize()
				val packageName = variant.applicationId
				if (uploadFlavors == null || uploadFlavors.contains(variant.flavorName)) {
					uploadTask.dependsOn(project.tasks.named("assemble${flavor}Release"))

					variant.outputs.forEach {
						if (it.outputFile.parentFile.name == "release") {
							val uploadRequest = UploadRequest(
								apk = it.outputFile,
								appIcon = it.outputFile,
								appName = "some fancy name",
								packageName = packageName,
								flavor = flavor,
								branch = "someFancyBranch",
								minSdk = minSdk,
								targetSdk = targetSdk,
								usesFeature = emptyList(),
								buildNumber = 0L,
								buildTime = 0L,
								buildBatch = "buildBatch",
								changelog = "Some fancy changelog",
								signature = "someFancySignature",
								version = versionName,
							)
							uploadRequests.add(uploadRequest)
						}
					}
				}
			}

			uploadTask.uploadRequests = uploadRequests.toList()
		}

	}

	private fun getAndroidExtension(project: Project): AppExtension {
		val ext = project.extensions.findByType(AppExtension::class.java) ?: error(
			"Android gradle plugin extension has not been applied before"
		)
		return ext
	}
}
