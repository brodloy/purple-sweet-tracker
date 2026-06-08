package com.purplesweet;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(PurpleSweetTrackerConfig.GROUP)
public interface PurpleSweetTrackerConfig extends Config
{
	String GROUP = "purplesweettracker";

	@ConfigSection(
		name = "Display",
		description = "How the tracker is shown on screen.",
		position = 0
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Sound",
		description = "The sound played when you eat a purple sweet.",
		position = 1
	)
	String soundSection = "sound";

	@ConfigSection(
		name = "Notifications",
		description = "Milestone notifications.",
		position = 2
	)
	String notificationSection = "notifications";

	// ----------------------------------- Display -----------------------------------

	@ConfigItem(
		keyName = "displayStyle",
		name = "Display style",
		description = "Show a draggable overlay box, an item-timer style infobox, both, or nothing.",
		position = 1,
		section = displaySection
	)
	default DisplayStyle displayStyle()
	{
		return DisplayStyle.OVERLAY;
	}

	@ConfigItem(
		keyName = "infoboxContent",
		name = "Infobox type",
		description = "Whether the infobox shows the amount of sweets eaten or their GP value.",
		position = 2,
		section = displaySection
	)
	default InfoboxContent infoboxContent()
	{
		return InfoboxContent.AMOUNT;
	}

	@ConfigItem(
		keyName = "statsSource",
		name = "Stats shown",
		description = "Whether the overlay and infobox show your all-time totals or just this session.",
		position = 3,
		section = displaySection
	)
	default StatsSource statsSource()
	{
		return StatsSource.LIFETIME;
	}

	@ConfigItem(
		keyName = "overlayShowEaten",
		name = "Overlay: sweets eaten",
		description = "Show the 'Eaten' line on the overlay box.",
		position = 4,
		section = displaySection
	)
	default boolean overlayShowEaten()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayShowValue",
		name = "Overlay: value",
		description = "Show the 'Value' line on the overlay box.",
		position = 5,
		section = displaySection
	)
	default boolean overlayShowValue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayShowPerHour",
		name = "Overlay: per hour",
		description = "Show the 'Per hour' line on the overlay box.",
		position = 6,
		section = displaySection
	)
	default boolean overlayShowPerHour()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayColor",
		name = "Overlay title colour",
		description = "Colour of the overlay/infobox title text.",
		position = 7,
		section = displaySection
	)
	default Color overlayColor()
	{
		return new Color(170, 90, 230);
	}

	// ----------------------------------- Sound -----------------------------------

	@ConfigItem(
		keyName = "playSound",
		name = "Play sound",
		description = "Master toggle for the sound played when you eat a purple sweet.",
		position = 1,
		section = soundSection
	)
	default boolean playSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sound",
		name = "Sound",
		description = "Which sound to play when you eat a purple sweet.",
		position = 2,
		section = soundSection
	)
	default SweetSound sound()
	{
		return SweetSound.KERCHING;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "volume",
		name = "Volume",
		description = "Playback volume for the sound.",
		position = 3,
		section = soundSection
	)
	default int volume()
	{
		return 100;
	}

	// -------------------------------- Notifications --------------------------------

	@Range(min = 0)
	@ConfigItem(
		keyName = "milestoneInterval",
		name = "Milestone every",
		description = "Notify every this many sweets eaten (lifetime total). Set to 0 to disable.",
		position = 1,
		section = notificationSection
	)
	default int milestoneInterval()
	{
		return 100;
	}

	// ---- Persisted lifetime statistics (hidden; shown in the side panel/overlay) ----

	@ConfigItem(keyName = "lifetimeEaten", name = "", description = "", hidden = true)
	default int lifetimeEaten()
	{
		return 0;
	}

	@ConfigItem(keyName = "lifetimeEaten", name = "", description = "", hidden = true)
	void lifetimeEaten(int lifetimeEaten);

	@ConfigItem(keyName = "lifetimeValue", name = "", description = "", hidden = true)
	default long lifetimeValue()
	{
		return 0L;
	}

	@ConfigItem(keyName = "lifetimeValue", name = "", description = "", hidden = true)
	void lifetimeValue(long lifetimeValue);
}
