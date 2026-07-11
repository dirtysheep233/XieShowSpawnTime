package com.seosean.showspawntime;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SstCategoryConfigScreen extends Screen {
    private static final int CONTENT_TOP = 38;
    private static final int CONTENT_BOTTOM_MARGIN = 36;
    private final Screen parent;
    private final Category category;
    private final List<ClickableWidget> contentWidgets = new ArrayList<>();
    private final Map<ClickableWidget, Integer> baseY = new IdentityHashMap<>();
    private int scrollOffset;
    private int contentBottom;

    public SstCategoryConfigScreen(Screen parent, Category category, String titleKey) {
        super(SstI18n.text(titleKey));
        this.parent = parent;
        this.category = category;
    }

    @Override
    protected void init() {
        int y = 42;
        switch (category) {
            case SST -> {
                y = addToggle(y, "option.showspawntime.aa_wave_sound", () -> ShowSpawnTimeClient.CONFIG.playAaSound,
                        value -> ShowSpawnTimeClient.CONFIG.playAaSound = value);
                y = addToggle(y, "option.showspawntime.debb_wave_sound", () -> ShowSpawnTimeClient.CONFIG.playDebbSound,
                        value -> ShowSpawnTimeClient.CONFIG.playDebbSound = value);
                y = addField(y, "option.showspawntime.preceded_wave_sound", ShowSpawnTimeClient.CONFIG.precededWaveSound,
                        value -> ShowSpawnTimeClient.CONFIG.precededWaveSound = value);
                y = addField(y, "option.showspawntime.preceded_wave_pitch", Double.toString(ShowSpawnTimeClient.CONFIG.precededWavePitch),
                        value -> ShowSpawnTimeClient.CONFIG.precededWavePitch = parsePitch(value, ShowSpawnTimeClient.CONFIG.precededWavePitch));
                y = addField(y, "option.showspawntime.final_wave_sound", ShowSpawnTimeClient.CONFIG.finalWaveSound,
                        value -> ShowSpawnTimeClient.CONFIG.finalWaveSound = value);
                y = addField(y, "option.showspawntime.final_wave_pitch", Double.toString(ShowSpawnTimeClient.CONFIG.finalWavePitch),
                        value -> ShowSpawnTimeClient.CONFIG.finalWavePitch = parsePitch(value, ShowSpawnTimeClient.CONFIG.finalWavePitch));
                y = addToggle(y, "option.showspawntime.final_wave_countdown", () -> ShowSpawnTimeClient.CONFIG.debbCountdown,
                        value -> ShowSpawnTimeClient.CONFIG.debbCountdown = value);
                y = addField(y, "option.showspawntime.countdown_sound", ShowSpawnTimeClient.CONFIG.countdownSound,
                        value -> ShowSpawnTimeClient.CONFIG.countdownSound = value);
                y = addField(y, "option.showspawntime.countdown_pitch", Double.toString(ShowSpawnTimeClient.CONFIG.countdownPitch),
                        value -> ShowSpawnTimeClient.CONFIG.countdownPitch = parsePitch(value, ShowSpawnTimeClient.CONFIG.countdownPitch));
                y = addToggle(y, "option.showspawntime.aa_boss_colors", () -> ShowSpawnTimeClient.CONFIG.colorAlert,
                        value -> ShowSpawnTimeClient.CONFIG.colorAlert = value);
            }
            case RECORD -> {
                y = addRecord(y, "option.showspawntime.aa_record", true);
                y = addRecord(y, "option.showspawntime.debb_record", false);
                y = addToggle(y, "option.showspawntime.cleanup_time_tips", () -> ShowSpawnTimeClient.CONFIG.cleanUpTime,
                        value -> ShowSpawnTimeClient.CONFIG.cleanUpTime = value);
            }
            case POWERUP -> {
                y = addToggle(y, "option.showspawntime.powerup_alert", () -> ShowSpawnTimeClient.CONFIG.powerupAlert,
                        value -> ShowSpawnTimeClient.CONFIG.powerupAlert = value);
                y = addToggle(y, "option.showspawntime.powerup_predict", () -> ShowSpawnTimeClient.CONFIG.powerupPredict,
                        value -> ShowSpawnTimeClient.CONFIG.powerupPredict = value);
                y = addToggle(y, "option.showspawntime.powerup_countdown", () -> ShowSpawnTimeClient.CONFIG.powerupCountdown,
                        value -> ShowSpawnTimeClient.CONFIG.powerupCountdown = value);
                y = addToggle(y, "option.showspawntime.powerup_nametag_shadow", () -> ShowSpawnTimeClient.CONFIG.powerupNameTagShadow,
                        value -> ShowSpawnTimeClient.CONFIG.powerupNameTagShadow = value);
            }
            case QOL -> {
                y = addToggle(y, "option.showspawntime.lr_queue_helper", () -> ShowSpawnTimeClient.CONFIG.lightningRodQueue,
                        value -> ShowSpawnTimeClient.CONFIG.lightningRodQueue = value);
                y = addToggle(y, "option.showspawntime.wave_3_left_notice", () -> ShowSpawnTimeClient.CONFIG.wave3LeftNotice,
                        value -> ShowSpawnTimeClient.CONFIG.wave3LeftNotice = value);
                y = addToggle(y, "option.showspawntime.player_health_notice", () -> ShowSpawnTimeClient.CONFIG.playerHealthNotice,
                        value -> ShowSpawnTimeClient.CONFIG.playerHealthNotice = value);
                y = addRevive(y);
                y = addToggle(y, "option.showspawntime.individual_dps_counter", () -> ShowSpawnTimeClient.CONFIG.dpsCounter,
                        value -> ShowSpawnTimeClient.CONFIG.dpsCounter = value);
            }
        }
        contentBottom = y;
        for (net.minecraft.client.gui.Element element : children()) {
            if (element instanceof ClickableWidget widget) {
                contentWidgets.add(widget);
                baseY.put(widget, widget.getY());
            }
        }
        addDrawableChild(ButtonWidget.builder(SstI18n.text("gui.done"), button -> close())
                .dimensions(width / 2 - 100, height - 30, 200, 20).build());
        updateScroll();
    }

    private int addToggle(int y, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        ButtonWidget button = ButtonWidget.builder(toggleText(key, getter.getAsBoolean()), clicked -> {
            setter.accept(!getter.getAsBoolean());
            clicked.setMessage(toggleText(key, getter.getAsBoolean()));
            ShowSpawnTimeClient.CONFIG.save();
        }).dimensions(width / 2 - 100, y, 200, 20).build();
        addDrawableChild(button);
        return y + 24;
    }

    private int addField(int y, String key, String value, Consumer<String> setter) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, width / 2 - 100, y + 12, 200, 20, SstI18n.text(key));
        field.setText(value);
        field.setMaxLength(128);
        field.setChangedListener(setter);
        field.setPlaceholder(SstI18n.text(key));
        addDrawableChild(field);
        return y + 38;
    }

    private int addRecord(int y, String key, boolean aa) {
        ButtonWidget button = ButtonWidget.builder(recordText(key, aa), clicked -> {
            if (aa) ShowSpawnTimeClient.CONFIG.aaRoundsRecord = nextRecord(ShowSpawnTimeClient.CONFIG.aaRoundsRecord);
            else ShowSpawnTimeClient.CONFIG.debbRoundsRecord = nextRecord(ShowSpawnTimeClient.CONFIG.debbRoundsRecord);
            clicked.setMessage(recordText(key, aa));
            ShowSpawnTimeClient.CONFIG.save();
        }).dimensions(width / 2 - 100, y, 200, 20).build();
        addDrawableChild(button);
        return y + 24;
    }

    private int addRevive(int y) {
        ButtonWidget button = ButtonWidget.builder(reviveText(), clicked -> {
            ShowSpawnTimeClient.CONFIG.fastRevivePosition = ShowSpawnTimeClient.CONFIG.fastRevivePosition.next();
            clicked.setMessage(reviveText());
            ShowSpawnTimeClient.CONFIG.save();
        }).dimensions(width / 2 - 100, y, 200, 20).build();
        addDrawableChild(button);
        return y + 24;
    }

    private static Text toggleText(String key, boolean value) {
        return optionText(key, SstI18n.text(value ? "value.showspawntime.on" : "value.showspawntime.off"));
    }

    private static Text recordText(String key, boolean aa) {
        String value = aa ? ShowSpawnTimeClient.CONFIG.aaRoundsRecord : ShowSpawnTimeClient.CONFIG.debbRoundsRecord;
        return optionText(key, SstI18n.text("value.showspawntime.record." + value.toLowerCase(Locale.ROOT)));
    }

    private static Text reviveText() {
        return optionText("option.showspawntime.fast_revive", SstI18n.text("value.showspawntime.position."
                + ShowSpawnTimeClient.CONFIG.fastRevivePosition.name().toLowerCase(Locale.ROOT)));
    }

    private static Text optionText(String key, Text value) {
        return SstI18n.text("option.showspawntime.format", SstI18n.text(key), value);
    }

    private static String nextRecord(String current) {
        String[] values = {"OFF", "Quintuple", "Tenfold", "ALL"};
        for (int index = 0; index < values.length; index++) {
            if (values[index].equalsIgnoreCase(current)) return values[(index + 1) % values.length];
        }
        return "ALL";
    }

    private static double parsePitch(String value, double fallback) {
        try {
            return Math.max(0.0, Math.min(2.0, Double.parseDouble(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, contentBottom - (height - CONTENT_BOTTOM_MARGIN));
        if (maxScroll == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.round(verticalAmount * 18.0)));
        updateScroll();
        return true;
    }

    private void updateScroll() {
        int viewportBottom = height - CONTENT_BOTTOM_MARGIN;
        for (ClickableWidget widget : contentWidgets) {
            int y = baseY.get(widget) - scrollOffset;
            widget.setY(y);
            widget.visible = y + widget.getHeight() > CONTENT_TOP && y < viewportBottom;
            widget.active = widget.visible;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
        if (category == Category.SST) {
            String[] keys = {"option.showspawntime.preceded_wave_sound", "option.showspawntime.preceded_wave_pitch",
                    "option.showspawntime.final_wave_sound", "option.showspawntime.final_wave_pitch",
                    "option.showspawntime.countdown_sound", "option.showspawntime.countdown_pitch"};
            int[] positions = {90, 128, 166, 204, 266, 304};
            for (int index = 0; index < keys.length; index++) {
                int y = positions[index] - scrollOffset;
                if (y >= CONTENT_TOP && y < height - CONTENT_BOTTOM_MARGIN) {
                    context.drawTextWithShadow(textRenderer, SstI18n.text(keys[index]), width / 2 - 100, y, 0xFFAAAAAA);
                }
            }
        }
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

    public enum Category { SST, RECORD, POWERUP, QOL }
}
