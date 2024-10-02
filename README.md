# Alpaka Gradle Plugin

[![Build](https://github.com/UbiqueInnovation/gradle-plugin-alpaka-android/actions/workflows/build.yml/badge.svg)](https://github.com/UbiqueInnovation/gradle-plugin-alpaka-android/actions/workflows/build.yml)
[![Release](https://github.com/UbiqueInnovation/gradle-plugin-alpaka-android/actions/workflows/publish.yml/badge.svg)](https://github.com/UbiqueInnovation/gradle-plugin-alpaka-android/actions/workflows/publish.yml)
[![Maven Central](https://img.shields.io/maven-central/v/ch.ubique.gradle/alpaka.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22ch.ubique.gradle%22%20AND%20a:%22alpaka%22)

Upload your app to the Alpaka App Distribution, label your launcher icon with some flavors, and have some build metadata in the BuildConfig and the Android manifest.

## Functionality

The plugin contains the following functionality:

* Enables the generation of the `BuildConfig` class
* Writes build information as fields to the `BuildConfig` class (see section [Build information](#build-information))
* Registers a task `injectMetadataIntoManifest<VARIANT>` for each variant that injects the build information into the Android
  manifest
* Registers a task `generateAppIcon<VARIANT>` for each variant that generates an app icon with the flavor name as overlay
* Registers a task `uploadToAlpaka<VARIANT>` for each release variant that uploads the APK to the Alpaka backend

## Configuration

```kotlin
plugins {
    id("ch.ubique.gradle.alpaka") version "8.7.0"
}
```

The major and minor version goes in lockstep with the Android Gradle Plugin,
also see [Releases](https://github.com/UbiqueInnovation/gradle-plugin-alpaka-android/releases).

After applying the plugin to your project, you can set the following configuration in your build.gradle.kts:

```kotlin
android {
    defaultConfig {
        launcherIconLabel = "tescht" // The default icon label per flavor (optional, nullable)
        alpakaUploadKey = "..." // The upload key identifying an application in the Alpaka backend
    }

    productFlavors {
        create("dev") {
            launcherIconLabel = "tescht" // Modify the default icon label per flavor (optional, nullable)
            alpakaUploadKey = "..." // Modify the default uploadKey per flavor (optional)
        }
    }
}

alpaka {
    changelogCommitCount = 10 // The number of commits to include in the changelog (optional, defaults to 10)
    proxy = "host:port" // An optional proxy to set for the upload task. Use for local debugging only 
    labelAppIcons = false // Globally configure the generateAppIcon tasks to label with flavor name (optional, default is enabled)
}
```

## Build information

These are the build information that are written to the `BuildConfig` class and Android manifest:

| BuildConfig field | Android manifest meta-data name    | Description                                                              | Value                                                                       |
|-------------------|------------------------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `BUILD_BATCH`     | `ch.ubique.alpaka.build.batch`     | An ID for a group of builds that belong together (e.g. multiple flavors) | `build_batch` Gradle property, defaults to `0`                              |
| `BUILD_ID`        | `ch.ubique.alpaka.build.id`        | An ID for this build                                                     | `build_id` or `ubappid` Gradle property, defaults to `localbuild`           |
| `BUILD_NUMBER`    | `ch.ubique.alpaka.build.number`    | An incremental number of this build                                      | `build_number` Gradle property, defaults to `0`                             |
| `BUILD_TIMESTAMP` | `ch.ubique.alpaka.build.timestamp` | The timestamp of this build                                              | `build_timestamp` Gradle property, defaults to `System.currentTimeMillis()` |
| `BRANCH`          | `ch.ubique.alpaka.branch`          | The Git branch this build was created from                               | `branch` Gradle property, defaults to calling the systems Git command line  |
| `FLAVOR`          | `ch.ubique.alpaka.flavor`          | The product flavor this build was created from                           | Product flavor name of the variant that started the gradle task             |

## Development & Testing

To test any changes to this plugin locally, you can use the example apps shipped with this repository, 
or alternatively, deploy an artifact to your local maven repository and include that in an application of your choice:

1. Define a custom version by setting the `VERSION` in alpaka/gradle.properties*
2. You might need to disable `signAllPublications()`*
3. Deploy the plugin artifact by running `./gradlew publishToMavenLocal`
4. Reference `mavenLocal()` in your application's build script's dependency repository list.
5. Apply your local plugin version.

<sup>* do not accidentally commit these changes.</sup>

## Deployment

Create a [Release](https://github.com/UbiqueInnovation/gradle-plugin-alpaka-android/releases),
setting the Tag to the desired version prefixed with a `v`.

Each release on Github will be deployed to Maven Central.
