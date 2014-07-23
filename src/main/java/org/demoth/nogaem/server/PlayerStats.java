package org.demoth.nogaem.server;

import com.jme3.network.serializing.Serializable;

/**
 * @author demoth
 */
@Serializable
public class PlayerStats {
    public int axeCountMax = 3;
    public int axeCount;
    public int hpMax = 10;
    public int hp;
    public int mpMax = 5;
    public int mp;
    public int score;

    public void reset() {
        axeCount = axeCountMax;
        hp = hpMax;
        mp = mpMax;
    }

    public PlayerStats() {
    }
}
