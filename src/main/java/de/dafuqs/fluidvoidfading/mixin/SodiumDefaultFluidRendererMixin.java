/* Adapted for NeoForge from https://github.com/DaFuqs/fluidvoidfading/blob/1.21/src/main/java/de/dafuqs/fluidvoidfading/mixin/client/SodiumFluidRendererMixin.java
 * Licensed under GPL-3.0
 * 06/06/2025, AnAwesomGuy
 */

package de.dafuqs.fluidvoidfading.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.util.DirectionUtil;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public abstract class SodiumDefaultFluidRendererMixin {
    @Shadow
    public static final float EPSILON = 0.001F;

    @Shadow
    private static void setVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
    }

    @Shadow
    @Final
    private BlockPos.MutableBlockPos scratchPos;

    @Shadow
    @Final
    private ModelQuadViewMutable quad;

    @Shadow
    @Final
    private int[] quadColors;

    @Shadow
    @Final
    private LightPipelineProvider lighters;

    @Shadow
    @Final
    private float[] brightness;

    @Shadow
    @Final
    private QuadLightData quadLightData;

    @Shadow
    protected abstract void writeQuad(ChunkModelBuilder builder, TranslucentGeometryCollector collector, Material material, BlockPos offset, ModelQuadView quad, ModelQuadFacing facing, boolean flip);

    @Inject(method = "render", at = @At("RETURN"))
    public void renderVoidFade(LevelSlice levelSlice, BlockState state, FluidState fluid, BlockPos pos, BlockPos offset, TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder, Material material, ColorProvider<FluidState> colorProvider, TextureAtlasSprite[] sprites, CallbackInfo ci) {
        @SuppressWarnings("UnnecessaryLocalVariable") // stupid workaround to compile
        BlockAndTintGetter level = levelSlice;
        if (pos.getY() == level.getMinBuildHeight())
            fluidvoidfading$renderFluidInVoid(level, fluid, pos, offset, collector, meshBuilder, material, colorProvider, sprites);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;isFullBlockFluidOccluded(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Z", ordinal = 1))
    public boolean cullDownAtVoid(boolean original, LevelSlice levelSlice, BlockState state, FluidState fluid, BlockPos pos, BlockPos offset, TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder, Material material, ColorProvider<FluidState> colorProvider, TextureAtlasSprite[] sprites) {
        //noinspection RedundantCast
        return original || pos.getY() == ((BlockAndTintGetter) levelSlice).getMinBuildHeight();
    }

    @Inject(method = "isSideExposed", at = @At("HEAD"), cancellable = true)
    private void isSideExposed(BlockAndTintGetter level, int x, int y, int z, Direction dir, float height, CallbackInfoReturnable<Boolean> cir) {
        if (dir == Direction.DOWN && y == level.getMaxBuildHeight())
            cir.setReturnValue(false);
    }

    @Unique
    private void fluidvoidfading$renderFluidInVoid(BlockAndTintGetter level, FluidState fluid, BlockPos pos, BlockPos offset, TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder, Material material, ColorProvider<FluidState> colorProvider, TextureAtlasSprite[] sprites) {
        boolean isWater = fluid.is(FluidTags.WATER);

        final ModelQuadViewMutable quad = this.quad;

        LightMode lightMode = isWater && Minecraft.useAmbientOcclusion() ? LightMode.SMOOTH : LightMode.FLAT;
        LightPipeline lighter = this.lighters.getLighter(lightMode);

        quad.setFlags(ModelQuadFlags.IS_PARALLEL | ModelQuadFlags.IS_ALIGNED);
        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            BlockState adjBlock = level.getBlockState(this.scratchPos.setWithOffset(pos, dir));
            if (!adjBlock.getFluidState().isEmpty()) continue;
            float x1;
            float z1;
            float x2;
            float z2;

            if (dir == Direction.NORTH) {
                x1 = 0F;
                x2 = 1F;
                z1 = EPSILON;
                z2 = z1;
            } else if (dir == Direction.SOUTH) {
                x1 = 1F;
                x2 = 0F;
                z1 = 1F - EPSILON;
                z2 = z1;
            } else if (dir == Direction.WEST) {
                x1 = EPSILON;
                x2 = x1;
                z1 = 1F;
                z2 = 0F;
            } else if (dir == Direction.EAST) {
                x1 = 1F - EPSILON;
                x2 = x1;
                z1 = 0F;
                z2 = 1F;
            } else continue;

            TextureAtlasSprite sprite = sprites[1];

            boolean isOverlay = false;

            if (sprites.length > 2 && sprites[2] != null && PlatformBlockAccess.getInstance().shouldShowFluidOverlay(adjBlock, level, this.scratchPos, fluid)) {
                sprite = sprites[2];
                isOverlay = true;
            }

            float u1 = sprite.getU(0F);
            float u2 = sprite.getU(0.5F);
            float v1 = sprite.getV(0F);
            float v2 = sprite.getV(0.5F);

            quad.setSprite(sprite);

            setVertex(quad, 0, x2, 1F, z2, u2, v1);
            setVertex(quad, 1, x2, EPSILON, z2, u2, v2);
            setVertex(quad, 2, x1, EPSILON, z1, u1, v2);
            setVertex(quad, 3, x1, 1F, z1, u1, v1);
            float br = dir.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;

            ModelQuadFacing facing = ModelQuadFacing.fromDirection(dir);

            lighter.calculate(quad, pos, this.quadLightData, null, dir, false, false);
            colorProvider.getColors((LevelSlice) level, pos, this.scratchPos, fluid, quad, this.quadColors);

            int[] original = new int[]{ColorARGB.toABGR(this.quadColors[0]), ColorARGB.toABGR(this.quadColors[1]),
                    ColorARGB.toABGR(this.quadColors[2]), ColorARGB.toABGR(this.quadColors[3])};

            BlockPos downPos1 = offset.below();
            this.fluidvoidfading$updateQuadWithAlpha(quad, facing, br, original, 1F, 0.3F);
            this.writeQuad(meshBuilder, collector, material, downPos1, quad, facing, false);
            if (!isOverlay)
                this.writeQuad(meshBuilder, collector, material, downPos1, quad, facing.getOpposite(), true);

            BlockPos downPos2 = offset.below(2);
            this.fluidvoidfading$updateQuadWithAlpha(quad, facing, br, original, 0.3F, 0F);
            this.writeQuad(meshBuilder, collector, material, downPos2, quad, facing, false);
            if (!isOverlay)
                this.writeQuad(meshBuilder, collector, material, downPos2, quad, facing.getOpposite(), true);
        }
    }

    @Unique
    private void fluidvoidfading$updateQuadWithAlpha(ModelQuadViewMutable quad, ModelQuadFacing facing, float brightness, int[] originalColors, float alphaStart, float alphaEnd) {
        quad.setFaceNormal(facing.isAligned() ? facing.getPackedAlignedNormal() : quad.calculateNormal());
        {
            int original = originalColors[0];
            this.quadColors[0] = ColorABGR.withAlpha(original, alphaStart * ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(original)));
            this.brightness[0] = this.quadLightData.br[0] * brightness;
        }
        {
            int original = originalColors[1];
            this.quadColors[1] = ColorABGR.withAlpha(original, alphaEnd * ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(original)));
            this.brightness[1] = this.quadLightData.br[1] * brightness;
        }
        {
            int original = originalColors[2];
            this.quadColors[2] = ColorABGR.withAlpha(original, alphaEnd * ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(original)));
            this.brightness[2] = this.quadLightData.br[2] * brightness;
        }
        {
            int original = originalColors[3];
            this.quadColors[3] = ColorABGR.withAlpha(original, alphaStart * ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(original)));
            this.brightness[3] = this.quadLightData.br[3] * brightness;
        }
    }
}