/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.BlueMap;

/** Exact BlueMap 5.22 runtime identities whose internal ABI was audited. */
public final class AdapterCompatibility {

    private static final String UPSTREAM_VERSION = "5.22";
    private static final String UPSTREAM_COMMIT =
            "fe5115d5548a30d34175b8e0449aaca280af199f";
    private static final String BACKPORT_VERSION =
            "5.22-agent.backport-5.22-mc1.21.1-2";
    private static final String BACKPORT_COMMIT =
            "9be321df995a1103808621d529eb72773e719d4d";

    private AdapterCompatibility() {
    }

    public static boolean currentRuntimeSupported() {
        return supported(BlueMap.VERSION, BlueMap.GIT_HASH);
    }

    static boolean supported(String version, String commit) {
        return (UPSTREAM_VERSION.equals(version) && UPSTREAM_COMMIT.equals(commit))
                || (BACKPORT_VERSION.equals(version) && BACKPORT_COMMIT.equals(commit));
    }
}
