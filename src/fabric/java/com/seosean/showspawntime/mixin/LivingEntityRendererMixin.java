package com.seosean.showspawntime.mixin;

import com.seosean.showspawntime.PlayerRenderStateAlpha;
import com.seosean.showspawntime.TranslucentRenderLayer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private float showspawntime$currentAlpha = 1.0F;
    @Unique
    private Identifier showspawntime$currentTexture;

    @Shadow
    public abstract Identifier getTexture(LivingEntityRenderState state);

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void showspawntime$captureAlpha(LivingEntityRenderState state, MatrixStack matrices,
                                            VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        showspawntime$currentAlpha = state instanceof PlayerEntityRenderState
                ? ((PlayerRenderStateAlpha) state).showspawntime$getAlpha()
                : 1.0F;
        showspawntime$currentTexture = showspawntime$currentAlpha < 1.0F ? getTexture(state) : null;
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"),
            index = 0
    )
    private RenderLayer showspawntime$useTranslucentLayer(RenderLayer layer) {
        return layer != null && showspawntime$currentAlpha < 1.0F
                ? TranslucentRenderLayer.forTexture(showspawntime$currentTexture)
                : layer;
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
            index = 4
    )
    private int showspawntime$applyAlpha(int color) {
        return showspawntime$currentAlpha < 1.0F
                ? ColorHelper.withAlpha((int) (showspawntime$currentAlpha * 255.0F), color)
                : color;
    }
}
