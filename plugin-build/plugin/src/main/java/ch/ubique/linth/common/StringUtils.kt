package ch.ubique.linth.common

import java.io.File

object StringUtils {

	private fun getLabelName(manifestFile: File): String? {
		if (manifestFile.isDirectory || manifestFile.exists().not()) {
			return null
		}

		val xmlParser = XmlParser(manifestFile)
		val fileName = xmlParser.findAttribute("application", "android:label")

		return fileName?.split("/")?.get(1)
	}

	/**
	 * Finds all icon files matching the icon specified in the given manifest.
	 */
	fun findAppName(resDirs: List<File>, manifest: File): String? {
		val labelName = getLabelName(manifest) ?: return null

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

}
