package com.purplesweet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.SoundEffectID;

@Getter
@AllArgsConstructor
public enum SweetSound
{
	KERCHING("Kerching (GE coins)", SoundEffectID.GE_COIN_TINKLE),
	GE_DING("GE ding", SoundEffectID.GE_ADD_OFFER_DINGALING),
	COIN_COLLECT("Coin collect", SoundEffectID.GE_COLLECT_BLOOP),
	UI_BOOP("UI boop", SoundEffectID.UI_BOOP),
	TOWN_CRIER("Town crier bell", SoundEffectID.TOWN_CRIER_BELL_DING);

	private final String label;
	private final int soundId;

	@Override
	public String toString()
	{
		return label;
	}
}
