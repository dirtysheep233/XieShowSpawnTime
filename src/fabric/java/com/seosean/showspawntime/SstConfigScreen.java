package com.seosean.showspawntime;
 
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
 
public final class SstConfigScreen extends Screen {
    private final Screen parent;
 
    public SstConfigScreen(Screen parent) {
        super(SstI18n.text("screen.showspawntime.config.title"));
        this.parent = parent;
    }
 
    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(SstI18n.text("screen.showspawntime.config.hud_positions"), button -> {
            if (client != null) client.setScreen(new SstHudEditorScreen(this));
        }).dimensions(width / 2 - 100, height / 2 - 20, 200, 20).build());
 
        addDrawableChild(ButtonWidget.builder(SstI18n.text("gui.done"), button -> close())
                .dimensions(width / 2 - 100, height / 2 + 10, 200, 20).build());
    }
 
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
    }
 
    @Override
    public void close() {
        ShowSpawnTimeClient.CONFIG.save();
        if (client != null) client.setScreen(parent);
    }
 
    @Override
    public boolean shouldPause() { return false; }
}