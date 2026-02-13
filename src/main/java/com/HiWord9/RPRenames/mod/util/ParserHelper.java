package com.HiWord9.RPRenames.mod.util;

import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Properties;

public class ParserHelper {

    public static Properties getPropFromResource(Resource resource) throws IOException {
        Properties prop = new Properties();
        prop.load(resource.getInputStream());
        return prop;
    }

    public static String getFullPathFromIdentifier(String packName, Identifier identifier) {
        return validatePackName(packName) + "/assets/" + identifier.getNamespace() + "/" + identifier.getPath();
    }

    public static String validatePackName(String packName) {
        if (packName == null || packName.isBlank()) {
            return "unknown";
        }

        String normalized = packName;
        if (normalized.startsWith("file/")) {
            normalized = normalized.substring(5);
        }

        // Some loaders/mods expose virtual pack layers like "pack.zip#2".
        // We collapse layer suffixes so one physical pack does not generate duplicates.
        int layerSeparator = normalized.indexOf('#');
        if (layerSeparator > 0) {
            normalized = normalized.substring(0, layerSeparator);
        }

        return normalized;
    }
}
