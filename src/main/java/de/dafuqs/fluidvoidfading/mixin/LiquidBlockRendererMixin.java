/* Adapted for NeoForge from https://github.com/DaFuqs/FluidVoidFading/blob/1.21/src/main/java/de/dafuqs/fluidvoidfading/mixin/client/FluidRendererMixin.java
 * Licensed under GPL-3.0
 * 06/06/2025, AnAwesomGuy
 */

package de.dafuqs.fluidvoidfading.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {
    @Shadow
    private static boolean isNeighborSameFluid(FluidState firstState, FluidState secondState) {
        throw new AssertionError();
    }

    @Shadow
    private TextureAtlasSprite waterOverlay;

    @Shadow
    protected abstract int getLightColor(BlockAndTintGetter level, BlockPos pos);

    @Inject(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;isNeighborStateHidingOverlay(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
    public void render(BlockAndTintGetter level, BlockPos pos, VertexConsumer consumer, BlockState blockState, FluidState fluidState, CallbackInfo ci,
                       @Local(ordinal = 0) int color, @Local TextureAtlasSprite[] sprites,
                       @Local(ordinal = 3) FluidState north, @Local(ordinal = 4) FluidState south, @Local(ordinal = 5) FluidState west, @Local(ordinal = 6) FluidState east) {
        if (pos.getY() != level.getMinBuildHeight())
            return;

        Fluid fluid = fluidState.getType();
        if (fluid != Fluids.EMPTY) {
            float brightnessUp = level.getShade(Direction.UP, true);
            float brightnessNorth = level.getShade(Direction.NORTH, true);
            float brightnessWest = level.getShade(Direction.WEST, true);

            float xLo = (pos.getX() & 0xF);
            float yLo = (pos.getY() & 0xF);
            float zLo = (pos.getZ() & 0xF);

            int light = getLightColor(level, pos);
            TextureAtlasSprite sprite = sprites[1]; // flowing sprite

            float u1 = sprite.getU(0F);
            float u2 = sprite.getU(0.5F);
            float v1 = sprite.getV(0F);
            float v2 = sprite.getV(0.5F);

            float redF = FastColor.ARGB32.red(color) / 255F;
            float greenF = FastColor.ARGB32.green(color) / 255F;
            float blueF = FastColor.ARGB32.blue(color) / 255F;

            float alpha1 = FastColor.ARGB32.alpha(color) / 255F;
            float alpha2 = 0.3F * alpha1;

            for (Direction dir : Direction.Plane.HORIZONTAL) { // directions
                float x1, z1, x2, z2;
                boolean shouldRender;
                if (dir == Direction.NORTH) {
                    x1 = xLo;
                    x2 = xLo + 1F;
                    z1 = zLo + 0.001F;
                    z2 = zLo + 0.001F;
                    shouldRender = isNeighborSameFluid(fluidState, north);
                } else if (dir == Direction.SOUTH) {
                    x1 = xLo + 1F;
                    x2 = xLo;
                    z1 = zLo + 0.999F;
                    z2 = zLo + 0.999F;
                    shouldRender = isNeighborSameFluid(fluidState, south);
                } else if (dir == Direction.WEST) {
                    x1 = xLo + 0.001F;
                    x2 = xLo + 0.001F;
                    z1 = zLo + 1F;
                    z2 = zLo;
                    shouldRender = isNeighborSameFluid(fluidState, west);
                } else if (dir == Direction.EAST) {
                    x1 = xLo + 0.999F;
                    x2 = xLo + 0.999F;
                    z1 = zLo;
                    z2 = zLo + 1F;
                    shouldRender = isNeighborSameFluid(fluidState, east);
                } else continue;

                if (!shouldRender) {
                    float sidedBrightness = dir.getAxis() == Direction.Axis.Z ? brightnessNorth : brightnessWest;                    float red = brightnessUp * sidedBrightness * redF;
                    float green = brightnessUp * sidedBrightness * greenF;
                    float blue = brightnessUp * sidedBrightness * blueF;
                    fluidvoidfading$vertex(consumer, x1, yLo + 0F, z1, red, green, blue, u1, v1, light, alpha1);
                    fluidvoidfading$vertex(consumer, x2, yLo + 0F, z2, red, green, blue, u2, v1, light, alpha1);
                    fluidvoidfading$vertex(consumer, x2, yLo - 1F, z2, red, green, blue, u2, v2, light, alpha2);
                    fluidvoidfading$vertex(consumer, x1, yLo - 1F, z1, red, green, blue, u1, v2, light, alpha2);

                    fluidvoidfading$vertex(consumer, x1, yLo - 1F, z1, red, green, blue, u1, v1, light, alpha2);
                    fluidvoidfading$vertex(consumer, x2, yLo - 1F, z2, red, green, blue, u2, v1, light, alpha2);
                    fluidvoidfading$vertex(consumer, x2, yLo - 2F, z2, red, green, blue, u2, v2, light, 0F);
                    fluidvoidfading$vertex(consumer, x1, yLo - 2F, z1, red, green, blue, u1, v2, light, 0F);
                    if (sprite != waterOverlay) {
                        fluidvoidfading$vertex(consumer, x1, yLo - 1F, z1, red, green, blue, u1, v2, light, alpha2);
                        fluidvoidfading$vertex(consumer, x2, yLo - 1F, z2, red, green, blue, u2, v2, light, alpha2);
                        fluidvoidfading$vertex(consumer, x2, yLo + 0F, z2, red, green, blue, u2, v1, light, alpha1);
                        fluidvoidfading$vertex(consumer, x1, yLo + 0F, z1, red, green, blue, u1, v1, light, alpha1);

                        fluidvoidfading$vertex(consumer, x1, yLo - 2F, z1, red, green, blue, u1, v2, light, 0F);
                        fluidvoidfading$vertex(consumer, x2, yLo - 2F, z2, red, green, blue, u2, v2, light, 0F);
                        fluidvoidfading$vertex(consumer, x2, yLo - 1F, z2, red, green, blue, u2, v1, light, alpha2);
                        fluidvoidfading$vertex(consumer, x1, yLo - 1F, z1, red, green, blue, u1, v1, light, alpha2);
                    }
                }
            }
        }
    }

    @ModifyExpressionValue(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0))
    public boolean cullDownAtVoid(boolean original, BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState, FluidState fluidState) {
        return original && pos.getY() != level.getMinBuildHeight();
    }

    @Inject(method = "isFaceOccludedByState", at = @At("HEAD"), cancellable = true)
    private static void isSideCovered(BlockGetter level, Direction face, float height, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (face == Direction.DOWN && pos.getY() == level.getMinBuildHeight())
            cir.setReturnValue(true);
    }

    @Unique
    private void fluidvoidfading$vertex(VertexConsumer vertexConsumer, float x, float y, float z, float red, float green, float blue, float u, float v, int light, float alpha) {
        vertexConsumer.addVertex(x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setLight(light).setNormal(0F, 1F, 0F);
    }
}
