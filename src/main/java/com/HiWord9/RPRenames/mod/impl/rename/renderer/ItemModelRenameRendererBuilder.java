package com.HiWord9.RPRenames.mod.impl.rename.renderer;

import com.HiWord9.RPRenames.api.rename.renderer.builder.RenameRendererBuilder;
import com.HiWord9.RPRenames.mod.gui.widget.RPRWidget;
import com.HiWord9.RPRenames.mod.impl.rename.ItemModelRename;
import com.HiWord9.RPRenames.mod.impl.rename.renderer.builder.AcceptsFavoriteSupplier;
import com.HiWord9.RPRenames.mod.impl.rename.renderer.builder.AcceptsRPRWidget;

import java.util.function.Supplier;

public class ItemModelRenameRendererBuilder extends RenameRendererBuilder<ItemModelRename> implements AcceptsRPRWidget, AcceptsFavoriteSupplier {
    private Supplier<Boolean> favoriteSupplier = () -> false;
    private RPRWidget rprWidget = null;

    public ItemModelRenameRendererBuilder(ItemModelRename rename) {
        super(rename);
    }

    @Override
    public void setFavoriteSupplier(Supplier<Boolean> favoriteSupplier) {
        this.favoriteSupplier = favoriteSupplier;
    }

    @Override
    public void setRPRWidget(RPRWidget rprWidget) {
        this.rprWidget = rprWidget;
    }

    @Override
    public ItemModelRenameRenderer build() {
        return new ItemModelRenameRenderer(rename, rprWidget, favoriteSupplier);
    }
}
