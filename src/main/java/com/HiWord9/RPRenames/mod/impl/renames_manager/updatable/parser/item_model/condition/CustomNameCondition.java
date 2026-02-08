package com.HiWord9.RPRenames.mod.impl.renames_manager.updatable.parser.item_model.condition;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public final class CustomNameCondition
        extends AbstractPropertyValueCondition<ComponentType<Text>, List<Text>>
        implements ItemModelCondition.Applicable
{
    public CustomNameCondition(List<Text> values) {
        super(DataComponentTypes.CUSTOM_NAME, values);
    }

    @Override
    public void apply(ItemStack stack) {
        if (value == null || value.isEmpty()) return;
        stack.set(DataComponentTypes.CUSTOM_NAME, value.getFirst());
    }
}
