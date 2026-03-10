package com.arn.scrobble.utils

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.compose.resources.decodeToSvgPainter
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Converts an Android Vector Drawable XML InputStream into an SVG XML string.
 * Uses only javax.xml.
 */
private fun vectorDrawableToSvg(input: InputStream): String {
    val dbf = DocumentBuilderFactory.newInstance().also { it.isNamespaceAware = true }
    val vectorDoc = dbf.newDocumentBuilder().parse(input)
    val vectorRoot = vectorDoc.documentElement // <vector>

    val svgDoc = dbf.newDocumentBuilder().newDocument()
    val svgRoot = svgDoc.createElementNS("http://www.w3.org/2000/svg", "svg")
    svgDoc.appendChild(svgRoot)

    // Map viewport / dimensions
    val vpW = vectorRoot.getAttribute("android:viewportWidth").ifEmpty { "24" }
    val vpH = vectorRoot.getAttribute("android:viewportHeight").ifEmpty { "24" }
    val w = vectorRoot.getAttribute("android:width").replace("dp", "").ifEmpty { vpW }
    val h = vectorRoot.getAttribute("android:height").replace("dp", "").ifEmpty { vpH }
    svgRoot.setAttribute("xmlns", "http://www.w3.org/2000/svg")
    svgRoot.setAttribute("width", w)
    svgRoot.setAttribute("height", h)
    svgRoot.setAttribute("viewBox", "0 0 $vpW $vpH")

    val alpha = vectorRoot.getAttribute("android:alpha")
    if (alpha.isNotEmpty()) svgRoot.setAttribute("opacity", alpha)

    // Clip counter for unique IDs
    var clipIdCounter = 0

    fun convertChildren(vectorParent: Element, svgParent: Element) {
        val children = vectorParent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType != Node.ELEMENT_NODE) continue
            val el = child as Element

            when (el.tagName) {
                "path" -> {
                    val svgPath = svgDoc.createElement("path")
                    val d = el.getAttribute("android:pathData")
                    if (d.isNotEmpty()) svgPath.setAttribute("d", d)

                    val fill = el.getAttribute("android:fillColor")
                    svgPath.setAttribute("fill", fill.ifEmpty { "none" })

                    val stroke = el.getAttribute("android:strokeColor")
                    if (stroke.isNotEmpty()) svgPath.setAttribute("stroke", stroke)

                    val sw = el.getAttribute("android:strokeWidth")
                    if (sw.isNotEmpty()) svgPath.setAttribute("stroke-width", sw)

                    val slj = el.getAttribute("android:strokeLineJoin")
                    if (slj.isNotEmpty()) svgPath.setAttribute("stroke-linejoin", slj)

                    val slc = el.getAttribute("android:strokeLineCap")
                    if (slc.isNotEmpty()) svgPath.setAttribute("stroke-linecap", slc)

                    val sml = el.getAttribute("android:strokeMiterLimit")
                    if (sml.isNotEmpty()) svgPath.setAttribute("stroke-miterlimit", sml)

                    val pathAlpha = el.getAttribute("android:alpha")
                    if (pathAlpha.isNotEmpty()) svgPath.setAttribute("opacity", pathAlpha)

                    val fillRule = el.getAttribute("android:fillType")
                    if (fillRule.equals("evenOdd", ignoreCase = true))
                        svgPath.setAttribute("fill-rule", "evenodd")

                    svgParent.appendChild(svgPath)
                }

                "group" -> {
                    val svgG = svgDoc.createElement("g")

                    // Build transform from individual group attributes
                    val rotation = el.getAttribute("android:rotation").toFloatOrNull()
                    val pivotX = el.getAttribute("android:pivotX").toFloatOrNull() ?: 0f
                    val pivotY = el.getAttribute("android:pivotY").toFloatOrNull() ?: 0f
                    val scaleX = el.getAttribute("android:scaleX").toFloatOrNull()
                    val scaleY = el.getAttribute("android:scaleY").toFloatOrNull()
                    val translateX = el.getAttribute("android:translateX").toFloatOrNull()
                    val translateY = el.getAttribute("android:translateY").toFloatOrNull()

                    val transforms = buildList {
                        if (translateX != null || translateY != null)
                            add("translate(${translateX ?: 0f}, ${translateY ?: 0f})")
                        if (rotation != null)
                            add("rotate($rotation, $pivotX, $pivotY)")
                        if (scaleX != null || scaleY != null)
                            add("scale(${scaleX ?: 1f}, ${scaleY ?: 1f})")
                    }
                    if (transforms.isNotEmpty())
                        svgG.setAttribute("transform", transforms.joinToString(" "))

                    val groupAlpha = el.getAttribute("android:alpha")
                    if (groupAlpha.isNotEmpty()) svgG.setAttribute("opacity", groupAlpha)

                    // Handle clip-path children first
                    val clipPaths = (0 until el.childNodes.length)
                        .map { el.childNodes.item(it) }
                        .filterIsInstance<Element>()
                        .filter { it.tagName == "clip-path" }

                    for (cp in clipPaths) {
                        val clipId = "clip${clipIdCounter++}"
                        val svgClipPath = svgDoc.createElement("clipPath")
                        svgClipPath.setAttribute("id", clipId)
                        val cpPath = svgDoc.createElement("path")
                        cpPath.setAttribute("d", cp.getAttribute("android:pathData"))
                        svgClipPath.appendChild(cpPath)
                        svgG.appendChild(svgClipPath)
                        svgG.setAttribute("clip-path", "url(#$clipId)")
                    }

                    convertChildren(el, svgG)
                    svgParent.appendChild(svgG)
                }

                // clip-path at root level or already handled above — skip standalone ones
                "clip-path" -> { /* handled inside group */
                }
            }
        }
    }

    convertChildren(vectorRoot, svgRoot)

    // Serialize to string
    val transformer = TransformerFactory.newInstance().newTransformer().also {
        it.setOutputProperty(OutputKeys.INDENT, "yes")
        it.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    }
    val sw = StringWriter()
    transformer.transform(DOMSource(svgDoc), StreamResult(sw))
    return sw.toString()
}

fun vectorDrawableToImageBitmap(
    input: InputStream,
    widthPx: Int,
    heightPx: Int,
    darkTint: Boolean,
    density: Density = Density(1f),
): ImageBitmap {
    // Step 1: VectorDrawable XML → SVG XML string
    val svgString = vectorDrawableToSvg(input)

    // Step 2: SVG string → Painter (using Compose Desktop built-in)
    val painter = svgString.toByteArray().decodeToSvgPainter(density = density)

    // Step 3: Painter → ImageBitmap via CanvasDrawScope (no Compose context needed)
    return painter.toImageBitmap(
        Size(widthPx.toFloat(), heightPx.toFloat()),
        darkTint,
        density
    )
}

/**
 * Renders this Painter into a new ImageBitmap of the given [size].
 * Does NOT require a Compose composable/UI context.
 */
private fun Painter.toImageBitmap(
    size: Size,
    darkTint: Boolean,
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): ImageBitmap {
    val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
    val canvas = Canvas(bitmap)
    CanvasDrawScope().draw(density, layoutDirection, canvas, size) {
        draw(
            size,
            colorFilter = ColorFilter.tint(
                color = if (darkTint) Color.Black else Color.White,
                blendMode = BlendMode.SrcIn
            )
        )
    }
    return bitmap
}