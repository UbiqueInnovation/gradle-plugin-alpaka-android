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

	private fun parseXml(file: File): Document {
		val documentBuilderFactory = DocumentBuilderFactory.newInstance()
		val docBuilder = documentBuilderFactory.newDocumentBuilder()
		return docBuilder.parse(file)
	}
}