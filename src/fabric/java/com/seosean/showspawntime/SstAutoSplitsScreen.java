package com.seosean.showspawntime;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class SstAutoSplitsScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget hostField;
    private TextFieldWidget portField;

    public SstAutoSplitsScreen(Screen parent) {
        super(SstI18n.text("screen.showspawntime.autosplits.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        addDrawableChild(ButtonWidget.builder(enabledText(), button -> {
            ShowSpawnTimeClient.AUTO_SPLITS.toggle();
            button.setMessage(enabledText());
        }).dimensions(x, 45, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(modeText(), button -> {
            ShowSpawnTimeClient.CONFIG.autoSplitsMode = ShowSpawnTimeClient.CONFIG.autoSplitsMode.next();
            ShowSpawnTimeClient.CONFIG.save();
            button.setMessage(modeText());
        }).dimensions(x, 70, 200, 20).build());

        hostField = new TextFieldWidget(textRenderer, x, 112, 200, 20, SstI18n.text("option.showspawntime.livesplit_host"));
        hostField.setText(ShowSpawnTimeClient.CONFIG.autoSplitsHost);
        hostField.setMaxLength(255);
        addDrawableChild(hostField);

        portField = new TextFieldWidget(textRenderer, x, 154, 200, 20, SstI18n.text("option.showspawntime.livesplit_port"));
        portField.setText(Integer.toString(ShowSpawnTimeClient.CONFIG.autoSplitsPort));
        portField.setMaxLength(5);
        addDrawableChild(portField);

        addDrawableChild(ButtonWidget.builder(SstI18n.text("gui.done"), button -> close())
                .dimensions(x, height - 30, 200, 20).build());
    }

    private static Text enabledText() {
        return SstI18n.text("option.showspawntime.format", Text.literal("AutoSplits"),
                SstI18n.text(ShowSpawnTimeClient.AUTO_SPLITS.enabled()
                        ? "value.showspawntime.on" : "value.showspawntime.off"));
    }

    private static Text modeText() {
        return SstI18n.text("option.showspawntime.format", SstI18n.text("option.showspawntime.mode"),
                SstI18n.text("value.showspawntime.autosplits_mode."
                        + ShowSpawnTimeClient.CONFIG.autoSplitsMode.name().toLowerCase(java.util.Locale.ROOT)));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, SstI18n.text("option.showspawntime.livesplit_host"),
                width / 2 - 100, 100, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, SstI18n.text("option.showspawntime.livesplit_port"),
                width / 2 - 100, 142, 0xFFAAAAAA);
    }

    @Override
    public void close() {
        String host = hostField == null ? "" : hostField.getText().trim();
        if (!host.isEmpty()) {
            ShowSpawnTimeClient.CONFIG.autoSplitsHost = host;
        }
        if (portField != null) {
            try {
                int port = Integer.parseInt(portField.getText());
                if (port >= 1 && port <= 65535) {
                    ShowSpawnTimeClient.CONFIG.autoSplitsPort = port;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        ShowSpawnTimeClient.CONFIG.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
