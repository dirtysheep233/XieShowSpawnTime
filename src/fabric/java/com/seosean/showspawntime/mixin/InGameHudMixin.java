package com.seosean.showspawntime.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seosean.showspawntime.ShowSpawnTimeClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void showspawntime$render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ShowSpawnTimeClient.OVERLAY.render(context);
    }

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void showspawntime$setTitle(Text title, CallbackInfo ci) {
        ShowSpawnTimeClient.STATE.onTitle(title);
    }

    @ModifyExpressionValue(
            method = "method_55439(Lnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/number/NumberFormat;Lnet/minecraft/scoreboard/ScoreboardEntry;)Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/scoreboard/Team;decorateName(Lnet/minecraft/scoreboard/AbstractTeam;Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;"
            ),
            require = 1
    )
    private MutableText showspawntime$enhanceSidebarName(MutableText decoratedName,
                                                         net.minecraft.scoreboard.Scoreboard scoreboard,
                                                         net.minecraft.scoreboard.number.NumberFormat numberFormat,
                                                         ScoreboardEntry entry) {
        return ShowSpawnTimeClient.OVERLAY.enhanceSidebarLine(entry, decoratedName);
    }
}
