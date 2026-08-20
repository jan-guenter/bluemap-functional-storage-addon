/*
 * SPDX-License-Identifier: MIT
 *
 * This installed-resource interpreter adapts the JSON scanning pattern from
 * the owner's MIT BlueMap Mekanism add-on. It contains no candidate assets or
 * candidate implementation code.
 */
package io.github.janguenter.bluemap.functionalstorage.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Exact installed {@code functionalstorage:framedblock} child-shell definitions. */
public final class ChildShellCatalog {

    public static final Set<String> MATERIAL_CHILDREN = Set.of(
            "front", "side", "front_divider"
    );
    public static final Set<String> NATIVE_CHILDREN = Set.of("tank", "display");

    private final Map<String, List<Child>> hosts;

    private ChildShellCatalog(Map<String, List<Child>> hosts) {
        this.hosts = Map.copyOf(hosts);
    }

    public static ChildShellCatalog load(Path functionalStorageJar) throws IOException {
        Map<String, List<Child>> parsed = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(functionalStorageJar.toFile())) {
            for (Map.Entry<String, FunctionalStorageProfile.Host> entry
                    : FunctionalStorageProfile.HOSTS.entrySet()) {
                String hostId = entry.getKey();
                String resource = "assets/functionalstorage/models/block/"
                        + entry.getValue().path() + ".json";
                ZipEntry modelEntry = zip.getEntry(resource);
                if (modelEntry == null || modelEntry.isDirectory()
                        || modelEntry.getSize() < 2 || modelEntry.getSize() > 64 * 1024) {
                    throw new IOException("missing or oversized framed model " + resource);
                }
                JsonObject root;
                try (InputStream input = zip.getInputStream(modelEntry);
                     InputStreamReader reader = new InputStreamReader(
                             input, StandardCharsets.UTF_8)) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonObject()) {
                        throw new IOException("framed model is not an object " + resource);
                    }
                    root = element.getAsJsonObject();
                }
                parsed.put(hostId, parseHost(hostId, entry.getValue(), root));
            }
        }
        if (!parsed.keySet().equals(FunctionalStorageProfile.HOST_IDS)) {
            throw new IOException("framed host roster mismatch");
        }
        return new ChildShellCatalog(parsed);
    }

    public List<Child> children(String hostId) {
        return hosts.get(hostId);
    }

    public boolean validateModels(ResourcePack resourcePack) {
        for (List<Child> children : hosts.values()) {
            for (Child child : children) {
                Model model = resourcePack.getModels().get(child.parent());
                if (model == null || model.getElements() == null
                        || model.getElements().length == 0) {
                    return false;
                }
                boolean hasFace = false;
                boolean hasMaterialReference = false;
                for (Element element : model.getElements()) {
                    if (element == null) {
                        continue;
                    }
                    for (Face face : element.getFaces().values()) {
                        hasFace = true;
                        String reference = face.getTexture().getReferenceName();
                        if (child.material() && child.name().equals(reference)) {
                            hasMaterialReference = true;
                        } else if (!child.material()
                                && textureKey(model, face, child.textures()) == null) {
                            return false;
                        }
                    }
                }
                if (!hasFace || (child.material() && !hasMaterialReference)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Set<Key> collectInstalledTextures(ResourcePack resourcePack) {
        Set<Key> result = new LinkedHashSet<>();
        for (List<Child> children : hosts.values()) {
            for (Child child : children) {
                Model model = resourcePack.getModels().get(child.parent());
                if (model == null || model.getElements() == null) {
                    continue;
                }
                for (Element element : model.getElements()) {
                    if (element == null) {
                        continue;
                    }
                    for (Face face : element.getFaces().values()) {
                        Key texture = textureKey(model, face, child.textures());
                        if (texture != null) {
                            result.add(texture);
                        }
                    }
                }
            }
        }
        return Set.copyOf(result);
    }

    private static List<Child> parseHost(
            String hostId,
            FunctionalStorageProfile.Host profile,
            JsonObject root
    ) throws IOException {
        if (!FunctionalStorageProfile.LOADER.equals(string(root.get("loader")))
                || !"minecraft:block/block".equals(string(root.get("parent")))) {
            throw new IOException("unexpected framed loader or parent for " + hostId);
        }
        JsonObject children = object(root.get("children"));
        if (children == null || !children.keySet().equals(profile.children())) {
            throw new IOException("unexpected child roster for " + hostId);
        }
        List<Child> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : children.entrySet()) {
            String name = entry.getKey();
            if (!(MATERIAL_CHILDREN.contains(name) || NATIVE_CHILDREN.contains(name))
                    || !entry.getValue().isJsonObject()) {
                throw new IOException("unsupported child " + hostId + '/' + name);
            }
            JsonObject child = entry.getValue().getAsJsonObject();
            if (child.has("loader") || child.has("children") || child.has("elements")) {
                throw new IOException("nested custom child " + hostId + '/' + name);
            }
            String parent = string(child.get("parent"));
            if (parent == null || !parent.startsWith("functionalstorage:block/")) {
                throw new IOException("unsupported child parent " + hostId + '/' + name);
            }
            Map<String, Key> textures = parseTextures(child.get("textures"));
            result.add(new Child(name, Key.parse(parent), textures,
                    MATERIAL_CHILDREN.contains(name)));
        }
        return List.copyOf(result);
    }

    private static Map<String, Key> parseTextures(JsonElement raw) throws IOException {
        if (raw == null) {
            return Map.of();
        }
        JsonObject object = object(raw);
        if (object == null || object.size() > 16) {
            throw new IOException("invalid inline child textures");
        }
        Map<String, Key> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String value = string(entry.getValue());
            if (entry.getKey().isBlank() || value == null || value.startsWith("#")) {
                throw new IOException("invalid inline child texture");
            }
            try {
                result.put(entry.getKey(), Key.parse(value));
            } catch (IllegalArgumentException exception) {
                throw new IOException("invalid inline child texture key", exception);
            }
        }
        return Map.copyOf(result);
    }

    private static Key textureKey(Model model, Face face, Map<String, Key> overrides) {
        String reference = face.getTexture().getReferenceName();
        if (reference != null && overrides.containsKey(reference)) {
            return overrides.get(reference);
        }
        ResourcePath<Texture> path = face.getTexture()
                .getTexturePath(model.getTextures()::get);
        return path;
    }

    private static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonElement element) {
        return element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString() ? element.getAsString() : null;
    }

    /** One ordinary installed inline child after strict structural decoding. */
    public record Child(
            String name,
            Key parent,
            Map<String, Key> textures,
            boolean material
    ) {

        public Child {
            textures = Map.copyOf(textures);
        }
    }
}
