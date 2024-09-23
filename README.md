# Linth Gradle Plugin

This plugin configures an Android project for build and upload to the Ubique Linth backend.

## Functionality

The plugin contains the following functionality:

* Enables the generation of the `BuildConfig` class
* Writes build information as fields to the `BuildConfig` class (see section [Build information](#build-information))
* Registers a task `injectMetadataIntoManifest<VARIANT>` for each variant that injects the build information into the Android
  manifest
* Registers a task `generateAppIcon<VARIANT>` for each variant that generates an app icon with the flavor name as overlay
* Registers a task `uploadToLinth<VARIANT>` for each release variant that uploads the APK to the Linth backend

## Configuration

After applying the plugin to your project, you can set the following configuration in your build.gradle.kts:

```kotlin
linthPlugin {
	uploadKey = "..." // The upload key identifying an application in the Linth backend (required)
	changelogCommitCount = 10 // The number of commits to include in the changelog (optional, defaults to 10)
	proxy = "host:port" // An optional proxy to set for the upload task. Use for local debugging only 
    labelAppIcons = false // Globally configure the generateAppIcon tasks to label with flavor name (optional, default is enabled)
}

android { 
	productFlavors {
	    create("dev") {
          extraProperties.set("launcherIconLabel", "tescht") // Modify the default icon label per flavor (optional, nullable)
          extraProperties.set("uploadKey", "...") // Modify the default uploadKey per flavor (optional)
	    }
    }
}
```

## Build information

These are the build information that are written to the `BuildConfig` class and Android manifest:

| BuildConfig name  | Android manifest name             | Description                                                              | Value                                                                       |
|-------------------|-----------------------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `BUILD_BATCH`     | `ch.ubique.linth.build.batch`     | An ID for a group of builds that belong together (e.g. multiple flavors) | `build_batch` Gradle property, defaults to `0`                              |
| `BUILD_ID`        | `ch.ubique.linth.build.id`        | An ID for this build                                                     | `build_id` or `ubappid` Gradle property, defaults to `localbuild`           |
| `BUILD_NUMBER`    | `ch.ubique.linth.build.number`    | An incremental number of this build                                      | `build_number` Gradle property, defaults to `0`                             |
| `BUILD_TIMESTAMP` | `ch.ubique.linth.build.timestamp` | The timestamp of this build                                              | `build_timestamp` Gradle property, defaults to `System.currentTimeMillis()` |
| `BRANCH`          | `ch.ubique.linth.branch`          | The Git branch this build was created from                               | `branch` Gradle property, defaults to calling the systems Git command line  |
| `FLAVOR`          | `ch.ubique.linth.flavor`          | The product flavor this build was created from                           | Product flavor name of the variant that started the gradle task             |