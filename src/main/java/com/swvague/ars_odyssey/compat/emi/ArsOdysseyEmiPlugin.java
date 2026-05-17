package com.swvague.ars_odyssey.compat.emi;

import com.swvague.ars_odyssey.registry.ModRegistry;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class ArsOdysseyEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(ModRegistry.ARCHIVE_TERMINAL_MENU.get(), new ArchiveEmiRecipeHandler());
    }
}
