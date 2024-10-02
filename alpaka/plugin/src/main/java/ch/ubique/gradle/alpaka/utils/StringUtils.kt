package ch.ubique.gradle.alpaka.utils

import org.gradle.api.logging.Logger
import java.io.File

object StringUtils {

	/**
	 * Finds the app name from specified in the manifest.
	 */
	fun findAppName(logger: Logger, resDirs: List<File>, manifest: File): String? {
		val labelName = findAttributeValue(manifest, "application", "android:label")?.substringAfter("/") ?: return null

		val stringFiles = resDirs.filter { it.exists() }
			.flatMap { resDir ->
				resDir.walkTopDown()
					.maxDepth(1)
					.filter { it.isDirectory && it.name.equals("values") }
					.mapNotNull { dir ->
						dir.walkTopDown()
							.filter { it.isFile && it.name.matches(".*\\.xml".toRegex()) }
							.sortedByDescending { it.name.contains("strings") }
							.toList()
							.takeIf { it.isNotEmpty() }
					}.flatten()
			}

		if (stringFiles.isEmpty()) {
			logger.warn("No string files found in res directories")
			return null
		}

		logger.debug("Looking for $labelName in string files: ${stringFiles.joinToString { it.absolutePath }}")

		return stringFiles.firstNotNullOf { file ->
			val xmlParser = XmlParser(file)
			xmlParser.findTagValue("string", mapOf("name" to labelName))
				.takeIf { it.isNullOrEmpty().not() }
				?.trim('"') // Strip double quotes
		}
	}

	fun findRequiredFeatures(manifest: File): List<String> {
		if (manifest.isDirectory || manifest.exists().not()) {
			return emptyList()
		}

		val xmlParser = XmlParser(manifest)
		val features = xmlParser.findAttributeValues("uses-feature", "android:name", mapOf("android:required" to "true"))
		val openGl = xmlParser.findAttributeValues("uses-feature", "android:glEsVersion", mapOf("android:required" to "true")).map {
			when (it) {
				"0x00020000" -> "OpenGL 2.0"
				"0x00030002" -> "OpenGL 3.2"
				else -> "OpenGL $it"
			}
		}
		return features + openGl
	}

	fun findMetadataValue(manifest: File, name: String): String? {
		if (manifest.isDirectory || manifest.exists().not()) {
			return null
		}

		val xmlParser = XmlParser(manifest)
		return xmlParser.findAttributeValue("meta-data", "android:value", mapOf("android:name" to name))
	}

	private fun findAttributeValue(manifest: File, tag: String, attribute: String): String? {
		if (manifest.isDirectory || manifest.exists().not()) {
			return null
		}

		val xmlParser = XmlParser(manifest)
		return  xmlParser.findAttributeValue(tag, attribute)
	}

}
