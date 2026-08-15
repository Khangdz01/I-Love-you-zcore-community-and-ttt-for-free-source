package made4mischief.astatine.loader.api.event;

import net.minecraft.network.packet.Packet;

public class PacketEvent {
    private final Packet<?> packet;
    private final boolean receive;
    private boolean cancelled;

    public PacketEvent(Packet<?> packet, boolean receive) {
        this.packet = packet;
        this.receive = receive;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public boolean isReceive() {
        return this.receive;
    }

    public boolean isSend() {
        return !this.receive;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
