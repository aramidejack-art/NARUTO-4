package com.mayhem.shinobi;

import com.mayhem.shinobi.story.MissionDef;
import com.mayhem.shinobi.story.MissionDef.Objective;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lightweight tests that do not need a Minecraft runtime. */
public class MissionDefTest {
    @Test
    void objectiveText_coversNewTypes() {
        MissionDef escort = new MissionDef("t", "T", "D", "d", Objective.ESCORT, "npc", 45,
                1, 10, 0, 0, "", 0, 0, false, "", "");
        MissionDef protect = new MissionDef("t2", "T2", "C", "d", Objective.PROTECT, "npc", 6,
                1, 10, 0, 0, "", 0, 0, false, "", "");
        assertTrue(escort.objectiveText().toLowerCase().contains("escort"));
        assertTrue(protect.objectiveText().toLowerCase().contains("protect"));
    }

    @Test
    void allObjectivesHaveText() {
        for (Objective o : Objective.values()) {
            MissionDef d = new MissionDef("x", "X", "D", "d", o, "any", 1,
                    1, 1, 0, 0, "", 0, 0, false, "", "");
            assertNotNull(d.objectiveText());
            assertFalse(d.objectiveText().isBlank());
        }
    }
}
