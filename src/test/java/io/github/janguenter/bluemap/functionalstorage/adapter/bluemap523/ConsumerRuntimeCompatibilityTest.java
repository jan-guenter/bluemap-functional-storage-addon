/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap523;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.janguenter.bluemap.addon.adapter.api.bluemap523.BlueMapRuntimeCompatibility;
import org.junit.jupiter.api.Test;

class ConsumerRuntimeCompatibilityTest {

    @Test
    void onlyTheAuditedFeatureBackportIdentityIsAccepted() {
        assertTrue(BlueMapRuntimeCompatibility.matches(
                "5.22-feature.backport-5.23-stateless-java-web-server-46",
                "7e07f4e74ec1e92a6ead9aa1e66054af3e133aac"
        ));
        assertFalse(BlueMapRuntimeCompatibility.matches(
                "5.22-agent.backport-5.22-mc1.21.1-2",
                "9be321df995a1103808621d529eb72773e719d4d"
        ));
        assertFalse(BlueMapRuntimeCompatibility.matches(
                "5.22-feature.backport-5.23-stateless-java-web-server-46",
                "0".repeat(40)
        ));
    }
}
