package com.swvague.ars_odyssey.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.swvague.ars_odyssey.block.ResonanceNexusCoreBlock;
import com.swvague.ars_odyssey.block.ResonanceNexusCoreBlockEntity;
import com.swvague.ars_odyssey.registry.ModRegistry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class ResonanceNexusCoreRenderer implements BlockEntityRenderer<ResonanceNexusCoreBlockEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final String[] LAW_TEXT = {
            "LAW://A7F3",
            "REALITY::REWRITE",
            "CAUSAL_STACK<?>",
            "SOURCE_TRUTH#99"
    };
    private static final String[] FINAL_LAW_TEXT = {
            "AXIOM://RENDERED",
            "WORLDLINE_LOCKED",
            "MANA_SOURCE=TRUTH",
            "ENTROPY--",
            "CAUSE=>EFFECT",
            "FORM(NULL)=>LAW",
            "INDEX:STABLE",
            "REALITY.PRISM"
    };

    private final ItemRenderer itemRenderer;
    private final Font font;
    private final ItemStack prismShard;
    private final ItemStack lawCube;
    private final ItemStack lawCore;

    public ResonanceNexusCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
        this.prismShard = new ItemStack(ModRegistry.RESONANCE_PRISM_SHARD_VISUAL.get());
        this.lawCube = new ItemStack(ModRegistry.RESONANCE_LAW_CUBE_VISUAL.get());
        this.lawCore = new ItemStack(ModRegistry.RESONANCE_LAW_CORE_VISUAL.get());
    }

    @Override
    public void render(ResonanceNexusCoreBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(ResonanceNexusCoreBlock.PARTICLES)) {
            return;
        }

        float time = level.getGameTime() + partialTick;
        int tier = state.getValue(ResonanceNexusCoreBlock.TIER);
        int visualTier = Math.min(tier, ResonanceNexusCoreBlock.LAW_TIER);
        if (tier < ResonanceNexusCoreBlock.FINAL_TIER) {
            int orbitCount = 4 + visualTier * 2;
            float orbitRadius = 0.64F + visualTier * 0.08F;
            float orbitHeight = 1.92F + visualTier * 0.16F;
            for (int i = 0; i < orbitCount; i++) {
                renderOrbitingShard(level, poseStack, bufferSource, packedLight, time,
                        360.0F / orbitCount * i, orbitRadius, orbitHeight, 0.54F);
            }
        }

        if (tier > 0 && tier < ResonanceNexusCoreBlock.FINAL_TIER) {
            int crownCount = 4 + visualTier;
            for (int i = 0; i < crownCount; i++) {
                renderCrownShard(level, poseStack, bufferSource, packedLight, time,
                        360.0F / crownCount * i + 45.0F, 0.35F + visualTier * 0.04F, 2.72F + visualTier * 0.18F, 0.40F);
            }
        }

        if (tier == ResonanceNexusCoreBlock.LAW_TIER) {
            renderLawCore(level, poseStack, bufferSource, FULL_BRIGHT, time);
            renderLawText(poseStack, bufferSource, FULL_BRIGHT, time);
        } else if (tier >= ResonanceNexusCoreBlock.FINAL_TIER) {
            renderFinalPrism(level, poseStack, bufferSource, FULL_BRIGHT, time);
            renderFinalLawText(poseStack, bufferSource, FULL_BRIGHT, time);
        }
    }

    private void renderOrbitingShard(Level level, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                     float time, float angleOffset, float radius, float baseY, float scale) {
        float angle = time * 1.25F + angleOffset;
        float bob = (float) Math.sin((time + angleOffset) * 0.09F) * 0.07F;
        renderShard(level, poseStack, bufferSource, packedLight, angle, radius, baseY + bob, scale, 18.0F);
    }

    private void renderCrownShard(Level level, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                  float time, float angleOffset, float radius, float baseY, float scale) {
        float angle = -time * 0.82F + angleOffset;
        float bob = (float) Math.sin((time + angleOffset) * 0.075F) * 0.05F;
        renderShard(level, poseStack, bufferSource, packedLight, angle, radius, baseY + bob, scale, -24.0F);
    }

    private void renderShard(Level level, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                             float angleDegrees, float radius, float y, float scale, float zTilt) {
        double radians = Math.toRadians(angleDegrees);
        double x = Math.cos(radians) * radius;
        double z = Math.sin(radians) * radius;

        poseStack.pushPose();
        poseStack.translate(0.5D + x, y, 0.5D + z);
        poseStack.mulPose(Axis.YP.rotationDegrees(angleDegrees + 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(zTilt));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(prismShard, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, level, 0);
        poseStack.popPose();
    }

    private void renderLawCore(Level level, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float time) {
        float bob = Mth.sin(time * 0.055F) * 0.08F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 3.45D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.4F));
        poseStack.mulPose(Axis.XP.rotationDegrees(35.264F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.scale(0.78F, 0.78F, 0.78F);
        itemRenderer.renderStatic(lawCube, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, level, 0);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5D, 3.45D + bob * 0.55F, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 1.9F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-35.264F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.scale(0.48F, 0.48F, 0.48F);
        itemRenderer.renderStatic(lawCore, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, level, 0);
        poseStack.popPose();
    }

    private void renderLawText(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float time) {
        float spin = time * 0.9F;
        for (int i = 0; i < LAW_TEXT.length; i++) {
            float angle = spin + i * 90.0F;
            float bob = Mth.sin(time * 0.045F + i * 1.7F) * 0.05F;
            renderTextPlate(poseStack, bufferSource, packedLight, LAW_TEXT[i], angle, 1.04F, 3.98F + bob, i, 0.012F);
        }
    }

    private void renderFinalPrism(Level level, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float time) {
        float bob = Mth.sin(time * 0.04F) * 0.10F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 3.55D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.48F));
        poseStack.mulPose(Axis.XP.rotationDegrees(35.264F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.scale(2.08F, 2.08F, 2.08F);
        itemRenderer.renderStatic(lawCube, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, level, 0);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5D, 3.55D - bob * 0.35F, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 0.72F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-35.264F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.scale(1.34F, 1.34F, 1.34F);
        itemRenderer.renderStatic(lawCore, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, level, 0);
        poseStack.popPose();
    }

    private void renderFinalLawText(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float time) {
        float lowerSpin = -time * 0.48F;
        float upperSpin = time * 0.36F;
        for (int i = 0; i < FINAL_LAW_TEXT.length; i++) {
            float lowerAngle = lowerSpin + i * (360.0F / FINAL_LAW_TEXT.length);
            float lowerBob = Mth.sin(time * 0.03F + i * 1.7F) * 0.06F;
            renderTextPlate(poseStack, bufferSource, packedLight, FINAL_LAW_TEXT[i],
                    lowerAngle, 2.22F, 3.75F + lowerBob, i, 0.0135F);

            float upperAngle = upperSpin + i * (360.0F / FINAL_LAW_TEXT.length) + 22.5F;
            float upperBob = Mth.sin(time * 0.025F + i * 1.1F) * 0.05F;
            renderTextPlate(poseStack, bufferSource, packedLight, FINAL_LAW_TEXT[(i + 3) % FINAL_LAW_TEXT.length],
                    upperAngle, 1.78F, 5.45F + upperBob, i + 1, 0.012F);
        }
    }

    private void renderTextPlate(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                 String text, float angleDegrees, float radius, float y, int index, float scale) {
        double radians = Math.toRadians(angleDegrees);
        float x = (float) Math.cos(radians) * radius;
        float z = (float) Math.sin(radians) * radius;
        int color = index % 2 == 0 ? 0x9FFBFF : 0xD8A8FF;

        poseStack.pushPose();
        poseStack.translate(0.5F + x, y, 0.5F + z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-angleDegrees + 90.0F));
        poseStack.scale(scale, -scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        float width = font.width(text);
        font.drawInBatch(text, -width / 2.0F, 0.0F, color, false, matrix, bufferSource,
                Font.DisplayMode.SEE_THROUGH, 0x33000000, packedLight);
        poseStack.popPose();
    }
}
