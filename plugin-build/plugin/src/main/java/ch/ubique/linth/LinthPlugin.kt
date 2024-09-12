package ch.ubique.linth

import ch.ubique.linth.common.GitUtils
import ch.ubique.linth.common.capitalize
import ch.ubique.linth.model.UploadRequest
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.ProguardFiles.getDefaultProguardFile
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

abstract class LinthPlugin : Plugin<Project> {

	companion object {
		private const val DEBUG_KEYSTORE_SIGNATURE_MD5 = "87:73:90:5A:BB:B4:58:47:CB:E5:9E:53:D3:7A:71:19"
	}

	override fun apply(project: Project) {
		val extension = project.extensions.create("linthPlugin", LinthPluginConfig::class.java, project)

		// The build ID is a unique UUID for each build
		val buildId = project.findProperty("build_id")?.toString() ?: project.findProperty("ubappid")?.toString() ?: "localbuild"

		// The build number is the run number of a build pipeline (e.g. GitHub workflow run number)
		val buildNumber = project.findProperty("build_number")?.toString()?.toLongOrNull() ?: 0L

		// The build batch is a unique UUID across all builds of a certain batch (e.g. all flavors of a commit)
		val buildBatch = project.findProperty("build_batch")?.toString() ?: "0"

		// The build timestamp is the timestamp when the build was started
		val buildTimestamp = project.findProperty("build_timestamp")?.toString()?.toLongOrNull() ?: System.currentTimeMillis()

		// The build branch is the Git name of the branch
		val buildBranch = project.findProperty("branch")?.toString() ?: GitUtils.obtainBranch()

		val androidExtension = getAndroidExtension(project)
		val androidComponentExtension = getAndroidComponentsExtension(project)

		// Default flavors
		androidExtension.flavorDimensions("default")
		androidExtension.productFlavors.register("dev") {
			it.dimension = "default"
			it.applicationIdSuffix = ".dev"
		}
		androidExtension.productFlavors.register("prod") {
			it.dimension = "default"
		}

		// Enable BuildConfig
		androidExtension.buildFeatures.buildConfig = true

		// Set BuildConfig fields
		androidExtension.defaultConfig.apply {
			buildConfigField("String", "BUILD_ID", "\"$buildId\"")
			buildConfigField("long", "BUILD_NUMBER", "${buildNumber}L")
			buildConfigField("String", "BUILD_BATCH", "\"$buildBatch\"")
			buildConfigField("long", "BUILD_TIMESTAMP", "${buildTimestamp}L")
			buildConfigField("String", "BRANCH", "\"$buildBranch\"")
		}

		// Add flavor boolean fields to BuildConfig
		androidExtension.productFlavors.forEach { flavor ->
			val sanitizedFlavorName = flavor.name.replace("[^a-zA-Z0-9_]", "_").uppercase()
			val flavorFieldName = "IS_FLAVOR_$sanitizedFlavorName"

			// true for this flavor ...
			flavor.buildConfigField("boolean", flavorFieldName, "true")
			// ... false for all others
			androidExtension.defaultConfig.buildConfigField("boolean", flavorFieldName, "false")
		}

		// Release build config
		androidExtension.buildTypes.maybeCreate("release").apply {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android.txt", project.layout.buildDirectory), "proguard-rules.pro")
		}

		// R8 full mode check
		if (project.findProperty("android.enableR8.fullMode") != "false" && project.findProperty("android.enableR8.fullModeAllowed") != "true") {
			throw IllegalArgumentException("R8 full mode is enabled. Disable it with android.enableR8.fullMode=false or allow it by setting android.enableR8.fullModeAllowed=true")
		}

		// Exclude library version files on release builds
		androidComponentExtension.onVariants { variant ->
			if (variant.buildType == "release") {
				variant.packaging.resources.excludes.add("META-INF/*.version")
			}
		}

		// Compile with Java 17 compatibility
		androidExtension.compileOptions.apply {
			sourceCompatibility = JavaVersion.VERSION_17
			targetCompatibility = JavaVersion.VERSION_17
		}

		// Let Kotlin target JVM 17
		project.tasks.withType(KotlinCompile::class.java) { task ->
			task.compilerOptions.jvmTarget.set(JvmTarget.JVM_17) // Kotlin 1.8+
			@Suppress("DEPRECATION")
			task.kotlinOptions.jvmTarget = "17" // Deprecated since Kotlin 1.8
		}

		// Signing Config
		androidExtension.signingConfigs.register("ubique") { signingConfig ->
			signingConfig.storeFile = project.getKeystoreFile()
			signingConfig.storePassword = "android"
			signingConfig.keyAlias = "androiddebugkey"
			signingConfig.keyPassword = "android"
		}
		androidExtension.buildTypes.forEach { buildType ->
			buildType.signingConfig = androidExtension.signingConfigs.getByName("ubique")
		}

		// Lint settings
		androidExtension.lintOptions.isAbortOnError = false

		// Keystore generation
		project.afterEvaluate {
			androidExtension.applicationVariants.configureEach { variant ->
				val generateKeystoreTask = project.tasks.register(
					"generateKeyStore${variant.name.capitalize()}"
				) { task ->
					task.doLast { project.generateKeystoreFile() }
				}
				variant.assembleProvider.configure { assembleTask ->
					assembleTask.dependsOn.add(generateKeystoreTask)
				}
			}
		}

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
					manifestTask.buildId = buildId
					manifestTask.buildNumber = buildNumber
					manifestTask.buildBatch = buildBatch
					manifestTask.buildTimestamp = buildTimestamp
					manifestTask.buildBranch = buildBranch
				}

