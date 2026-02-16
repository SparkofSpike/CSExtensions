package org.Spike.CSExtensions.Modifier.Accessories.Mythic;

public enum AccessoryMythicTrigger {
    ON_TIMER,      
    ON_DAMAGED,    
    ON_ATTACK,     
    ON_SHOOT,      
    ON_CRIT,       
    ON_HEADSHOT,   
    ON_RELOAD,     
    ON_HITBLOCK;    

    public static AccessoryMythicTrigger fromString(String str) {
        if (str == null) return null;

        str = str.trim().toLowerCase();
        if (str.startsWith("~ontimer:")) {
            return ON_TIMER;
        } else if (str.equals("~ondamaged")) {
            return ON_DAMAGED;
        } else if (str.equals("~onattack")) {
            return ON_ATTACK;
        } else if (str.equals("~onshoot")) {
            return ON_SHOOT;
        } else if (str.equals("~oncrit")) {
            return ON_CRIT;
        } else if (str.equals("~onheadshot")) {
            return ON_HEADSHOT;
        } else if (str.equals("~onreload")) {
            return ON_RELOAD;
        } else if (str.equals("~onhitblock")) {
            return ON_HITBLOCK;
        }
        return null;
    }

    public static int extractTimerTicks(String triggerStr) {
        if (triggerStr == null || !triggerStr.startsWith("~onTimer:")) {
            return 20; 
        }

        try {
            String[] parts = triggerStr.split(":");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            
        }
        return 20;
    }
}