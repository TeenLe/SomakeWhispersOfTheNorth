package com.somake.wotn.client.dialogue;

import com.somake.wotn.network.CloseDialoguePayload;
import com.somake.wotn.network.ShowDialoguePayload;
import net.minecraft.client.Minecraft;

public final class DialogueClient {
    public static void show(ShowDialoguePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DialogueScreen screen
                && screen.sessionId().equals(payload.sessionId())) {
            screen.update(payload);
        } else {
            minecraft.setScreen(new DialogueScreen(payload));
        }
    }

    public static void close(CloseDialoguePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DialogueScreen screen
                && screen.sessionId().equals(payload.sessionId())) {
            screen.closeFromServer();
        }
    }

    private DialogueClient() {
    }
}