				variant.outputs.forEach { output ->
					output.processManifestProvider.get().finalizedBy(injectMetaTask)
				}
			}
		}

		//hook iconTask into android build process
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

			androidExtension.applicationVariants.forEach { variant ->
				val variantName = variant.name.capitalize()
				val flavor = variant.flavorName.capitalize()
				val buildType = variant.buildType.name.capitalize()
				val flavorBuild = flavor + buildType

				val iconTask = project.tasks.register("generateAppIcon$flavorBuild", IconTask::class.java) { iconTask ->
					iconTask.flavor = flavor
					iconTask.buildType = buildType
					iconTask.targetWebIcon = File(getWebIconPath(buildDir, flavor)).also {
						it.parentFile.mkdirs()
						it.createNewFile()
					}
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
			val buildDir = project.getBuildDirectory()

			androidExtension.applicationVariants.forEach { variant ->
				val flavor = variant.flavorName
				val buildType = variant.buildType.name.capitalize()
				val flavorBuild = flavor.capitalize() + buildType
				if (buildType != "Release") return@forEach

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
						usesFeature = emptyList(), // Will be set from manifest inside the task TODO: Not yet implemented
						buildId = buildId,
						buildNumber = buildNumber,
						buildTime = buildTimestamp,
						buildBatch = buildBatch,
						changelog = "", // Will be set inside the task
						signature = extension.signature.getOrElse(DEBUG_KEYSTORE_SIGNATURE_MD5),
						version = versionName,
					)
				}

				project.tasks.register("uploadToLinth$flavorBuild", UploadToLinthBackend::class.java) { uploadTask ->
					uploadTask.dependsOn("assemble$flavorBuild")
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
		val ext = project.extensions.findByType(AppExtension::class.java)
			?: throw GradleException("Android gradle plugin extension has not been applied before")
		return ext
	}

	private fun getAndroidComponentsExtension(project: Project): AndroidComponentsExtension<*, *, *> {
		val ext = project.extensions.findByType(AndroidComponentsExtension::class.java)
			?: throw GradleException("Android gradle plugin extension has not been applied before")
		return ext
	}

	private fun Project.getKeystoreFile(): File {
		return this.file("${getBuildDirectory()}/generated/ubique/debug.keystore")
	}

	private fun Project.generateKeystoreFile() {
		val keystoreFile = getKeystoreFile()
		if (keystoreFile.length() > 4) return
		keystoreFile.parentFile.mkdirs()
		val resourceStream = this@LinthPlugin.javaClass.getResourceAsStream("/debug.keystore")
		if (resourceStream != null) {
			resourceStream.use { resource -> keystoreFile.outputStream().use { output -> resource.copyTo(output) } }
			logger.info("Generated debug.keystore (${keystoreFile.length()}B)")
		} else {
			throw GradleException("Failed to find debug.keystore in resources")
		}
	}

	private fun Project.getBuildDirectory(): File {
		return project.layout.buildDirectory.asFile.get()
	}

	private fun getWebIconPath(buildDir: File, flavor: String): String {
		return "$buildDir/generated/res/launcher-icon/${flavor.lowercase()}/web-icon.png"
	}
}
