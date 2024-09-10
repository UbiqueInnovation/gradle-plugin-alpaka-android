package ch.ubique.linth.common

import java.io.File

object StringUtils {

	/**
	 * Finds the app name from specified in the manifest.
	 */
	fun findAppName(resDirs: List<File>, manifest: File): String? {
		val labelName = findAttribute(manifest, "application", "android:label")?.substringAfter("/") ?: return null

		val stringFiles = mutableListOf<File>()
		for (resDir in resDirs) {
			if (resDir.exists()) {
				resDir.walkTopDown().maxDepth(1)
					.filter { it.isDirectory && (it.name.startsWith("values")) }
					.forEach { dir ->
						dir.walkTopDown().filter { it.isFile && it.name.matches(Regex(".*strings.*.xml")) }
							.forEach { stringFiles.add(it) }
					}
			}
		}
		if (stringFiles.isEmpty()) return null

		stringFiles.forEach {
			val xmlParser = XmlParser(it)
			val appName = xmlParser.findAttribute("string", "name", labelName, findTextValue = true)
			if (appName != null) return appName
		}

		return null
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
