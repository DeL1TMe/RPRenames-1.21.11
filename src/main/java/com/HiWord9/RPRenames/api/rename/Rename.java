package com.HiWord9.RPRenames.api.rename;

import com.HiWord9.RPRenames.api.rename.renderer.builder.RenameRendererBuilder;
import com.HiWord9.RPRenames.api.rename.renderer.builder.SimpleRenameRendererBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Rename {
    protected final List<Text> name;
    protected final List<Item> items = new ArrayList<>();
    private Integer currentIndex = 0;

    public Rename(Text name, Item... items) {
        this.name = List.of(name);
        for (Item item : items) if (item != null) this.items.add(item);
    }

    public Rename(List<Text> name, Item... items) {
        this.name = name;
        for (Item item : items) if (item != null) this.items.add(item);
    }

    public Text getName() {
        return name.get(currentIndex);
    }

    public List<Text> getNames() {
        return name;
    }

    public void setCurrentIndex(Integer currentIndex) {
        this.currentIndex = currentIndex;
    }

    public Integer getCurrentIndex() {
        return currentIndex;
    }

    public List<Item> getItems() {
        return items;
    }

    public Item getItem() {
        return items.isEmpty() ? null : items.getFirst();
    }

    public ItemStack toStack() {
        return toStack(0);
    }

    public ItemStack toStack(int index) {
        ItemStack stack = new ItemStack(items.get(index));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.of(name.get(currentIndex)));
        return stack;
    }

    public boolean matchesStack(ItemStack stack) {
        if (!getItems().contains(stack.getItem())) return false;
        var customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        return customName != null && customName.equals(getName());
    }

    public RenameRendererBuilder<?> getNewRendererBuilder() {
        return new SimpleRenameRendererBuilder<>(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Rename rename
                && Objects.equals(name, rename.name)
                && Objects.equals(items, rename.items);
    }

    public boolean baseEquals(Rename rename) {
        return equals(rename);
    }
}
