package com.purplesweet;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;

public class PurpleSweetTrackerOverlay extends OverlayPanel
{
	private static final int MIN_WIDTH = 120;

	private final PurpleSweetTrackerPlugin plugin;
	private final PurpleSweetTrackerConfig config;

	@Inject
	private PurpleSweetTrackerOverlay(PurpleSweetTrackerPlugin plugin, PurpleSweetTrackerConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		// DETACHED makes the box freely draggable anywhere on screen, and RuneLite
		// remembers where the user drops it.
		setPosition(OverlayPosition.DETACHED);
		// Minimum width for the box (the user can still drag-resize it wider).
		setMinimumSize(MIN_WIDTH);
		panelComponent.setPreferredSize(new Dimension(150, 0));
		// A little extra vertical breathing room between the text rows.
		panelComponent.setGap(new Point(0, 3));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.displayStyle().showOverlay())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Purple Sweets")
			.color(config.overlayColor())
			.build());

		if (config.overlayShowEaten())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Eaten:")
				.right(QuantityFormatter.formatNumber(plugin.getLifetimeEaten()))
				.build());
		}

		if (config.overlayShowValue())
		{
			final long value = plugin.getLifetimeValue();
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Value:")
				.right(QuantityFormatter.quantityToStackSize(value) + " gp")
				.rightColor(PurpleSweetTrackerPlugin.valueColor(value))
				.build());
		}

		if (config.overlayShowPerHour())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Per hour:")
				.right(QuantityFormatter.formatNumber(plugin.getSweetsPerHour()))
				.build());
		}

		return super.render(graphics);
	}
}
