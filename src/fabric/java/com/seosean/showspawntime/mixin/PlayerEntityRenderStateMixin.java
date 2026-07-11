package com.seosean.showspawntime.mixin;

import com.seosean.showspawntime.PlayerRenderStateAlpha;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements PlayerRenderStateAlpha {
    @Unique
    private float showspawntime$alpha = 1.0F;

    @Override
    public float showspawntime$getAlpha() {
        return showspawntime$alpha;
    }

    @Override
    public void showspawntime$setAlpha(float alpha) {
        showspawntime$alpha = alpha;
    }
}
