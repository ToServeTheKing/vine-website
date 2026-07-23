package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** TEMPORARY: proves a failing test actually stops the image being published. Removed immediately. */
class GateProbeTest {

    @Test
    void deliberatelyFails() {
        assertThat("the gate").isEqualTo("proved");
    }
}
