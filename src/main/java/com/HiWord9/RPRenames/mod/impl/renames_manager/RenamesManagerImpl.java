package com.HiWord9.RPRenames.mod.impl.renames_manager;

import com.HiWord9.RPRenames.api.rename.Rename;
import com.HiWord9.RPRenames.api.RenamesManager;
import net.minecraft.item.Item;

import java.util.*;
import java.util.stream.Collectors;

public class RenamesManagerImpl<R extends Rename> implements RenamesManager<R> {
    protected final Map<Item, List<R>> renames = new HashMap<>();
    private volatile List<R> allRenamesCache = null;

    public List<R> getAllRenames() {
        var cache = allRenamesCache;
        if (cache != null) {
            return cache;
        }

        cache = renames.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
        allRenamesCache = cache;
        return cache;
    }

    public List<R> getRenames(Item item) {
        var l = renames.get(item);
        return l == null ? List.of() : List.copyOf(l);
    }

    public boolean addRename(Item item, R rename) {
        boolean added = renames.computeIfAbsent(item, i -> new ArrayList<>()).add(rename);
        if (added) {
            allRenamesCache = null;
        }
        return added;
    }

    public boolean removeRename(Item item, R rename) {
        var l = renames.get(item);
        if (l != null) {
            boolean removed = l.remove(rename);
            if (removed) {
                allRenamesCache = null;
            }
            return removed;
        }
        return false;
    }

    public void clearRenames() {
        renames.clear();
        allRenamesCache = null;
    }
}
