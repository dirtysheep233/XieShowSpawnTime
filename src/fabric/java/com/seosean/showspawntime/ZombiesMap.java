package com.seosean.showspawntime;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public enum ZombiesMap {
    NULL(new int[][]{}, 0),
    PRISON(new int[][]{{10,20},{10,20,30},{10,17,24,31},{10,17,24,31},{10,20,30},{10,20,30},{10,20,30},{10,25,40},{10,25,35},{10,25,45},{10,25,40},{10,25,37},{10,22,34},{10,25,37},{10,25,40},{10,22,37},{10,22,42},{10,25,45},{10,25,45},{10,25,40},{10,20,35,55,75},{10,25,40},{10,30,50},{10,30,50},{10,25,45},{10,30,50},{10,25,45},{10,30,50},{10,30,55},{10}}, 30),
    THE_LAB(new int[][]{{10,22},{10,22},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34}}, 40),
    DEAD_END(new int[][]{{10,20},{10,20},{10,20,35},{10,20,35},{10,22,37},{10,22,44},{10,25,47},{10,25,50},{10,22,38},{10,24,45},{10,25,48},{10,25,50},{10,25,50},{10,25,45},{10,25,46},{10,24,47},{10,24,47},{10,24,47},{10,24,47},{10,24,49},{10,23,44},{10,23,45},{10,23,42},{10,23,43},{10,23,43},{10,23,36},{10,24,44},{10,24,42},{10,24,42},{10,24,45}}, 30),
    BAD_BLOOD(new int[][]{{10,22},{10,22},{10,22},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,22,34},{10,24,38},{10,24,38},{10,22,34},{10,24,38},{10,22,34}}, 30),
    ALIEN_ARCADIUM(new int[][]{{10,13,16,19},{10,14,18,22},{10,13,16,19},{10,14,17,21,25,28},{10,14,18,22,26,30},{10,14,19,23,28,32},{10,15,19,23,27,31},{10,15,20,25,30,35},{10,14,19,23,28,32},{10,16,22,27,33,38},{10,16,21,27,32,38},{10,16,22,28,34,40},{10,16,22,28,34,40},{10,16,21,26,31,36},{10,17,24,31,38,46},{10,16,22,27,33,38},{10,14,19,23,28,32},{10,14,19,23,28,32},{10,14,18,22,26,30},{10,15,21,26,31,36},{10,14,19,23,28,32},{10,14,19,23,28,34},{10,14,18,22,26,30},{10,14,19,23,28,32},{10},{10,23,36},{10,22,34},{10,20,30},{10,24,38},{10,22,34},{10,22,34},{10,21,32},{10,22,34},{10,22,34},{10},{10,22,34},{10,20,31},{10,22,34},{10,22,34},{10,22,34,37,45},{10,21,32},{10,22,34},{10,13,22,25,34,37},{10,22,34},{10,22,34,35},{10,21,32,35},{10,20,30},{10,20,30,33},{10,21,32},{10,22,34,37},{10,20,30,33},{10,22,34,37},{10,22,34,37},{10,20,32,35,39},{10,16,22,28,34,40},{10,14,18},{10,14,18},{10,22,34,37,38},{10,14,18,22,26,30},{10,20,30,33},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,27,32},{10,14,18,22,27,32},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{10,14,18,22,26,30},{5},{5},{5},{5},{5}}, 105);

    private final int[][] timer;
    private final int maxRound;

    ZombiesMap(int[][] timer, int maxRound) {
        this.timer = timer;
        this.maxRound = maxRound;
    }

    public int[][] timer() {
        return timer;
    }

    public int maxRound() {
        return maxRound;
    }

    public int[] roundTimes(int round) {
        if (round < 1 || round > timer.length) {
            return new int[0];
        }
        return timer[round - 1].clone();
    }

    public static ZombiesMap detect(MinecraftClient client, ScoreboardSnapshot scoreboard) {
        ZombiesMap byBlocks = detectByBlock(client);
        if (byBlocks != NULL) {
            return byBlocks;
        }

        String joined = String.join(" ", scoreboard.lines()).toLowerCase();
        if (joined.contains("alien arcadium") || joined.contains("外星")) {
            return ALIEN_ARCADIUM;
        }
        if (joined.contains("bad blood") || joined.contains("坏血") || joined.contains("壞血")) {
            return BAD_BLOOD;
        }
        if (joined.contains("dead end") || joined.contains("穷途") || joined.contains("窮途")) {
            return DEAD_END;
        }
        if (joined.contains("the lab")) {
            return THE_LAB;
        }
        if (joined.contains("prison")) {
            return PRISON;
        }
        return NULL;
    }

    private static ZombiesMap detectByBlock(MinecraftClient client) {
        if (client.world == null) {
            return NULL;
        }

        BlockPos pos = new BlockPos(0, 72, 12);
        if (!client.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return NULL;
        }
        BlockState state = client.world.getBlockState(pos);
        Block block = state.getBlock();
        Identifier id = Registries.BLOCK.getId(block);
        String path = id.getPath();

        if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
            return THE_LAB;
        }
        if (path.endsWith("_carpet")) {
            return ALIEN_ARCADIUM;
        }
        if (block == Blocks.STONE_BRICKS || block == Blocks.CRACKED_STONE_BRICKS || block == Blocks.CHISELED_STONE_BRICKS) {
            return BAD_BLOOD;
        }
        if (path.endsWith("_wool")) {
            return DEAD_END;
        }
        if (path.endsWith("terracotta")) {
            return PRISON;
        }
        return NULL;
    }
}
