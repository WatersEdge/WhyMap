package dev.wefhy.whymap.utils

import net.minecraft.client.texture.NativeImage
import java.awt.image.BufferedImage

// 1.21.1 API compatibility helpers for older helper names
fun NativeImage.setColorArgb(x: Int, y: Int, color: Int) = setColor(x, y, color)
fun NativeImage.getColorArgb(x: Int, y: Int): Int = getColor(x, y)
fun BufferedImage.getColorArgb(x: Int, y: Int): Int = getRGB(x, y)
