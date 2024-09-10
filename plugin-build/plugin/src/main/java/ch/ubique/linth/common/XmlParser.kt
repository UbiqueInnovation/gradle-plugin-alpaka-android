package ch.ubique.linth.common

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

	fun findAttribute(tag: String, attrib: String, matchingAttribValue: String? = null, findTextValue: Boolean = false): String? {
		val nodeList = root.childNodes
		for (i in 0 until nodeList.length) {
			val node = nodeList.item(i)

			if (node.nodeType == Document.ELEMENT_NODE) {
				val element = node as Element
				if (element.tagName == tag) {
					// Access attributes
					val attributes = element.attributes
					for (j in 0 until attributes.length) {
						val attribute = attributes.item(j)
						if (attribute.nodeName == attrib) {
							if (matchingAttribValue != null) {
								if (attribute.nodeValue == matchingAttribValue) {
									return if (findTextValue) {
										element.firstChild.textContent
									} else {
										attribute.nodeValue
									}
								}
							} else {
								return if (findTextValue) {
									element.firstChild.textContent
								} else {
									attribute.nodeValue
								}
							}
						}
					}
					return null
				}
			}
		}
		return null
	}

	/**
	 * Finds the value of [attributeName] for a [tag], where the tag also matches the optional [attributeFilter].
	 *
	 * @param tag The tag to which the attribute belongs
	 * @param attributeName The name of the attribute to find
	 * @param attributeFilter A map of attributes that must match on the tag
	 */
	fun findAttributeValue(tag: String, attributeName: String, attributeFilter: Map<String, String> = emptyMap()): String? {
		return root.findAttributeValue(tag, attributeName, attributeFilter)
	}

	private fun Element.findAttributeValue(tag: String, attributeName: String, attributeFilter: Map<String, String>): String? {
		val nodeList = this.childNodes
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
						return attributeValue
					}
				} else {
					val value = element.findAttributeValue(tag, attributeName, attributeFilter)
					if (value != null) {
						return value
					}
				}
			}
		}
		return null
	}

	private fun parseXml(file: File): Document {
		val documentBuilderFactory = DocumentBuilderFactory.newInstance()
		val docBuilder = documentBuilderFactory.newDocumentBuilder()
		return docBuilder.parse(file)
	}
}