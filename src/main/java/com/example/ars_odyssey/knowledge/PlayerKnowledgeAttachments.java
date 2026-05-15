package com.example.ars_odyssey.knowledge;

import com.example.ars_odyssey.ArsOdyssey;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlayerKnowledgeAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ArsOdyssey.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerKnowledgeData>> PLAYER_KNOWLEDGE =
            ATTACHMENTS.register("player_knowledge", () -> AttachmentType
                    .serializable(PlayerKnowledgeData::new)
                    .copyOnDeath()
                    .build());

    private PlayerKnowledgeAttachments() {
    }
}
