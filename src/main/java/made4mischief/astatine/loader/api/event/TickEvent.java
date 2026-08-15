package made4mischief.astatine.loader.api.event;

import net.minecraft.client.MinecraftClient;

public class TickEvent {
    private final MinecraftClient client;

    public TickEvent(MinecraftClient client) {
        this.client = client;
    }

    public MinecraftClient getClient() {
        return this.client != null ? this.client : MinecraftClient.getInstance();
    }
}
