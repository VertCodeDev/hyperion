package dev.vertcode.hyperion.listener;

import java.util.UUID;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import org.bukkit.entity.Player;

import dev.vertcode.hyperion.api.model.behavior.builtin.RiderInput;
import dev.vertcode.hyperion.model.ModelInteractions;

/**
 * Note the movement keys every player holds, for seats that let their rider steer.
 *
 * <p>From 1.21.2 the client sends its keys as they change; before, it sends them every tick while
 * riding, as a forward and a sideways amount. Runs on the network threads and only writes a
 * concurrent map.
 */
public final class RiderInputListener implements PacketListener {

    private final ModelInteractions index;

    /**
     * Create the listener.
     *
     * @param index where the keys go
     */
    public RiderInputListener(final ModelInteractions index) {
        this.index = index;
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        UUID player = event.getUser().getUUID();
        if (player == null) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_INPUT) {
            WrapperPlayClientPlayerInput packet = new WrapperPlayClientPlayerInput(event);
            this.index.input(player, new RiderInput(
                    packet.isForward(),
                    packet.isBackward(),
                    packet.isLeft(),
                    packet.isRight(),
                    packet.isJump(),
                    packet.isShift(),
                    packet.isSprint()
            ));
        } else if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);
            Player online = event.getPlayer();
            this.index.input(player, new RiderInput(
                    packet.getForward() > 0f,
                    packet.getForward() < 0f,
                    packet.getSideways() > 0f,
                    packet.getSideways() < 0f,
                    packet.isJump(),
                    packet.isUnmount(),
                    online != null && online.isSprinting()
            ));
        }
    }
}
