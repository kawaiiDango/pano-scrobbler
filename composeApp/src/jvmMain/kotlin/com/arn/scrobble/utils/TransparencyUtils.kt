package com.arn.scrobble.utils


import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import org.jetbrains.skiko.SkiaLayer
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Window
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLayeredPane
import javax.swing.JWindow


/*
 * Copyright 2022-2025 MayakaApps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// forked from https://github.com/MayakaApps/ComposeWindowStyler/blob/main/window-styler/src/jvmMain/kotlin/com/mayakapps/compose/windowstyler/TransparencyUtils.kt

val Window.isUndecorated: Boolean
    get() = when (this) {
        is ComposeWindow -> isUndecorated
        is ComposeDialog -> isUndecorated
        else -> throw IllegalArgumentException(
            "Unsupported window type: ${this::class.simpleName}"
        )
    }


fun ComposeWindow.findSkiaLayer(): SkiaLayer? = findComponent<SkiaLayer>()


fun ComposeDialog.findSkiaLayer(): SkiaLayer? = findComponent<SkiaLayer>()

fun Window.findSkiaLayer(): SkiaLayer? = when (this) {
    is ComposeWindow -> findSkiaLayer()
    is ComposeDialog -> findSkiaLayer()
    else -> throw IllegalArgumentException(
        "Unsupported window type: ${this::class.simpleName}"
    )
}

fun Window.hackContentPane() {
    val oldContentPane = contentPane ?: return

    // Create hacked content pane the same way of AWT
    val newContentPane: JComponent = HackedContentPane()
    newContentPane.name = "$name.contentPane"
    newContentPane.layout = object : BorderLayout() {
        override fun addLayoutComponent(comp: Component, constraints: Any?) {
            super.addLayoutComponent(comp, constraints ?: CENTER)
        }
    }

    newContentPane.background = Color(0, 0, 0, 0)
    newContentPane.isOpaque = false
    newContentPane.size = oldContentPane.size

    newContentPane.enableInputMethods(true)
    oldContentPane.components.forEach { component ->
        newContentPane.add(component)
    }

    contentPane = newContentPane
}

private class HackedContentPane : JLayeredPane() {
    override fun paint(g: Graphics) {
        if (background.alpha != 255) {
            val gg = g.create()
            try {
                if (gg is Graphics2D) {
                    gg.color = background
                    gg.composite = AlphaComposite.getInstance(AlphaComposite.SRC)
                    gg.fillRect(0, 0, width, height)
                }
            } finally {
                gg.dispose()
            }
        }
        super.paint(g)
    }
}

// Try hard to get the contentPane.
private var Window.contentPane
    get() = when (this) {
        is JFrame -> contentPane
        is JDialog -> contentPane
        is JWindow -> contentPane
        else -> null
    }
    set(value) = when (this) {
        is JFrame -> contentPane = value
        is JDialog -> contentPane = value
        is JWindow -> contentPane = value
        else -> throw IllegalStateException()
    }


private fun <T : JComponent> findComponent(container: Container, klass: Class<T>): T? {
    for (component in container.components) {
        if (klass.isInstance(component)) {
            @Suppress("UNCHECKED_CAST")
            return component as T
        }
        if (component is Container) {
            val found = findComponent(component, klass)
            if (found != null) {
                return found
            }
        }
    }
    return null
}

private inline fun <reified T : JComponent> Container.findComponent(): T? =
    findComponent(this, T::class.java)