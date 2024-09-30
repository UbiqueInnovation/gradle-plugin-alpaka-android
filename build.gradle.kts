plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.layout.buildDirectory)
}

tasks.register("preMerge") {
    description = "Runs all the tests/verification tasks on both top level and included build."

    dependsOn(":examplekts:check")
    dependsOn(":examplegroovy:check")
    dependsOn(gradle.includedBuild("alpaka").task(":plugin:check"))
    dependsOn(gradle.includedBuild("alpaka").task(":plugin:validatePlugins"))
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
