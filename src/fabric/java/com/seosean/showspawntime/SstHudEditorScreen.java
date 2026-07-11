package com.seosean.showspawntime;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class SstHudEditorScreen extends HudEditorCompatScreen {
    private static final int BOX_WIDTH = 110;
    private static final int BOX_HEIGHT = 28;
    private final Screen parent;
    private HudPart dragging;

    public SstHudEditorScreen(Screen parent) {
        super(SstI18n.text("screen.showspawntime.hud.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(SstI18n.text("gui.done"), button -> close())
                .dimensions(width / 2 - 102, height - 25, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(SstI18n.text("controls.reset"), button -> {
            ShowSpawnTimeClient.CONFIG.resetHudPositions();
            clearAndInit();
        }).dimensions(width / 2 + 2, height - 25, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, SstI18n.text("screen.showspawntime.hud.instructions"),
                width / 2, 26, 0xFFAAAAAA);
        drawPart(context, HudPart.SPAWN, spawnX(), spawnY(), Text.literal("➤ W1 00:10"));
        drawPart(context, HudPart.POWERUP, powerupX(), powerupY(),
                SstI18n.text("hud.showspawntime.powerup.active", SstI18n.text("powerup.showspawntime.insta_kill"), "00:60"));
        drawPart(context, HudPart.DPS, dpsX(), dpsY(), SstI18n.text("hud.showspawntime.dps", 0));
        drawPart(context, HudPart.AUTO_SPLITS, autoSplitsX(), autoSplitsY(), Text.literal("0:00:0"));
    }

    private void drawPart(DrawContext context, HudPart part, int x, int y, Text text) {
        int color = dragging == part ? 0xA0804080 : 0xA0303030;
        context.fill(x, y, x + BOX_WIDTH, y + BOX_HEIGHT, color);
        int border = dragging == part ? 0xFFFFFFFF : 0xFF808080;
        context.fill(x, y, x + BOX_WIDTH, y + 1, border);
        context.fill(x, y + BOX_HEIGHT - 1, x + BOX_WIDTH, y + BOX_HEIGHT, border);
        context.fill(x, y, x + 1, y + BOX_HEIGHT, border);
        context.fill(x + BOX_WIDTH - 1, y, x + BOX_WIDTH, y + BOX_HEIGHT, border);
        context.drawTextWithShadow(textRenderer, text, x + 5, y + 10, 0xFFFFFFFF);
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, spawnX(), spawnY())) dragging = HudPart.SPAWN;
            else if (inside(mouseX, mouseY, powerupX(), powerupY())) dragging = HudPart.POWERUP;
            else if (inside(mouseX, mouseY, dpsX(), dpsY())) dragging = HudPart.DPS;
            else if (inside(mouseX, mouseY, autoSplitsX(), autoSplitsY())) dragging = HudPart.AUTO_SPLITS;
            if (dragging != null) {
                move(dragging, mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging != null) {
            move(dragging, mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging != null) {
            dragging = null;
            ShowSpawnTimeClient.CONFIG.save();
            return true;
        }
        return false;
    }

    private void move(HudPart part, double mouseX, double mouseY) {
        double x = clamp((mouseX - BOX_WIDTH / 2.0) / width);
        double y = clamp((mouseY - BOX_HEIGHT / 2.0) / height);
        switch (part) {
            case SPAWN -> {
                ShowSpawnTimeClient.CONFIG.xSpawnTime = x;
                ShowSpawnTimeClient.CONFIG.ySpawnTime = y;
            }
            case POWERUP -> {
                ShowSpawnTimeClient.CONFIG.xPowerup = x;
                ShowSpawnTimeClient.CONFIG.yPowerup = y;
            }
            case DPS -> {
                ShowSpawnTimeClient.CONFIG.xDpsCounter = x;
                ShowSpawnTimeClient.CONFIG.yDpsCounter = y;
            }
            case AUTO_SPLITS -> {
                ShowSpawnTimeClient.CONFIG.xAutoSplits = x;
                ShowSpawnTimeClient.CONFIG.yAutoSplits = y;
            }
        }
    }

    private boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + BOX_WIDTH && mouseY >= y && mouseY < y + BOX_HEIGHT;
    }

    private int spawnX() {
        return ShowSpawnTimeClient.CONFIG.xSpawnTime < 0 ? width - BOX_WIDTH - 10
                : (int) (ShowSpawnTimeClient.CONFIG.xSpawnTime * width);
    }

    private int spawnY() {
        return ShowSpawnTimeClient.CONFIG.ySpawnTime < 0 ? height - 80
                : (int) (ShowSpawnTimeClient.CONFIG.ySpawnTime * height);
    }

    private int powerupX() {
        return ShowSpawnTimeClient.CONFIG.xPowerup < 0 ? 10
                : (int) (ShowSpawnTimeClient.CONFIG.xPowerup * width);
    }

    private int powerupY() {
        return ShowSpawnTimeClient.CONFIG.yPowerup < 0 ? height / 2 - 40
                : (int) (ShowSpawnTimeClient.CONFIG.yPowerup * height);
    }

    private int dpsX() {
        return ShowSpawnTimeClient.CONFIG.xDpsCounter < 0 ? width * 3 / 4 - BOX_WIDTH
                : (int) (ShowSpawnTimeClient.CONFIG.xDpsCounter * width);
    }

    private int dpsY() {
        return ShowSpawnTimeClient.CONFIG.yDpsCounter < 0 ? height - 40
                : (int) (ShowSpawnTimeClient.CONFIG.yDpsCounter * height);
    }

    private int autoSplitsX() {
        return ShowSpawnTimeClient.CONFIG.xAutoSplits < 0 ? width - BOX_WIDTH - 10
                : (int) (ShowSpawnTimeClient.CONFIG.xAutoSplits * width);
    }

    private int autoSplitsY() {
        return ShowSpawnTimeClient.CONFIG.yAutoSplits < 0 ? height - 30
                : (int) (ShowSpawnTimeClient.CONFIG.yAutoSplits * height);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(0.95, value));
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

    private enum HudPart {
        SPAWN,
        POWERUP,
        DPS,
        AUTO_SPLITS
    }
}
