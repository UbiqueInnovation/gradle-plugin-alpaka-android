import com.vanniktech.maven.publish.GradlePublishPlugin
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	`java-gradle-plugin`
	alias(libs.plugins.pluginPublish)
	alias(libs.plugins.vanniktech)
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
		}
	}
}

mavenPublishing {
	configure(GradlePublishPlugin())
	coordinates(property("GROUP").toString(), property("ARTIFACT_ID").toString(), project.version.toString())
	publishToMavenCentral(SonatypeHost.S01, true)
	signAllPublications()
}

// enable test logging with gradle
project.tasks.withType(Test::class.java).configureEach {
	testLogging {
		exceptionFormat = TestExceptionFormat.FULL
		events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
		showStandardStreams = true
	}
}