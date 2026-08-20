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
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Rotation;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.TextureVariable;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
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
    private final Map<Key, ModelStructure> modelStructures;

    private ChildShellCatalog(
            Map<String, List<Child>> hosts,
            Map<Key, ModelStructure> modelStructures
    ) {
        this.hosts = Map.copyOf(hosts);
        this.modelStructures = Map.copyOf(modelStructures);
    }

    public static ChildShellCatalog load(Path functionalStorageJar) throws IOException {
        Map<String, List<Child>> parsed = new LinkedHashMap<>();
        Map<Key, ModelStructure> structures;
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
            structures = loadModelStructures(zip, parsed);
        }
        if (!parsed.keySet().equals(FunctionalStorageProfile.HOST_IDS)) {
            throw new IOException("framed host roster mismatch");
        }
        return new ChildShellCatalog(parsed, structures);
    }

    public List<Child> children(String hostId) {
        return hosts.get(hostId);
    }

    public boolean validateModels(ResourcePack resourcePack) {
        for (List<Child> children : hosts.values()) {
            for (Child child : children) {
                Model model = resourcePack.getModels().get(child.parent());
                if (!matchesInstalledModelStructure(child.parent(), model)) {
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
                        }
                        Key textureKey = textureKey(model, face, child.textures());
                        Texture texture = textureKey == null
                                ? null : resourcePack.getTextures().get(textureKey);
                        if (!installedTextureAllowed(texture)) {
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

    boolean matchesInstalledModelStructure(Key key, Model model) {
        ModelStructure expected = modelStructures.get(key);
        return expected != null && expected.equals(ModelStructure.from(model));
    }

    static boolean installedTextureAllowed(Texture texture) {
        return texture != null;
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

    private static Map<Key, ModelStructure> loadModelStructures(
            ZipFile zip,
            Map<String, List<Child>> hosts
    ) throws IOException {
        Set<Key> parents = new LinkedHashSet<>();
        for (List<Child> children : hosts.values()) {
            for (Child child : children) {
                parents.add(child.parent());
            }
        }
        Map<Key, ModelStructure> structures = new LinkedHashMap<>();
        for (Key parent : parents) {
            String resource = "assets/" + parent.getNamespace() + "/models/"
                    + parent.getValue() + ".json";
            ZipEntry entry = zip.getEntry(resource);
            if (entry == null || entry.isDirectory()
                    || entry.getSize() < 2 || entry.getSize() > 256 * 1024) {
                throw new IOException("missing or oversized child model " + resource);
            }
            Model model;
            try (InputStream input = zip.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(
                         input, StandardCharsets.UTF_8)) {
                model = ResourcesGson.INSTANCE.fromJson(reader, Model.class);
            } catch (RuntimeException exception) {
                throw new IOException("invalid child model " + resource, exception);
            }
            ModelStructure structure = ModelStructure.from(model);
            if (structure == null || structures.put(parent, structure) != null) {
                throw new IOException("unsupported child model structure " + resource);
            }
        }
        if (!structures.keySet().equals(parents)) {
            throw new IOException("child model structure roster mismatch");
        }
        return Map.copyOf(structures);
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

    private record ModelStructure(
            String parent,
            boolean ambientOcclusion,
            Map<String, TextureStructure> textures,
            List<ElementStructure> elements
    ) {

        private static ModelStructure from(Model model) {
            if (model == null || model.getElements() == null
                    || model.getElements().length == 0) {
                return null;
            }
            Map<String, TextureStructure> textures = new LinkedHashMap<>();
            for (Map.Entry<String, TextureVariable> entry
                    : model.getTextures().entrySet()) {
                TextureStructure texture = TextureStructure.from(entry.getValue());
                if (entry.getKey() == null || texture == null
                        || textures.put(entry.getKey(), texture) != null) {
                    return null;
                }
            }
            List<ElementStructure> elements = new ArrayList<>();
            for (Element element : model.getElements()) {
                ElementStructure structure = ElementStructure.from(element);
                if (structure == null) {
                    return null;
                }
                elements.add(structure);
            }
            return new ModelStructure(
                    model.getParent() == null
                            ? null : model.getParent().getFormatted(),
                    model.isAmbientocclusion(),
                    Map.copyOf(textures),
                    List.copyOf(elements)
            );
        }
    }

    private record ElementStructure(
            Vector3Structure from,
            Vector3Structure to,
            RotationStructure rotation,
            boolean shade,
            int lightEmission,
            Map<Direction, FaceStructure> faces
    ) {

        private static ElementStructure from(Element element) {
            if (element == null || element.getFrom() == null || element.getTo() == null
                    || element.getRotation() == null || element.getFaces() == null
                    || element.getFaces().isEmpty()) {
                return null;
            }
            Map<Direction, FaceStructure> faces = new LinkedHashMap<>();
            for (Map.Entry<Direction, Face> entry : element.getFaces().entrySet()) {
                FaceStructure face = FaceStructure.from(entry.getValue());
                if (entry.getKey() == null || face == null
                        || faces.put(entry.getKey(), face) != null) {
                    return null;
                }
            }
            return new ElementStructure(
                    Vector3Structure.from(element.getFrom()),
                    Vector3Structure.from(element.getTo()),
                    RotationStructure.from(element.getRotation()),
                    element.isShade(),
                    element.getLightEmission(),
                    Map.copyOf(faces)
            );
        }
    }

    private record RotationStructure(
            Vector3Structure origin,
            float x,
            float y,
            float z,
            String axis,
            float angle,
            boolean rescale
    ) {

        private static RotationStructure from(Rotation rotation) {
            return new RotationStructure(
                    Vector3Structure.from(rotation.getOrigin()),
                    rotation.getX(), rotation.getY(), rotation.getZ(),
                    rotation.getAxis().name(), rotation.getAngle(), rotation.isRescale()
            );
        }
    }

    private record FaceStructure(
            Vector4Structure uv,
            TextureStructure texture,
            Direction cullface,
            int rotation,
            int tintIndex
    ) {

        private static FaceStructure from(Face face) {
            if (face == null || face.getUv() == null || face.getTexture() == null) {
                return null;
            }
            TextureStructure texture = TextureStructure.from(face.getTexture());
            return texture == null ? null : new FaceStructure(
                    Vector4Structure.from(face.getUv()), texture, face.getCullface(),
                    face.getRotation(), face.getTintindex()
            );
        }
    }

    private record TextureStructure(String reference, String path) {

        private static TextureStructure from(TextureVariable texture) {
            if (texture == null) {
                return null;
            }
            String reference = texture.getReferenceName();
            ResourcePath<Texture> path = texture.getTexturePath();
            if (reference != null) {
                return new TextureStructure(reference, null);
            }
            return path == null
                    ? null : new TextureStructure(null, path.getFormatted());
        }
    }

    private record Vector3Structure(float x, float y, float z) {

        private static Vector3Structure from(
                com.flowpowered.math.vector.Vector3f vector
        ) {
            return new Vector3Structure(
                    vector.getX(), vector.getY(), vector.getZ()
            );
        }
    }

    private record Vector4Structure(float x, float y, float z, float w) {

        private static Vector4Structure from(
                com.flowpowered.math.vector.Vector4f vector
        ) {
            return new Vector4Structure(
                    vector.getX(), vector.getY(), vector.getZ(), vector.getW()
            );
        }
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
