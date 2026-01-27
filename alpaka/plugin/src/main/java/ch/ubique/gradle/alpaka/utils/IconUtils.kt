package ch.ubique.gradle.alpaka.utils

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

internal object IconUtils {

	private const val DEFAULT_ICON_NAME = "ic_launcher"

	/**
	 * Retrieve the app icon from the application manifest
	 */
	private fun getIconName(manifestFile: File): String? {
		if (manifestFile.isDirectory || manifestFile.exists().not()) {
			return null
		}

		val xmlParser = XmlParser(manifestFile)
		val fileName = xmlParser.findAttributeValue("application", "android:icon")

		return fileName?.split("/")?.get(1)
	}

	/**
	 * Finds all icon files matching the icon specified in the given manifest.
	 */
	fun findIcons(resDirs: List<File>, manifests: List<File>): List<File> {
		val localIconName = manifests.firstNotNullOfOrNull { getIconName(it) }
		val iconName = localIconName ?: DEFAULT_ICON_NAME

		for (resDir in resDirs) {
			if (resDir.exists()) {
				val result = mutableListOf<File>()
				resDir.walkTopDown().maxDepth(1)
					.filter { it.isDirectory && (it.name.startsWith("drawable") || it.name.startsWith("mipmap")) }
					.forEach { dir ->
						dir.walkTopDown()
							.filter { it.isFile && it.name.matches(Regex(".*$iconName.(png|webp)")) }
							.forEach { result.add(it) }
						dir.walkTopDown()
							.filter { it.isFile && it.name.matches(Regex(".*${iconName}_(foreground|monochrome).(png|webp|xml)")) }
							.forEach { result.add(it) }
					}
				if (result.isNotEmpty()) return result
			}
		}

		return emptyList()
	}

	/**
	 * Creates a layered drawable putting the label banner over the launcher icon.
	 */
	fun createLayeredLabel(iconFile: File, bannerLabel: String, adaptive: Boolean, monochrome: Boolean) {
		val iconName = iconFile.name.substringBefore(".")
		val iconExt = iconFile.name.substringAfter(".")
		val iconNameOverlay = "${iconName}_overlay"
		val iconOverlayFile = File(iconFile.parentFile, "$iconNameOverlay.png")
		iconOverlayFile.delete()

		val iconNameOriginal = "${iconName}_original"
		val iconOriginalFile = File(iconFile.parentFile, "$iconNameOriginal.$iconExt")
		iconOriginalFile.delete()

		val resType = if (iconFile.parentFile.name.startsWith("mipmap")) "mipmap" else "drawable"

		// Create upper layer, transparent image with the same size as iconFile
		val (sourceWidth, sourceHeight) = if (iconExt.equals("xml", ignoreCase = true)) {
			Pair(512, 512)
		} else {
			val img = ImageIO.read(iconFile)
			Pair(img.width, img.height)
		}
		val overlayBitmap = createTransparentImage(sourceWidth, sourceHeight)

		// Draw label to upper layer
		drawLabelOnImage(overlayBitmap, bannerLabel, adaptive, monochrome)
		ImageIO.write(overlayBitmap, "png", iconOverlayFile)

		// Move iconFile to iconFile-lower-layer
		iconFile.renameTo(iconOriginalFile)

		// Save layer list XML into iconFile
		val layerListFile = File(iconFile.parentFile, "$iconName.xml")
		val layerListXml = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<!-- GENERATED FILE - DO NOT EDIT -->
            |<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
            |    <item android:drawable="@${resType}/${iconNameOriginal}" />
            |    <item android:drawable="@${resType}/${iconNameOverlay}" />
            |</layer-list>
        """.trimMargin()

		layerListFile.writeText(layerListXml)
	}

	fun drawLabel(sourceFile: File, targetFile: File, label: String, adaptive: Boolean, monochrome: Boolean) {
		val img = ImageIO.read(sourceFile)
		drawLabelOnImage(img, label, adaptive, monochrome)
		val fileExtension = sourceFile.extension
		ImageIO.write(img, fileExtension, targetFile)
	}

	private fun drawLabelOnImage(img: BufferedImage, label: String, adaptive: Boolean, monochrome: Boolean) {
		val sourceWidth = img.width
		val sourceHeight = img.height
		val dp = img.width / 108.0

		val g = GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(img)
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

		val scale = if (adaptive) 1.0 else 1.5

		val anchorRel = if (adaptive) 0.6 else 0.65
		val anchorX = (sourceWidth * anchorRel).toInt()
		val anchorY = (sourceHeight * anchorRel).toInt()

		val bannerTransform = AffineTransform()
		bannerTransform.rotate(Math.toRadians(-45.0), anchorX.toDouble(), anchorY.toDouble())
		g.transform = bannerTransform

		val bannerHeight = (scale * sourceHeight / 5).toInt()
		val banner = Rectangle(anchorX - sourceWidth, anchorY - bannerHeight / 2, sourceWidth * 2, bannerHeight)

		// Draw banner shadow
		if (!monochrome) {
			val shadow1 = Rectangle(banner).apply { grow(0, (scale * 0.5 * dp).toInt()) }
			g.color = Color(0, 0, 0, 58)
			g.fill(shadow1)

			val shadow2 = Rectangle(banner).apply { size = Dimension(width, (height + scale * dp).toInt()) }
			g.fill(shadow2)
		}

		// Draw banner
		g.color = if (monochrome) Color(255, 255, 255, 128) else Color.WHITE
		g.fill(banner)

		// Set font and calculate size
		val labelFontSize = (scale * sourceHeight / 7).toInt()
		val labelFont = Font(Font.SANS_SERIF, Font.PLAIN, labelFontSize)
		g.font = labelFont
		val fontMetrics = g.fontMetrics
		val labelHeight = fontMetrics.ascent - fontMetrics.descent
		val labelWidth = fontMetrics.stringWidth(label.uppercase())

		g.color = if (monochrome) Color.BLACK else Color.decode("#273c56")
		g.drawString(label.uppercase(), anchorX - labelWidth / 2, anchorY + labelHeight / 2)
	}

	private fun createTransparentImage(width: Int, height: Int): BufferedImage {
		val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
		val g = GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(img)
		g.background = Color(0, true)
		g.clearRect(0, 0, width, height)
		g.dispose()
		return img
	}
}
