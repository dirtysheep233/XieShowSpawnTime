package com.seosean.showspawntime;
 
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
 
public final class SstHudEditorScreen extends HudEditorCompatScreen {
    private static final int BOX_WIDTH = 80;
    private static final int BOX_HEIGHT = 30;
    private static final int HEADER_HEIGHT = 12;
 
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
 
    public SstHudEditorScreen(Screen parent) {
        super(SstI18n.text("screen.showspawntime.hud_editor.title"));
    }
 
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (client == null) return;
 
        int spawnX = getSpawnX();
        int spawnY = getSpawnY();
 
        context.fill(spawnX, spawnY, spawnX + BOX_WIDTH, spawnY + HEADER_HEIGHT, 0x80000000);
        context.fill(spawnX, spawnY + HEADER_HEIGHT, spawnX + BOX_WIDTH, spawnY + BOX_HEIGHT, 0x40FFFFFF);
        context.drawTextWithShadow(textRenderer, SstI18n.text("hud.showspawntime.spawn_time_title").getString(),
                spawnX + 4, spawnY + 2, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "W1 5.00",
                spawnX + 4, spawnY + HEADER_HEIGHT + 2, 0xFFFFFF00);
        context.drawTextWithShadow(textRenderer, "W2 10.00",
                spawnX + 4, spawnY + HEADER_HEIGHT + 12, 0xFF808080);
 
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
        Text hint = SstI18n.text("screen.showspawntime.hud_editor.hint").formatted(Formatting.GREEN);
        context.drawCenteredTextWithShadow(textRenderer, hint, width / 2, height - 20, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, deltaTicks);
    }
 
    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        int spawnX = getSpawnX();
        int spawnY = getSpawnY();
        if (mouseX >= spawnX && mouseX <= spawnX + BOX_WIDTH
                && mouseY >= spawnY && mouseY <= spawnY + BOX_HEIGHT) {
            dragging = true;
            dragOffsetX = mouseX - spawnX;
            dragOffsetY = mouseY - spawnY;
            return true;
        }
        return false;
    }
 
    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging) return false;
        double newX = (mouseX - dragOffsetX) / width;
        double newY = (mouseY - dragOffsetY) / height;
        ShowSpawnTimeClient.CONFIG.xSpawnTime = Math.max(0, Math.min(1, newX));
        ShowSpawnTimeClient.CONFIG.ySpawnTime = Math.max(0, Math.min(1, newY));
        return true;
    }
 
    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            ShowSpawnTimeClient.CONFIG.save();
            return true;
        }
        return false;
    }
 
    @Override
    public void close() {
        ShowSpawnTimeClient.CONFIG.save();
        if (client != null) client.setScreen(null);
    }
 
    @Override
    public boolean shouldPause() { return false; }
 
    private int getSpawnX() {
        return ShowSpawnTimeClient.CONFIG.xSpawnTime < 0
                ? width - BOX_WIDTH - 5
                : (int) (ShowSpawnTimeClient.CONFIG.xSpawnTime * width);
    }
 
    private int getSpawnY() {
        return ShowSpawnTimeClient.CONFIG.ySpawnTime < 0
                ? height - BOX_HEIGHT - 5
                : (int) (ShowSpawnTimeClient.CONFIG.ySpawnTime * height);
    }
}