package com.mayhem.shinobi.story;

/**
 * A mission or story chapter. Story chapters (story=true) must be completed in order and are started at the sensei.
 * Rewards are granted exactly once per completion by MissionTracker.claim (state machine: active -> complete -> claimed).
 */
public record MissionDef(String id, String title, String rank, String desc, Objective type, String arg, int target,
                         int minLevel, int xp, int ap, int sp, String rewardItem, int rewardCount,
                         int repeatCooldownTicks, boolean story, String intro, String outro) {

    public enum Objective {
        KILL, KILL_TYPE, CAST, COLLECT, EVENT, BEAST_DEFEAT, BEAST_SEAL, LEVEL, LEARN,
        VISIT, TALK, WATER, WALL, ESCORT, PROTECT
    }

    public String objectiveText() {
        return switch (type) {
            case KILL -> "Defeat " + target + " hostile creatures";
            case KILL_TYPE -> "Defeat " + target + " x " + arg.replace("minecraft:", "");
            case CAST -> "Use jutsu " + target + " times" + ("any".equals(arg) ? "" : " (" + arg + ")");
            case COLLECT -> "Gather " + target + " x " + arg.replace("minecraft:", "");
            case EVENT -> "Complete " + target + " world event(s): " + arg;
            case BEAST_DEFEAT -> "Weaken a tailed beast" + ("any".equals(arg) ? "" : " (" + arg + ")");
            case BEAST_SEAL -> "Seal a tailed beast";
            case LEVEL -> "Reach level " + target;
            case LEARN -> "Learn " + target + " new jutsu";
            case VISIT -> "Travel to a " + arg.replace('_', ' ');
            case TALK -> "Speak with a village " + ("story".equals(arg) ? "sensei" : "contracts".equals(arg) ? "clerk" : "training".equals(arg) ? "trainer" : "merchant");
            case WATER -> "Walk on water for " + target + " seconds";
            case WALL -> "Climb walls for " + target + " seconds";
            case ESCORT -> "Escort the client safely for " + target + " seconds (stay close)";
            case PROTECT -> "Protect the client — keep them alive while defeating " + target + " hostiles";
        };
    }
}
