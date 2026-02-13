package com.HiWord9.RPRenames.mod.impl.renames_manager.updatable.parser;

import com.HiWord9.RPRenames.mod.RPRenames;
import com.HiWord9.RPRenames.mod.util.ParserHelper;
import com.HiWord9.RPRenames.mod.util.PropertiesHelper;
import com.HiWord9.RPRenames.mod.util.ResourceStackHelper;
import com.HiWord9.RPRenames.api.RenamesManager;
import com.HiWord9.RPRenames.api.rename.Rename;
import com.HiWord9.RPRenames.mod.impl.rename.CITRename;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.*;

public class CITParser implements Parser {
    private static final List<String> ROOTS = List.of("mcpatcher", "optifine", "citresewn");
    private final Map<Item, Set<CitKey>> seenByItem = new HashMap<>();

    public RenamesManager<? super CITRename> renamesManager;

    public CITParser(RenamesManager<? super CITRename> renamesManager) {
        this.renamesManager = renamesManager;
    }

    public void parse(ResourceManager resourceManager, Profiler profiler) {
        profiler.push("rprenames:collecting_cit_renames");
        seenByItem.clear();
        for (String root : ROOTS) {
            for (var entry : ResourceStackHelper.findAllResources(resourceManager, root + "/cit", s -> s.getPath().endsWith(".properties"))) {
                try {
                    String packName = ParserHelper.validatePackName(entry.resource().getPack().getId());
                    propertiesToRename(
                            ParserHelper.getPropFromResource(entry.resource()),
                            packName,
                            ParserHelper.getFullPathFromIdentifier(packName, entry.id())
                    );
                } catch (Exception e) {
                    RPRenames.LOGGER.error("Something went wrong while parsing CIT Renames", e);
                }
            }
        }
        profiler.pop();
    }

    private void propertiesToRename(Properties p, String packName, String path) {
        String matchItems = p.getProperty("matchItems");
        if (matchItems == null) matchItems = p.getProperty("items");
        if (matchItems == null) return;

        while (matchItems.endsWith(" ") || matchItems.endsWith("\t")) {
            matchItems = matchItems.substring(0, matchItems.length() - 1);
        }

        var items = itemsFromMatchItems(matchItems);
        if (items.isEmpty()) return;

        String customName = PropertiesHelper.getCustomName(p);
        if (customName == null) return;
        //todo lore

        String stackSizeProp = p.getProperty("stackSize");
        String firstStackSize = PropertiesHelper.getFirstValueInList(stackSizeProp == null ? "" : stackSizeProp);
        Integer stackSize = null;
        if (!firstStackSize.isEmpty()) {
            int i = Integer.parseInt(firstStackSize);
            if (i <= 64 && i > 0) {
                stackSize = i;
            }
        }

        String damageProp = p.getProperty("damage");
        CITRename.Damage damage = null;
        if (damageProp != null) {
            String firstDamage = PropertiesHelper.getFirstValueInList(damageProp);

            if (!firstDamage.isEmpty()) {
                try {
                    int d = Integer.parseInt(firstDamage.replace("%", ""));
                    damage = new CITRename.Damage(d, firstDamage.contains("%"));
                } catch (NumberFormatException ignored) {
                    RPRenames.LOGGER.warn("Could not get valid damage value {} for {}", firstDamage, path);
                }
            }
        }

        String enchantIdProp = p.getProperty("enchantmentIDs");
        Identifier enchantment = null;
        if (enchantIdProp != null) {
            String firstEnchantId = PropertiesHelper.getFirstValueInList(enchantIdProp);
            enchantment = Identifier.of(firstEnchantId);
        }

        String enchantLvlProp = p.getProperty("enchantmentLevels");
        String firstEnchantLvl = PropertiesHelper.getFirstValueInList(enchantLvlProp == null ? "" : enchantLvlProp);
        Integer enchantLvl = firstEnchantLvl.isEmpty() ? null : Integer.parseInt(firstEnchantLvl) <= 0 ? null : Integer.parseInt(firstEnchantLvl);

        String description = p.getProperty("$rprenames.description");
        if (description == null) description = p.getProperty("$rpr.description");
        if (description == null) description = p.getProperty("$description");

        CITRename rename = new CITRename(
                PropertiesHelper.getFirstName(customName, path),
                packName,
                path,
                stackSize,
                damage,
                enchantment,
                enchantLvl,
                p,
                description,
                items.toArray(new Item[]{})
        );
        CitKey key = CitKey.of(rename);

        for (Item item : items) {
            var seen = seenByItem.computeIfAbsent(item, ignored -> new HashSet<>());
            if (seen.add(key)) {
                renamesManager.addRename(item, rename);
            }
        }
    }

    private static List<String> splitMatchItems(String matchItems) {
        ArrayList<String> items = new ArrayList<>();
        int start = 0;
        while (start <= matchItems.length()) {
            String item = PropertiesHelper.getFirstValueInList(matchItems.substring(start));
            start += item.length() + 1;
            if (item.startsWith("minecraft:")) {
                item = item.substring(10);
            }
            if (item.equals("air")) {
                continue;
            }
            items.add(item);
        }
        return items;
    }

    private static List<Item> itemsFromMatchList(List<String> matchItemsList) {
        ArrayList<Item> items = new ArrayList<>();
        for (String matchItem : matchItemsList) {
            Item item = Registries.ITEM.get(Identifier.of(matchItem));
            if (item == Items.AIR) continue;
            items.add(item);
        }
        return items;
    }

    private static List<Item> itemsFromMatchItems(String matchItems) {
        return itemsFromMatchList(splitMatchItems(matchItems));
    }

    private record CitKey(
            String name,
            Integer stackSize,
            CITRename.Damage damage,
            Identifier enchantment,
            Integer enchantmentLevel,
            String packName
    ) {
        private static CitKey of(CITRename rename) {
            return new CitKey(
                    rename.getName().getString(),
                    rename.getStackSize(),
                    rename.getDamage(),
                    rename.getEnchantment(),
                    rename.getEnchantmentLevel(),
                    rename.getPackName()
            );
        }
    }
}
