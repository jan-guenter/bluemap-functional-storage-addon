/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import io.github.janguenter.bluemap.functionalstorage.model.ChildShellCatalog;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared exact-profile activation state and bounded diagnostics. */
final class FunctionalStorageRuntime {

    static final FunctionalStorageRuntime INSTANCE = new FunctionalStorageRuntime();
    private static final int MAX_DIAGNOSTICS = 8;

    private final AtomicBoolean active = new AtomicBoolean();
    private final AtomicInteger diagnostics = new AtomicInteger();
    private volatile ChildShellCatalog catalog;

    private FunctionalStorageRuntime() {
    }

    boolean active() {
        return active.get();
    }

    ChildShellCatalog catalog() {
        return catalog;
    }

    void activate(ChildShellCatalog loaded) {
        catalog = loaded;
        active.set(true);
    }

    void inactive(String reason) {
        active.set(false);
        catalog = null;
        report("inactive-" + reason);
    }

    void report(String reason) {
        if (diagnostics.incrementAndGet() <= MAX_DIAGNOSTICS) {
            System.err.println("BlueMap Functional Storage add-on: " + reason + '.');
        }
    }
}
