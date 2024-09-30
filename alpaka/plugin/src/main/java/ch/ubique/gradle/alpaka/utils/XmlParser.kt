package ch.ubique.gradle.alpaka.utils

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class XmlParser(
	file: File,
) {

	private val root: Element

	init {
		val document = parseXml(file)
		document.documentElement.normalize()
		root = document.documentElement
	}

	/**
	 * Finds the value of a [tag], where the tag also matches the optional [attributeFilter].
	 *
	 * @param tag The tag to find
	 * @param attributeFilter A map of attributes that must match on the tag
	 *
	 * @return The value of the single matching tag or null if none or more than one matches
	 */
	fun findTagValue(tag: String, attributeFilter: Map<String, String> = emptyMap()): String? {
		return root.findTagValues(tag, attributeFilter).singleOrNull()
	}

	/**
	 * Finds all values of a [tag], where the tag also matches the optional [attributeFilter].
	 *
	 * @param tag The tag to find
	 * @param attributeFilter A map of attributes that must match on the tag
	 *
	 * @return The values of all matching tags
	 */
	fun findTagValues(tag: String, attributeFilter: Map<String, String> = emptyMap()): List<String> {
		return root.findTagValues(tag, attributeFilter)
	}

	private fun Element.findTagValues(tag: String, attributeFilter: Map<String, String>): List<String> {
		val nodeList = this.childNodes
		val values = mutableListOf<String>()
		for (i in 0 until nodeList.length) {
			val node = nodeList.item(i)

			if (node.nodeType == Document.ELEMENT_NODE) {
				val element = node as Element
				if (element.tagName == tag) {
					// Access attributes
					val attributes = element.attributes
					var matchesAttributeFilter = true

					for (j in 0 until attributes.length) {
						val attribute = attributes.item(j)

						if (attribute.nodeName in attributeFilter) {
							val matches = attribute.nodeValue == attributeFilter.getValue(attribute.nodeName)
							matchesAttributeFilter = matchesAttributeFilter && matches
						}
					}

					if (matchesAttributeFilter) {
						values.add(element.textContent)
					}
				} else {
					val nestedValues = element.findTagValues(tag, attributeFilter)
					values.addAll(nestedValues)
				}
			}
		}
		return values

	}

	/**
	 * Finds the value of [attributeName] for a [tag], where the tag also matches the optional [attributeFilter].
	 *
	 * @param tag The tag to which the attribute belongs
	 * @param attributeName The name of the attribute to find
	 * @param attributeFilter A map of attributes that must match on the tag
	 *
	 * @return The attribute value of the single matching tag or null if none or more than one matches
	 */
	fun findAttributeValue(tag: String, attributeName: String, attributeFilter: Map<String, String> = emptyMap()): String? {
		return root.findAttributeValues(tag, attributeName, attributeFilter).singleOrNull()
	}

	/**
	 * Finds all values of [attributeName] for a [tag], where the tag also matches the optional [attributeFilter].
	 *
	 * @param tag The tag to which the attribute belongs
	 * @param attributeName The name of the attribute to find
	 * @param attributeFilter A map of attributes that must match on the tag
	 *
	 * @return The attribute values of all matching tags
	 */
	fun findAttributeValues(tag: String, attributeName: String, attributeFilter: Map<String, String> = emptyMap()): List<String> {
		return root.findAttributeValues(tag, attributeName, attributeFilter)
	}

	private fun Element.findAttributeValues(
		tag: String,
		attributeName: String,
		attributeFilter: Map<String, String>,
	): List<String> {
		val nodeList = this.childNodes
		val values = mutableListOf<String>()
		for (i in 0 until nodeList.length) {
			val node = nodeList.item(i)

			if (node.nodeType == Document.ELEMENT_NODE) {
				val element = node as Element
				if (element.tagName == tag) {
					// Access attributes
					val attributes = element.attributes
					var attributeValue: String? = null
					var matchesAttributeFilter = true

					for (j in 0 until attributes.length) {
						val attribute = attributes.item(j)

						if (attribute.nodeName == attributeName) {
							attributeValue = attribute.nodeValue
						} else if (attribute.nodeName in attributeFilter) {
							val matches = attribute.nodeValue == attributeFilter.getValue(attribute.nodeName)
							matchesAttributeFilter = matchesAttributeFilter && matches
						}
					}

					if (matchesAttributeFilter && attributeValue != null) {
						values.add(attributeValue)
					}
				} else {
					val nestedValues = element.findAttributeValues(tag, attributeName, attributeFilter)
					values.addAll(nestedValues)
				}
			}
		}
		return values
	}

	private fun parseXml(file: File): Document {
		val documentBuilderFactory = DocumentBuilderFactory.newInstance()
		val docBuilder = documentBuilderFactory.newDocumentBuilder()
		return docBuilder.parse(file)
	}
}