package net.eltown.apiserver.components.handler.player.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class SyncPlayer {

    private String inventory, armorInventory, enderchest, foodLevel, saturation, exhaustion, selectedSlot, potionEffects, totalExperience, level, experience, gamemode, flying;
    private boolean canSync;

}
