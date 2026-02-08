package com.HiWord9.RPRenames.mod.impl.renames_manager.updatable.parser.item_model;

import com.HiWord9.RPRenames.mod.impl.rename.ItemModelRename;
import com.HiWord9.RPRenames.mod.impl.renames_manager.updatable.parser.Parser;
import com.HiWord9.RPRenames.mod.impl.renames_manager.updatable.parser.item_model.condition.CustomNameCondition;
import com.HiWord9.RPRenames.mod.impl.renames_manager.updatable.parser.item_model.condition.ItemModelCondition;
import com.HiWord9.RPRenames.api.RenamesManager;
import com.HiWord9.RPRenames.mod.impl.renames_manager.updatable.parser.item_model.condition.SelectCondition;
import com.HiWord9.RPRenames.mod.util.Util;
import com.HiWord9.RPRenames.mod.RPRenames;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.item.property.select.ComponentSelectProperty;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ItemModelParser implements Parser {
    private final Map<Identifier, ItemAsset> itemAssets = new HashMap<>();

    public RenamesManager<? super ItemModelRename> renamesManager;

    public ItemModelParser(RenamesManager<? super ItemModelRename> renamesManager) {
        this.renamesManager = renamesManager;
    }

    public void updateItemAssets(Map<Identifier, ItemAsset> itemAssets) {
        this.itemAssets.clear();
        this.itemAssets.putAll(itemAssets);
    }

    @Override
    public void parse(ResourceManager resourceManager, Profiler profiler) {
        var renameDataList = ItemModelDataExplorer.getListMerged(itemAssets);
        var existingNamesByItem = new HashMap<Item, Set<List<Text>>>();

        renameDataList.forEach(data -> {
            var name = getName(data.applicableConditions);
            if (name == null) return;

            var rename = new ItemModelRename(
                    data.applicableConditions,
                    name,
                    data.items.toArray(new Item[]{})
            );

            for (var item : data.items) {
                existingNamesByItem
                        .computeIfAbsent(item, ignored -> new HashSet<>())
                        .add(name);
                renamesManager.addRename(item, rename);
            }
        });

        parseRawItemModels(resourceManager, existingNamesByItem);
    }

    private static List<Text> getName(Collection<ItemModelCondition.Applicable> conditions) {
        var renameCondition = getRenameCondition(conditions);
        if (renameCondition == null) return null;
        return renameCondition.value;
    }

    private static @Nullable SelectCondition<ComponentSelectProperty<Text>, Text> getRenameCondition(
            Collection<ItemModelCondition.Applicable> conditions
    ) {
        SelectCondition<ComponentSelectProperty<Text>, Text> renameCondition = null;
        for (ItemModelCondition condition : conditions) {
            var candidate = asCustomNameConditionOrNull(condition);
            if (candidate != null) {
                if (renameCondition == null) {
                    renameCondition = candidate;
                } else {
                    // todo handle multiple rename conditions; probably an error
                }
            }
        }
        return renameCondition;
    }

    @SuppressWarnings("unchecked")
    private static SelectCondition<ComponentSelectProperty<Text>, Text> asCustomNameConditionOrNull(
            ItemModelCondition condition
    ) {
        if (condition instanceof SelectCondition<?, ?> select
                && select.property instanceof ComponentSelectProperty<?>(ComponentType<?> componentType)
                && componentType.equals(DataComponentTypes.CUSTOM_NAME)
        ) return (SelectCondition<ComponentSelectProperty<Text>, Text>) select;

        return null;
    }

    private void parseRawItemModels(ResourceManager resourceManager, Map<Item, Set<List<Text>>> existingNamesByItem) {
        for (var entry : resourceManager.findResources("items", id -> id.getPath().endsWith(".json")).entrySet()) {
            var itemId = itemIdFromItemModelId(entry.getKey());
            if (itemId == null) continue;
            var item = Util.itemFromId(itemId);
            if (item == Items.AIR) continue;

            try (var reader = new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement root = Util.GSON.fromJson(reader, JsonElement.class);
                if (root == null || root.isJsonNull()) continue;

                var nameLists = new ArrayList<List<Text>>();
                collectCustomNameLists(root, nameLists);
                if (nameLists.isEmpty()) continue;

                var existingNames = existingNamesByItem.computeIfAbsent(item, ignored -> new HashSet<>());
                for (var names : nameLists) {
                    if (names == null || names.isEmpty()) continue;
                    if (existingNames.contains(names)) continue;

                    var rename = new ItemModelRename(
                            List.of(new CustomNameCondition(names)),
                            names,
                            item
                    );
                    renamesManager.addRename(item, rename);
                    existingNames.add(names);
                }
            } catch (Exception e) {
                RPRenames.LOGGER.warn("Failed to parse item model json {}", entry.getKey(), e);
            }
        }
    }

    private static @Nullable Identifier itemIdFromItemModelId(Identifier id) {
        String path = id.getPath();
        if (!path.startsWith("items/") || !path.endsWith(".json")) return null;
        String itemPath = path.substring("items/".length(), path.length() - ".json".length());
        if (itemPath.isEmpty()) return null;
        return Identifier.of(id.getNamespace(), itemPath);
    }

    private static void collectCustomNameLists(JsonElement element, List<List<Text>> out) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (isCustomNameSelect(obj)) {
                var casesElement = obj.get("cases");
                if (casesElement instanceof JsonArray cases) {
                    for (JsonElement caseElement : cases) {
                        if (!(caseElement instanceof JsonObject caseObj)) continue;
                        var whenElement = caseObj.get("when");
                        var names = parseWhenList(whenElement);
                        if (!names.isEmpty()) out.add(names);
                    }
                }
            }

            for (var entry : obj.entrySet()) {
                collectCustomNameLists(entry.getValue(), out);
            }
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectCustomNameLists(child, out);
            }
        }
    }

    private static boolean isCustomNameSelect(JsonObject obj) {
        String type = getStringOrNull(obj, "type");
        String property = getStringOrNull(obj, "property");
        String component = getStringOrNull(obj, "component");
        return "minecraft:select".equals(type)
                && "minecraft:component".equals(property)
                && "minecraft:custom_name".equals(component);
    }

    private static List<Text> parseWhenList(JsonElement whenElement) {
        var values = new ArrayList<Text>();
        if (whenElement == null || whenElement.isJsonNull()) return values;

        if (whenElement.isJsonArray()) {
            for (JsonElement element : whenElement.getAsJsonArray()) {
                var text = parseText(element);
                if (text != null) values.add(text);
            }
            return values;
        }

        var text = parseText(whenElement);
        if (text != null) values.add(text);
        return values;
    }

    private static @Nullable Text parseText(JsonElement element) {
        return TextCodecs.CODEC
                .parse(JsonOps.INSTANCE, element)
                .resultOrPartial(message -> RPRenames.LOGGER.warn("Failed to parse text component: {}", message))
                .orElse(null);
    }

    private static @Nullable String getStringOrNull(JsonObject obj, String key) {
        var el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }
}
