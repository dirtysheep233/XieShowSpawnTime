package com.seosean.showspawntime;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScoreboardSnapshot {
    private volatile State state = new State(Text.empty(), "", List.of(), List.of());

    public void update(MinecraftClient client) {
        if (client.world == null) {
            clear();
            return;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            clear();
            return;
        }

        Text titleText = objective.getDisplayName().copy();
        String title = trim(titleText);
        List<String> lines = new ArrayList<>();
        List<ScoreboardLine> rawLines = new ArrayList<>();

        scoreboard.getScoreboardEntries(objective).stream()
                .filter(entry -> !entry.hidden())
                .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed())
                .forEach(entry -> {
                    Text display = Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name());
                    String plain = trim(display);
                    if (!plain.isEmpty()) {
                        lines.add(plain);
                        rawLines.add(new ScoreboardLine(display.copy(), plain, entry.owner(), entry.value()));
                    }
                });
        state = new State(titleText, title, List.copyOf(lines), List.copyOf(rawLines));
    }

    public void clear() {
        state = new State(Text.empty(), "", List.of(), List.of());
    }

    public Text titleText() {
        return state.titleText().copy();
    }

    public String title() {
        return state.title();
    }

    public List<String> lines() {
        return state.lines();
    }

    public List<ScoreboardLine> rawLines() {
        return state.rawLines();
    }

    public String line(int row) {
        List<String> lines = state.lines();
        if (row < 1 || row > lines.size()) {
            return "";
        }
        return lines.get(row - 1);
    }

    public int size() {
        return state.lines().size();
    }

    public boolean isZombiesTitle() {
        return isZombiesTitle(state.title());
    }

    public boolean isInZombiesGame() {
        State snapshot = state;
        return isZombiesTitle(snapshot.title())
                && snapshot.lines().stream().anyMatch(ShowSpawnTimeClient.LANG::isZombiesLeft);
    }

    public static boolean isZombiesTitle(String string) {
        String s = trim(string);
        return s.contains("ZOMBIES") || s.contains("僵尸末日") || s.contains("殭屍末日");
    }

    public static String trim(Text text) {
        return trim(text.getString());
    }

    public static String trim(String text) {
        if (text == null) {
            return "";
        }
        String stripped = Formatting.strip(text);
        if (stripped == null) {
            stripped = text;
        }
        return stripped.replaceAll("§[a-zA-Z0-9]", "")
                .replaceAll(ShowSpawnTimeClient.EMOJI_REGEX, "").trim();
    }

    private record State(Text titleText, String title, List<String> lines, List<ScoreboardLine> rawLines) {
    }

    public record ScoreboardLine(Text display, String plain, String owner, int score) {
    }
}
