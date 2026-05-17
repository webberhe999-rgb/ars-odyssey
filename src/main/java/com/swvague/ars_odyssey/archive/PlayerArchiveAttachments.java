package com.swvague.ars_odyssey.archive;

import com.swvague.ars_odyssey.ArsOdyssey;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlayerArchiveAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ArsOdyssey.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerArchiveData>> PLAYER_ARCHIVE =
            ATTACHMENTS.register("player_archive", () -> AttachmentType
                    .serializable(PlayerArchiveData::new)
                    .copyOnDeath()
                    .build());

    private PlayerArchiveAttachments() {
    }
}
