package ch.ubique.gradle.alpaka.extensions.android

import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import java.io.File

internal fun Artifacts.getApk(): Provider<File> {
	return get(SingleArtifact.APK).map { dir ->
		dir.asFile.listFiles()
			?.singleOrNull { it.extension == "apk" }
			?: throw GradleException("No single APK found in ${dir.asFile}")
	}
}
