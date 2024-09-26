plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.pluginPublish) apply false
    alias(libs.plugins.jfrog.artifactory) apply false
}

allprojects {
    group = property("GROUP").toString()
    version = getProjectVersion()
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.layout.buildDirectory)
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

private fun getProjectVersion(): String {
    val versionFromGradleProperties = property("VERSION").toString()
    val versionFromWorkflow = runCatching { property("githubRefName").toString().removePrefix("v") }.getOrNull()
    return versionFromWorkflow ?: versionFromGradleProperties
}