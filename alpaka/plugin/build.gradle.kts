import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	`java-gradle-plugin`
	alias(libs.plugins.pluginPublish)
	alias(libs.plugins.jfrog.artifactory)
}

dependencies {
	implementation(kotlin("stdlib"))
	implementation(gradleApi())

	implementation(libs.agp)
	implementation(libs.kotlin.gradle)

	implementation(libs.okhttp)
	implementation(libs.retrofit)
	implementation(libs.retrofitConverterScalars)
	implementation(libs.moshi)
	implementation(libs.moshiKotlin)
	implementation(libs.moshiAdapters)

	testImplementation(libs.junit)
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
	compilerOptions.jvmTarget = JvmTarget.JVM_17
	kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
}

gradlePlugin {
	plugins {
		create(property("ID").toString()) {
			id = property("ID").toString()
			implementationClass = property("IMPLEMENTATION_CLASS").toString()
			version = project.version
			description = property("DESCRIPTION").toString()
			displayName = property("DISPLAY_NAME").toString()
			tags = listOf("android", "ubique")
		}
	}
	website.set(property("WEBSITE").toString())
	vcsUrl.set(property("VCS_URL").toString())
}

tasks.create("setupPluginUploadFromEnvironment") {
	doLast {
		val key = System.getenv("GRADLE_PUBLISH_KEY")
		val secret = System.getenv("GRADLE_PUBLISH_SECRET")

		if (key == null || secret == null) {
			throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
		}

		System.setProperty("gradle.publish.key", key)
		System.setProperty("gradle.publish.secret", secret)
	}
}

// MAVEN PUBLISHING

publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components.getByName("java"))
			artifactId = property("ARTIFACT_ID").toString()
			groupId = property("GROUP").toString()
			version = project.version.toString()
		}
	}
}

artifactory {
	val url = System.getenv("UB_ARTIFACTORY_URL") ?: project.property("ubiqueMavenRootUrl")?.toString()
	setContextUrl(url)
	publish {
		repository {
			repoKey = System.getenv("UB_ARTIFACTORY_REPO_ANDROID") ?: project.property("ubiqueMavenRepoName")?.toString()
			username = System.getenv("UB_ARTIFACTORY_USER") ?: project.property("ubiqueMavenUser")?.toString()
			password = System.getenv("UB_ARTIFACTORY_PASSWORD") ?: project.property("ubiqueMavenPass")?.toString()
		}

		defaults {
			publications("mavenJava")
			setPublishArtifacts(true)
			setProperties(
				mapOf(
					"build.status" to this.project.status.toString()
				)
			)
			setPublishPom(true)
			setPublishIvy(false)
		}
	}
}

// enable test logging with gradle
project.tasks.withType(Test::class.java).configureEach {
	testLogging {
		exceptionFormat = TestExceptionFormat.FULL
		events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
		showStandardStreams = true
	}
}