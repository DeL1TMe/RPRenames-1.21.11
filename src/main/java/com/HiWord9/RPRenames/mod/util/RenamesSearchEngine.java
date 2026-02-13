package com.HiWord9.RPRenames.mod.util;

import com.HiWord9.RPRenames.mod.RPRenames;
import com.HiWord9.RPRenames.mod.impl.renames_manager.favorite.FavoritesManager;
import com.HiWord9.RPRenames.mod.impl.rename.CITRename;
import com.HiWord9.RPRenames.api.rename.Rename;
import com.HiWord9.RPRenames.mod.impl.rename.ResourcePackRename;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RenamesSearchEngine {
    public static List<Rename> search(List<Rename> list, String match) {
        return search(list, match, RPRenames.favoritesManager);
    }

    public static List<Rename> search(List<Rename> list, String match, FavoritesManager favoritesManager) {
        LinkedHashSet<Rename> resultSet = new LinkedHashSet<>();
        if (match.startsWith("#")) {
            String matchTag = match.substring(1);
            String upMatchTag = up(matchTag);
            if (matchTag.contains(" ") && !upMatchTag.contains("REGEX:") && !upMatchTag.contains("IREGEX:")) {
                matchTag = matchTag.substring(0, matchTag.indexOf(" "));
            } else if (matchTag.contains(" #")) {
                matchTag = matchTag.substring(0, matchTag.indexOf(" #"));
            }

            String tagUp = beforeColon(upMatchTag);

            switch (tagUp) {
                case "REGEX", "IREGEX" -> handleRegex(list, matchTag, resultSet);
                case "PACK", "PACKNAME" -> handlePackName(list, matchTag, resultSet);
                case "ITEM" -> handleItem(list, matchTag, resultSet);
                case "STACKSIZE", "STACK", "SIZE" -> handleStackSize(list, matchTag, resultSet);
                case "DAMAGE" -> handleDamage(list, matchTag, resultSet);
                case "ENCH", "ENCHANT", "ENCHANTMENT" -> handleEnchantment(list, matchTag, resultSet);
                case "FAV", "FAVORITE" -> handleFavorite(list, favoritesManager, resultSet);
            }

            if (match.substring(1).contains(" ") && !upMatchTag.contains("REGEX:") && !upMatchTag.contains("IREGEX:")) {
                return search(new ArrayList<>(resultSet), match.substring(match.indexOf(" ") + 1), favoritesManager);
            } else if (match.substring(1).contains(" #")) {
                return search(new ArrayList<>(resultSet), match.substring(match.indexOf(" #") + 1), favoritesManager);
            }
        } else {
            if (match.startsWith("\\#")) {
                match = match.substring(1);
            }
            for (Rename r : list) {
                for(Text name : r.getNames()) {
                    if (up(name.getString()).contains(up(match))) {
                        resultSet.add(r);
                    }
                }
            }
        }
        return new ArrayList<>(resultSet);
    }

    private static void handleRegex(List<Rename> renames, String regexTag, Set<Rename> resultList) {
        boolean caseInsensitive = up(regexTag).startsWith("I");
        String regexText = afterColon(regexTag);

        try {
            Pattern pattern = caseInsensitive ?
                    Pattern.compile(regexText, Pattern.CASE_INSENSITIVE) :
                    Pattern.compile(regexText);

            for (Rename r : renames) {
                if (pattern.matcher(r.getName().getString()).matches()) {
                    resultList.add(r);
                }
            }
        } catch (PatternSyntaxException ignored) {} // invalid pattern -> ignore
    }

    private static void handlePackName(List<Rename> renames, String packNameTag, Set<Rename> resultList) {
        String packNameUp = up(afterColon(packNameTag));

        for (Rename r : renames) {
            if (!(r instanceof ResourcePackRename rpRename)) continue;
            if (rpRename.getPackName() == null) continue;

            if (up(rpRename.getPackName())
                    .replace(" ", "_")
                    .contains(packNameUp)
            ) resultList.add(rpRename);
        }
    }

    private static void handleItem(List<Rename> renames, String itemTag, Set<Rename> resultList) {
        String itemNameUp = up(afterColon(itemTag));

        for (Rename r : renames) {
            for (Item item : r.getItems()) {
                if (up(Util.idFromItem(item)).contains(itemNameUp)) {
                    resultList.add(r);
                    break;
                }
            }
        }
    }

    private static void handleStackSize(List<Rename> renames, String stackSizeTag, Set<Rename> resultList) {
        String stackSize = afterColon(stackSizeTag);
        if (!stackSize.matches("[0-9]{1,9}")) return;

        int stackSizeValue = Integer.parseInt(stackSize);

        for (Rename r : renames) {
            if (!(r instanceof CITRename citRename)) continue;

            if (PropertiesHelper.matchesRange(
                    stackSizeValue,
                    citRename.getOriginalStackSize()
            )) resultList.add(r);
        }
    }

    private static void handleDamage(List<Rename> renames, String damageTag, Set<Rename> resultList) {
        String damage = afterColon(damageTag);
        if (!damage.matches("[0-9]{1,9}")) return;

        int damageValue = Integer.parseInt(damage);
        for (Rename r : renames) {
            if (!(r instanceof CITRename citRename)) continue;

            String originalDamage = citRename.getOriginalDamage();
            for (Item item : citRename.getItems()) {
                if (PropertiesHelper.matchesRange(damageValue, originalDamage, item)) {
                    resultList.add(r);
                    break;
                }
            }
        }
    }

    private static void handleEnchantment(List<Rename> renames, String enchantmentTag, Set<Rename> resultList) {
        String enchantUp = up(afterColon(enchantmentTag));

        for (Rename r : renames) {
            if (!(r instanceof CITRename citRename)) continue;
            if (citRename.getEnchantment() == null) continue;

            var split = PropertiesHelper.splitList(citRename.getOriginalEnchantment());
            for (String s : split) {
                if (up(s).contains(enchantUp)) {
                    resultList.add(r);
                    break;
                }
            }
        }
    }

    private static void handleFavorite(List<Rename> renames, FavoritesManager favoritesManager, Set<Rename> resultList) {
        for (Rename r : renames) {
            if (favoritesManager.isFavoriteAny(r.getItems(), r.getName().getString())) {
                resultList.add(r);
            }
        }
    }

    private static @NotNull String afterColon(String matchTag) {
        int i;
        return (i = matchTag.indexOf(':')) == -1 ? "" : matchTag.substring(i + 1);
    }

    private static @NotNull String beforeColon(String matchTag) {
        int i;
        return (i = matchTag.indexOf(':')) == -1 ? matchTag : matchTag.substring(0, i);
    }

    private static @NotNull String up(String string) {
        return string.toUpperCase(Locale.ROOT);
    }
}
