package com.seosean.showspawntime;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class SstConfigScreen extends Screen {
    private final Screen parent;

    public SstConfigScreen(Screen parent) {
        super(SstI18n.text("screen.showspawntime.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = 42;
        addCategory(x, y, "screen.showspawntime.category.sst", SstCategoryConfigScreen.Category.SST);
        addCategory(x, y += 24, "screen.showspawntime.category.record", SstCategoryConfigScreen.Category.RECORD);
        addCategory(x, y += 24, "screen.showspawntime.category.powerup", SstCategoryConfigScreen.Category.POWERUP);
        addCategory(x, y += 24, "screen.showspawntime.category.qol", SstCategoryConfigScreen.Category.QOL);
        addDrawableChild(ButtonWidget.builder(SstI18n.text("screen.showspawntime.config.hud_positions"), button -> {
            if (client != null) client.setScreen(new SstHudEditorScreen(this));
        }).dimensions(x, y += 32, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(SstI18n.text("screen.showspawntime.config.autosplits"), button -> {
            if (client != null) client.setScreen(new SstAutoSplitsScreen(this));
        }).dimensions(x, y += 24, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(SstI18n.text("gui.done"), button -> close())
                .dimensions(x, height - 30, 200, 20).build());
    }

    private void addCategory(int x, int y, String titleKey, SstCategoryConfigScreen.Category category) {
        addDrawableChild(ButtonWidget.builder(SstI18n.text(titleKey), button -> {
            if (client != null) client.setScreen(new SstCategoryConfigScreen(this, category, titleKey));
        }).dimensions(x, y, 200, 20).build());
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
    public boolean shouldPause() {
        return false;
    }
}
