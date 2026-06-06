package com.purplesweet;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.QuantityFormatter;

/**
 * Item-timer style display: the purple-sweet icon with short text overlaid and a
 * full breakdown on hover.
 */
class PurpleSweetInfoBox extends InfoBox
{
	private final PurpleSweetTrackerPlugin plugin;
	private final PurpleSweetTrackerConfig config;

	PurpleSweetInfoBox(BufferedImage image, PurpleSweetTrackerPlugin plugin, PurpleSweetTrackerConfig config)
	{
		super(image, plugin);
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public String getText()
	{
		if (config.infoboxContent() == InfoboxContent.VALUE)
		{
			return QuantityFormatter.quantityToStackSize(plugin.getLifetimeValue());
		}
		return QuantityFormatter.quantityToStackSize(plugin.getLifetimeEaten());
	}

	@Override
	public Color getTextColor()
	{
		// Colour the value (green at high amounts); keep the amount white.
		if (config.infoboxContent() == InfoboxContent.VALUE)
		{
			return PurpleSweetTrackerPlugin.valueColor(plugin.getLifetimeValue());
		}
		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		return "Purple sweets eaten: " + QuantityFormatter.formatNumber(plugin.getLifetimeEaten())
			+ "</br>Value: " + QuantityFormatter.formatNumber(plugin.getLifetimeValue()) + " gp"
			+ "</br>Per hour: " + QuantityFormatter.formatNumber(plugin.getSweetsPerHour());
	}
}
