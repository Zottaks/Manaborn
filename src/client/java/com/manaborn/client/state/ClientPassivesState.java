package com.manaborn.client.state;

import com.manaborn.passives.PassiveType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Клиентское состояние пассивок: изученные + два активных слота.
 */
public class ClientPassivesState {
    private static final Set<PassiveType> learned = EnumSet.noneOf(PassiveType.class);
    private static PassiveType[] active = new PassiveType[2];

    public static void setFromPacket(List<String> learnedIds, String slot0, String slot1) {
        learned.clear();
        for (String id : learnedIds) {
            PassiveType type = PassiveType.fromId(id);
            if (type != null) learned.add(type);
        }
        PassiveType s0 = (slot0 != null && !slot0.isEmpty()) ? PassiveType.fromId(slot0) : null;
        PassiveType s1 = (slot1 != null && !slot1.isEmpty()) ? PassiveType.fromId(slot1) : null;
        active = new PassiveType[]{s0, s1};
    }

    public static Set<PassiveType> getLearned() {
        return new HashSet<>(learned);
    }

    public static PassiveType[] getActive() {
        return active.clone();
    }

    public static void clear() {
        learned.clear();
        active = new PassiveType[2];
    }
}
