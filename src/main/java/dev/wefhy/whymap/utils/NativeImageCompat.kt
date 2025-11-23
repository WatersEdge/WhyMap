package dev.wefhy.whymap.utils

import net.minecraft.client.texture.NativeImage
import java.awt.image.BufferedImage

// NativeImage uses ABGR; convert to/from ARGB helpers
private fun Int.argbToAbgr(): Int {
    val a = this and -0x1000000
    val r = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val b = this and 0xFF
    return a or (b shl 16) or (g shl 8) or r
}

private fun Int.abgrToArgb(): Int {
    val a = this and -0x1000000
    val b = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val r = this and 0xFF
    return a or (r shl 16) or (g shl 8) or b
}

fun NativeImage.setColorArgb(x: Int, y: Int, color: Int) = setColor(x, y, color.argbToAbgr())
fun NativeImage.getColorArgb(x: Int, y: Int): Int = getColor(x, y).abgrToArgb()
fun BufferedImage.getColorArgb(x: Int, y: Int): Int = getRGB(x, y)
