package ru.totemswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class TotemSwapClient implements ClientModInitializer {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
            Identifier.of("totemswap", "main")
    );
    private static final KeyBinding SWAP_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.totemswap.swap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY)
    );

    private long swapRequestedNanos = Long.MIN_VALUE;

    @Override
    public void onInitializeClient() {
        SwapConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean swapRequested = false;
            while (SWAP_KEY.wasPressed()) {
                swapRequested = true;
            }

            if (client.player == null || client.currentScreen != null) {
                swapRequestedNanos = Long.MIN_VALUE;
                return;
            }

            if (swapRequested && swapRequestedNanos == Long.MIN_VALUE) {
                if (SwapConfig.delayMillis() == 0) {
                    swapTotem(client);
                } else {
                    swapRequestedNanos = System.nanoTime();
                }
            }

            if (swapRequestedNanos != Long.MIN_VALUE
                    && System.nanoTime() - swapRequestedNanos >= SwapConfig.delayMillis() * 1_000_000) {
                swapRequestedNanos = Long.MIN_VALUE;
                swapTotem(client);
            }
        });
    }

    private void swapTotem(MinecraftClient client) {
        var player = client.player;
        var inventory = player.getInventory();
        if (inventory.getStack(InventorySlots.OFFHAND).isOf(Items.TOTEM_OF_UNDYING)) {
            player.sendMessage(Text.translatable("message.totemswap.already_equipped"), true);
            return;
        }

        for (int inventoryIndex = 0; inventoryIndex < InventorySlots.MAIN_SIZE; inventoryIndex++) {
            if (!inventory.getStack(inventoryIndex).isOf(Items.TOTEM_OF_UNDYING)) {
                continue;
            }

            int handlerSlot = inventoryIndex < InventorySlots.HOTBAR_SIZE
                    ? inventoryIndex + InventorySlots.HOTBAR_HANDLER_OFFSET
                    : inventoryIndex;
            client.interactionManager.clickSlot(
                    player.playerScreenHandler.syncId,
                    handlerSlot,
                    InventorySlots.OFFHAND_BUTTON,
                    SlotActionType.SWAP,
                    player
            );
            return;
        }

        player.sendMessage(Text.translatable("message.totemswap.no_totem"), true);
    }

    private static final class InventorySlots {
        private static final int MAIN_SIZE = 36;
        private static final int HOTBAR_SIZE = 9;
        private static final int HOTBAR_HANDLER_OFFSET = 36;
        private static final int OFFHAND = 40;
        private static final int OFFHAND_BUTTON = 40;
    }
}