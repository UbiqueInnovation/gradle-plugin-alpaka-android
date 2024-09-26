package ch.ubique.gradle.linth.utils

import org.gradle.api.logging.Logger
import java.io.File

object StringUtils {

	/**
	 * Finds the app name from specified in the manifest.
	 */
	fun findAppName(logger: Logger, resDirs: List<File>, manifest: File): String? {
		val labelName = findAttribute(manifest, "application", "android:label")?.substringAfter("/") ?: return null

		val stringFiles = resDirs.filter { it.exists() }
			.flatMap { resDir ->
				resDir.walkTopDown()
					.maxDepth(1)
					.filter { it.isDirectory && it.name.equals("values") }
					.mapNotNull { dir ->
						dir.walkTopDown()
							.filter { it.isFile && it.name.matches(".*\\.xml".toRegex()) }
							.toList()
							.takeIf { it.isNotEmpty() }
					}.flatten()
			}

		if (stringFiles.isEmpty()) {
			logger.warn("No string files found in res directories")
			return null
		}

		logger.debug("Looking for app name in string files: ${stringFiles.joinToString { it.absolutePath }}")

		stringFiles.forEach {
			val xmlParser = XmlParser(it)
			val appName = xmlParser.findAttribute("string", "name", labelName, findTextValue = true)?.trim('"')
			if (appName.isNullOrEmpty().not()) {
				logger.debug("Found app name: $appName")
				return appName
			}
		}

		return null
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

	private fun findAttribute(manifest: File, tag: String, attribute: String): String? {
		if (manifest.isDirectory || manifest.exists().not()) {
			return null
		}

		val xmlParser = XmlParser(manifest)
		return  xmlParser.findAttribute(tag, attribute)
	}

}
