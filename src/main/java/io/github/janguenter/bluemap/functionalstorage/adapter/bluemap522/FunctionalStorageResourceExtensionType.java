/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;

/** Resource-pack extension factory registered before resource loading. */
final class FunctionalStorageResourceExtensionType
        implements ResourcePack.Extension<FunctionalStorageResourceExtension> {

    private static final Key KEY =
            Key.parse("bluemap_functionalstorage:framed_shell_extension");
    private final FunctionalStorageRuntime runtime;

    FunctionalStorageResourceExtensionType(FunctionalStorageRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public FunctionalStorageResourceExtension create(ResourcePack pack) {
        return new FunctionalStorageResourceExtension(pack, runtime);
    }
}
