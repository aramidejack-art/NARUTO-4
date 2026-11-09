package com.mayhem.shinobi;

import com.mayhem.shinobi.ability.BeastStages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BeastStagesTest {
    @Test
    void stageForBond_thresholds() {
        assertEquals(0, BeastStages.stageForBond(0, false));
        assertEquals(1, BeastStages.stageForBond(0, true));
        assertEquals(2, BeastStages.stageForBond(80, true));
        assertEquals(3, BeastStages.stageForBond(250, true));
        assertEquals(4, BeastStages.stageForBond(700, true));
        assertEquals(5, BeastStages.stageForBond(1600, true));
        assertEquals(5, BeastStages.stageForBond(99999, true));
    }

    @Test
    void names_matchStages() {
        assertEquals(6, BeastStages.NAMES.length);
        assertEquals("Full Avatar", BeastStages.NAMES[5]);
    }
}
