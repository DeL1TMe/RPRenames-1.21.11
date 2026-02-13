package com.HiWord9.RPRenames.mod.util;

import com.HiWord9.RPRenames.mod.RPRenames;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Predicate;

public class ResourceStackHelper {
    public record ResourceEntry(Identifier id, Resource resource) {}

    public static List<ResourceEntry> findAllResources(
            ResourceManager resourceManager,
            String startingPath,
            Predicate<Identifier> allowedPathPredicate
    ) {
        var keyed = new LinkedHashMap<String, ResourceEntry>();
        boolean loadedFromPacks = false;

        try {
            loadedFromPacks = collectFromResourcePacks(resourceManager, startingPath, allowedPathPredicate, keyed);
        } catch (Exception e) {
            RPRenames.LOGGER.warn("Failed to enumerate resources from resource packs for {}", startingPath, e);
        }

        if (loadedFromPacks) {
            return new ArrayList<>(keyed.values());
        }

        try {
            flattenResourcesMap(resourceManager.findAllResources(startingPath, allowedPathPredicate), keyed);
        } catch (Exception e) {
            RPRenames.LOGGER.warn("Failed to call findAllResources for {}", startingPath, e);
        }

        return new ArrayList<>(keyed.values());
    }

    public static List<Resource> getAllResources(ResourceManager resourceManager, Identifier identifier) {
        return resourceManager.getAllResources(identifier);
    }

    private static boolean collectFromResourcePacks(
            ResourceManager resourceManager,
            String startingPath,
            Predicate<Identifier> allowedPathPredicate,
            Map<String, ResourceEntry> out
    ) {
        int before = out.size();
        resourceManager.streamResourcePacks().forEach(pack -> {
            Set<String> namespaces;
            try {
                namespaces = pack.getNamespaces(ResourceType.CLIENT_RESOURCES);
            } catch (Exception e) {
                RPRenames.LOGGER.debug("Failed to get namespaces from resource pack {}", pack.getId(), e);
                return;
            }

            for (String namespace : namespaces) {
                pack.findResources(
                        ResourceType.CLIENT_RESOURCES,
                        namespace,
                        startingPath,
                        (id, supplier) -> {
                            if (!allowedPathPredicate.test(id)) return;
                            putEntry(out, new ResourceEntry(id, new Resource(pack, supplier)));
                        }
                );
            }
        });
        return out.size() > before;
    }

    private static void flattenResourcesMap(Map<?, ?> map, Map<String, ResourceEntry> out) {
        for (Map.Entry<?, ?> rawEntry : map.entrySet()) {
            if (!(rawEntry.getKey() instanceof Identifier id)) {
                continue;
            }

            Object rawValue = rawEntry.getValue();
            if (rawValue instanceof Resource resource) {
                putEntry(out, new ResourceEntry(id, resource));
            } else if (rawValue instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Resource resource) {
                        putEntry(out, new ResourceEntry(id, resource));
                    }
                }
            }
        }
    }

    private static void putEntry(Map<String, ResourceEntry> out, ResourceEntry entry) {
        String normalizedPackId = ParserHelper.validatePackName(entry.resource().getPackId());
        String key = entry.id().toString() + "\n" + normalizedPackId;
        out.putIfAbsent(key, entry);
    }
}
