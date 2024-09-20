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
					.filter { it.isDirectory && it.name.startsWith("values") }
					.mapNotNull { dir ->
						dir.walkTopDown()
							.filter { it.isFile && it.name.matches(".*strings.*\\.xml".toRegex()) }
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
			val appName = xmlParser.findAttribute("string", "name", labelName, findTextValue = true)
			if (appName != null) {
				logger.debug("Found app name: $appName")
				return appName.trim('"')
			}
		}

		return null
	}

	fun findRequiredFeatures(manifest: File): List<String> {
		if (manifest.isDirectory || manifest.exists().not()) {
			return emptyList()
		}

		val xmlParser = XmlParser(manifest)
		return xmlParser.findAttributeValues("uses-feature", "android:name", mapOf("android:required" to "true"))
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
