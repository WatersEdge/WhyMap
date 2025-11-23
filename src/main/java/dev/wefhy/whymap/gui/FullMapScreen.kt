package dev.wefhy.whymap.gui

import com.mojang.blaze3d.systems.RenderSystem
import dev.wefhy.whymap.WhyMapMod.Companion.activeWorld
import dev.wefhy.whymap.config.WhyMapConfig
import dev.wefhy.whymap.config.WhyUserSettings
import dev.wefhy.whymap.utils.LocalTile
import dev.wefhy.whymap.utils.TileZoom
import kotlinx.coroutines.runBlocking
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class FullMapScreen : Screen(Text.of("WhyMap")) {

    private var centerX = 0.0
    private var centerZ = 0.0
    private var pixelsPerBlock = WhyUserSettings.mapSettings.mapScale.toFloat()
    private var dragging = false

    private val regionTextures = mutableMapOf<LocalTile<TileZoom.RegionZoom>, NativeImageBackedTexture>()

    override fun init() {
        client?.player?.let {
            centerX = it.x
            centerZ = it.z
        }
    }

    override fun shouldPause() = false

    override fun close() {
        regionTextures.values.forEach { it.close() }
        regionTextures.clear()
        super.close()
    }

    override fun render(drawContext: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(drawContext, mouseX, mouseY, delta)
        drawContext.fill(0, 0, width, height, 0x88000000.toInt())
        if (!drawMap(drawContext)) {
            drawContext.drawCenteredTextWithShadow(textRenderer, Text.of("Map unavailable"), width / 2, height / 2, 0xFFFFFF)
        }
        drawOverlay(drawContext)
    }

    private fun drawMap(drawContext: DrawContext): Boolean {
        val mc = client ?: return false
        val world = activeWorld ?: return false
        val viewCenterBlock = LocalTile.Block(floor(centerX).toInt(), floor(centerZ).toInt())
        val regionSize = WhyMapConfig.storageTileBlocks
        val regionPixelSize = regionSize * pixelsPerBlock
        if (regionPixelSize <= 1f) return false

        val regionRadiusX = max(1, ceil(width / regionPixelSize).toInt() / 2 + 1)
        val regionRadiusZ = max(1, ceil(height / regionPixelSize).toInt() / 2 + 1)
        val centerRegion = viewCenterBlock.parent(TileZoom.RegionZoom)
        val screenCenterX = width / 2f
        val screenCenterY = height / 2f

        val offsets = (-regionRadiusX..regionRadiusX).flatMap { dx ->
            (-regionRadiusZ..regionRadiusZ).map { dz -> dx to dz }
        }

        offsets.forEach { (dx, dz) ->
            val region = LocalTile.Region(centerRegion.x + dx, centerRegion.z + dz)
            val rendered = runBlocking {
                world.mapRegionManager.getRegionForMinimapRendering(region) { renderWhyImageBuffered() }
            } ?: return@forEach

            val nativeImage = rendered.toNativeImage()
            val texture = regionTextures.getOrPut(region) { NativeImageBackedTexture(nativeImage) }
            if (texture.image != nativeImage) {
                texture.image = nativeImage
            }
            texture.upload()

            val textureId = mc.textureManager.registerDynamicTexture("whymap_full_${region.x}_${region.z}", texture)
            val start = region.getStart()
            val diffX = start.x - viewCenterBlock.x
            val diffZ = start.z - viewCenterBlock.z
            val drawX = screenCenterX + diffX * pixelsPerBlock
            val drawY = screenCenterY + diffZ * pixelsPerBlock

            drawContext.matrices.push()
            drawContext.matrices.translate(drawX.toDouble(), drawY.toDouble(), 0.0)
            drawTexture(drawContext.matrices, textureId, regionPixelSize, regionPixelSize)
            drawContext.matrices.pop()
        }
        return true
    }

    private fun drawOverlay(drawContext: DrawContext) {
        val mc = client ?: return
        val text = textRenderer
        val coords = "Center: ${centerX.toInt()}, ${centerZ.toInt()}  Zoom: ${"%.2f".format(pixelsPerBlock)} px/block"
        drawContext.drawText(text, coords, 10, 10, 0xFFFFFF, true)

        mc.player?.let { player ->
            val (px, pz) = blockToScreen(player.x, player.z)
            drawPlayerArrow(drawContext, px, pz, player.yaw)
        }

        val waypoints = activeWorld?.waypoints?.onlineWaypoints ?: emptyList()
        waypoints.forEach { wp ->
            val (wx, wz) = blockToScreen(wp.pos.x.toDouble(), wp.pos.z.toDouble())
            val color = parseColor(wp.color)
            drawContext.fill((wx - 2).toInt(), (wz - 2).toInt(), (wx + 2).toInt(), (wz + 2).toInt(), color)
            drawContext.drawText(text, wp.name, (wx + 4).toInt(), (wz - 4).toInt(), color, false)
        }

        val help = "Drag to pan | Scroll to zoom | Esc to close"
        drawContext.drawText(text, help, 10, height - 20, 0xAAAAAA, true)
    }

    private fun blockToScreen(blockX: Double, blockZ: Double): Pair<Float, Float> {
        val dx = (blockX - centerX) * pixelsPerBlock
        val dz = (blockZ - centerZ) * pixelsPerBlock
        val screenCenterX = width / 2f
        val screenCenterY = height / 2f
        return (screenCenterX + dx.toFloat()) to (screenCenterY + dz.toFloat())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            dragging = true
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            dragging = false
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (dragging && button == 0) {
            centerX -= deltaX / pixelsPerBlock
            centerZ -= deltaY / pixelsPerBlock
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val factor = if (verticalAmount > 0) 1.1f else 0.9f
        pixelsPerBlock = (pixelsPerBlock * factor).coerceIn(0.4f, 3.5f)
        return true
    }

    private fun parseColor(value: String?): Int {
        val hex = value?.removePrefix("#")
        val rgb = hex?.toIntOrNull(16) ?: 0xFF5555
        return 0xFF000000.toInt() or rgb
    }

    private fun drawTexture(matrixStack: MatrixStack, textureId: Identifier, width: Float, height: Float) {
        drawTexture(matrixStack, textureId, width, height, 0f, 0f)
    }

    private fun drawTexture(matrixStack: MatrixStack, textureId: Identifier, width: Float, height: Float, xOffset: Float, yOffset: Float) {
        val positionMatrix = matrixStack.peek().positionMatrix
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buffer.vertex(positionMatrix, xOffset, yOffset, 0f).color(1f, 1f, 1f, 1f).texture(0f, 0f)
        buffer.vertex(positionMatrix, xOffset, yOffset + height, 0f).color(1f, 1f, 1f, 1f).texture(0f, 1f)
        buffer.vertex(positionMatrix, xOffset + width, yOffset + height, 0f).color(1f, 1f, 1f, 1f).texture(1f, 1f)
        buffer.vertex(positionMatrix, xOffset + width, yOffset, 0f).color(1f, 1f, 1f, 1f).texture(1f, 0f)
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram)
        RenderSystem.setShaderTexture(0, textureId)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
    }

    private fun drawPlayerArrow(drawContext: DrawContext, x: Float, y: Float, yaw: Float) {
        val size = 6f
        val matrixStack = drawContext.matrices
        matrixStack.push()
        matrixStack.translate(x.toDouble(), y.toDouble(), 0.0)
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-(yaw)))

        val m = matrixStack.peek().positionMatrix
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR)
        buffer.vertex(m, 0f, -size, 0f).color(0f, 1f, 0f, 1f)
        buffer.vertex(m, size, size, 0f).color(0f, 1f, 0f, 1f)
        buffer.vertex(m, -size, size, 0f).color(0f, 1f, 0f, 1f)
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
        matrixStack.pop()
    }
}
