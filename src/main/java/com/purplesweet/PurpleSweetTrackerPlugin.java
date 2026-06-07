package com.purplesweet;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
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

@Slf4j
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

	// For accurate counting: we only count a sweet when one actually leaves the inventory
	// shortly after an "Eat" click, so spam-clicking during the eat cooldown isn't counted.
	private static final int EAT_CLICK_WINDOW = 2; // game ticks
	private int sweetCount = -1;     // last known inventory purple-sweet count (-1 = unknown)
	private int eatClickTick = -100; // tick of the most recent purple-sweet "Eat" click

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
		sweetCount = -1;
		eatClickTick = -100;

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
		if (PURPLE_SWEET_IDS.contains(event.getItemId()) && "Eat".equalsIgnoreCase(event.getMenuOption()))
		{
			// Record intent only. The actual count happens when a sweet leaves the
			// inventory (onItemContainerChanged), so spam-clicking during the eat
			// cooldown doesn't over-count.
			eatClickTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		final ItemContainer inventory = event.getItemContainer();
		int newCount = 0;
		for (int id : PURPLE_SWEET_IDS)
		{
			newCount += inventory.count(id);
		}

		if (sweetCount < 0)
		{
			// First reading since login/enable — establish the baseline, don't count.
			sweetCount = newCount;
			return;
		}

		final int eaten = sweetCount - newCount;
		sweetCount = newCount;

		// Only count a decrease that follows a recent "Eat" click, so banking, dropping
		// or using sweets on something isn't mistaken for eating.
		if (eaten > 0 && client.getTickCount() - eatClickTick <= EAT_CLICK_WINDOW)
		{
			eatClickTick = -100; // consumed; a fresh click is required for the next eat
			registerEaten(eaten);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		final GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			// Re-baseline against the fresh inventory on next login.
			sweetCount = -1;
		}
	}

	private void registerEaten(int amount)
	{
		final int price = Math.max(itemManager.getItemPrice(ItemID.TRAIL_SWEETS), 0);

		lifetimeEaten += amount;
		lifetimeValue += (long) price * amount;
		sessionEaten += amount;
		sessionValue += (long) price * amount;

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
	private java.awt.image.BufferedImage maxStackImage()
	{
		return itemManager.getImage(ItemID.TRAIL_SWEETS, 100_000, false);
	}

	/**
	 * OSRS-style stack colouring for a gp value: yellow, then white, then green.
	 */
	static java.awt.Color valueColor(long value)
	{
		if (value >= 10_000_000)
		{
			return new java.awt.Color(0, 222, 90);   // green
		}
		if (value >= 100_000)
		{
			return java.awt.Color.WHITE;
		}
		return java.awt.Color.YELLOW;
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
		if (!config.playSound())
		{
			return;
		}

		final int volume = config.volume();
		if (volume <= 0)
		{
			return;
		}

		// Map the 0-100 slider onto the game's 0-127 sound-effect volume range.
		client.playSoundEffect(config.sound().getSoundId(), volume * 127 / 100);
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
