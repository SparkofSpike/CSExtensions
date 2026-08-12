package org.Spike.CSExtensions.Modifier.Mythic.core;

public enum MythicTrigger {
    SHOOT,
    HIT,
    HITBLOCK,
    CRIT,
    HEADSHOT,
    KILL,
    RELOAD,
    DAMAGED,
    TIMER;

    public static MythicTrigger fromString(String str) {
        if (str == null) return null;
        str = str.toLowerCase();

        if (str.startsWith("~ontimer:")) return TIMER;
        if (str.equals("~onshoot")) return SHOOT;
        if (str.equals("~onhit") || str.equals("~onattack")) return HIT;
        if (str.equals("~onhitblock")) return HITBLOCK;
        if (str.equals("~oncrit")) return CRIT;
        if (str.equals("~onheadshot")) return HEADSHOT;
        if (str.equals("~onkill")) return KILL;
        if (str.equals("~onreload")) return RELOAD;
        if (str.equals("~ondamaged")) return DAMAGED;

        return null;
    }
}