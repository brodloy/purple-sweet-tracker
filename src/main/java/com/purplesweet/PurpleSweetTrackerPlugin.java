package com.purplesweet;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Purple Sweet Tracker",
	description = "Tracks the purple sweets you eat and their value, with a configurable sound, overlay and infobox.",
	tags = {"purple", "sweet", "sweets", "tracker", "counter", "kerching"}
)
public class PurpleSweetTrackerPlugin extends Plugin
{
	// Both in-game "Purple sweets" variants. TRAIL_SWEETS (10476) is the common Treasure
	// Trails reward that restores run energy; EASTER_EGG_2005_PURPLE (4561) is the older
	// "purple sweets" variant. Eating either is counted.
	static final Set<Integer> PURPLE_SWEET_IDS = ImmutableSet.of(
		ItemID.TRAIL_SWEETS,           // 10476
		ItemID.EASTER_EGG_2005_PURPLE  // 4561
	);

	@Inject
	private Client client;

	@Inject
	private PurpleSweetTrackerConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Notifier notifier;

	@Inject
	private PurpleSweetTrackerOverlay overlay;

	private PurpleSweetTrackerPanel panel;
	private NavigationButton navButton;
	private PurpleSweetInfoBox infoBox;

	private int lifetimeEaten;
	private long lifetimeValue;
	private int sessionEaten;
	private long sessionValue;
	private long sessionStartMillis;

	// Purple sweets share the standard food eat delay: one every 3 game ticks. We play the
	// sound and count on the click, then ignore further clicks until you could eat again,
	// so spam-clicking during the cooldown doesn't over-count.
	private static final int EAT_DELAY_TICKS = 3;
	private int lastEatTick = -100; // tick of the last counted eat

	@Provides
	PurpleSweetTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PurpleSweetTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		// Load persisted totals so the count survives a client restart; the session resets each launch.
		lifetimeEaten = config.lifetimeEaten();
		lifetimeValue = config.lifetimeValue();
		sessionEaten = 0;
		sessionValue = 0L;
		sessionStartMillis = System.currentTimeMillis();
		lastEatTick = -100;

		overlayManager.add(overlay);

		panel = new PurpleSweetTrackerPanel(this);
		refreshPanel();

		navButton = NavigationButton.builder()
			.tooltip("Purple Sweet Tracker")
			.icon(ImageUtil.loadImageResource(getClass(), "/com/purplesweet/panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		reconcileInfoBox();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
		}
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		infoBox = null;
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!PURPLE_SWEET_IDS.contains(event.getItemId()) || !"Eat".equalsIgnoreCase(event.getMenuOption()))
		{
			return;
		}

		final int tick = client.getTickCount();
		if (tick - lastEatTick < EAT_DELAY_TICKS)
		{
			// Still within the eat cooldown — the game won't consume another sweet yet,
			// so this click doesn't count (and makes no sound).
			return;
		}

		lastEatTick = tick;

		final int price = Math.max(itemManager.getItemPrice(ItemID.TRAIL_SWEETS), 0);
		lifetimeEaten++;
		lifetimeValue += price;
		sessionEaten++;
		sessionValue += price;

		config.lifetimeEaten(lifetimeEaten);
		config.lifetimeValue(lifetimeValue);

		playSound();
		maybeNotifyMilestone();
		refreshPanel();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PurpleSweetTrackerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		reconcileInfoBox();
		refreshPanel();
	}

	/**
	 * Periodically refresh the panel so the sweets/hour figure stays current even when idle.
	 */
	@Schedule(period = 5, unit = ChronoUnit.SECONDS)
	public void scheduledRefresh()
	{
		refreshPanel();
	}

	// ---------------------------------- Stats getters ----------------------------------

	int getLifetimeEaten()
	{
		return lifetimeEaten;
	}

	long getLifetimeValue()
	{
		return lifetimeValue;
	}

	int getSessionEaten()
	{
		return sessionEaten;
	}

	long getSessionValue()
	{
		return sessionValue;
	}

	// What the overlay/infobox should show, per the "Stats shown" config (lifetime vs session).
	int getDisplayedEaten()
	{
		return config.statsSource() == StatsSource.SESSION ? sessionEaten : lifetimeEaten;
	}

	long getDisplayedValue()
	{
		return config.statsSource() == StatsSource.SESSION ? sessionValue : lifetimeValue;
	}

	int getSweetsPerHour()
	{
		final long elapsedMillis = System.currentTimeMillis() - sessionStartMillis;
		if (elapsedMillis <= 0 || sessionEaten == 0)
		{
			return 0;
		}

		final double hours = elapsedMillis / 3_600_000.0;
		return (int) Math.round(sessionEaten / hours);
	}

	/**
	 * Reset all counters (lifetime and session). Invoked from the side panel's Reset button.
	 */
	void reset()
	{
		lifetimeEaten = 0;
		lifetimeValue = 0L;
		sessionEaten = 0;
		sessionValue = 0L;
		sessionStartMillis = System.currentTimeMillis();
		config.lifetimeEaten(0);
		config.lifetimeValue(0L);
		refreshPanel();
	}

	// ----------------------------------- Internals -----------------------------------

	private void reconcileInfoBox()
	{
		final boolean wanted = config.displayStyle().showInfobox();
		if (wanted && infoBox == null)
		{
			infoBox = new PurpleSweetInfoBox(maxStackImage(), this, config);
			infoBoxManager.addInfoBox(infoBox);
		}
		else if (!wanted && infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
	}

	/**
	 * The purple-sweet icon as it looks for a huge stack (100k) in the inventory — i.e.
	 * the fullest "pile" model. Passing stackable=false renders that model WITHOUT drawing
	 * the quantity number, so we get the big-stack graphic with no number on it.
	 */
	private BufferedImage maxStackImage()
	{
		return itemManager.getImage(ItemID.TRAIL_SWEETS, 100_000, false);
	}

	/**
	 * OSRS-style stack colouring for a gp value: yellow, then white, then green.
	 */
	static Color valueColor(long value)
	{
		if (value >= 10_000_000)
		{
			return new Color(0, 222, 90);   // green
		}
		if (value >= 100_000)
		{
			return Color.WHITE;
		}
		return Color.YELLOW;
	}

	private void maybeNotifyMilestone()
	{
		final int interval = config.milestoneInterval();
		if (interval <= 0 || lifetimeEaten % interval != 0)
		{
			return;
		}

		final String message = "Purple Sweet Tracker: you've eaten " + lifetimeEaten + " purple sweets!";
		// We're on the client thread here, so this chat message is safe to send directly.
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		notifier.notify(message);
	}

	private void playSound()
	{
		if (config.playSound())
		{
			// Plays at the player's in-game sound-effects volume (silent if they've muted it).
			client.playSoundEffect(config.sound().getSoundId());
		}
	}

	private void refreshPanel()
	{
		final PurpleSweetTrackerPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.update());
		}
	}
}
