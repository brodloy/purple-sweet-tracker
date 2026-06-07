package com.purplesweet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

/**
 * Side panel: live statistics only. All configuration lives in the plugin's
 * settings (the gear icon), to avoid splitting options across two UIs.
 */
class PurpleSweetTrackerPanel extends PluginPanel
{
	private static final Dimension MIN_SIZE = new Dimension(PluginPanel.PANEL_WIDTH, 220);
	private static final Color PURPLE = new Color(170, 90, 230);

	private final PurpleSweetTrackerPlugin plugin;

	private final JLabel lifetimeEaten = valueLabel();
	private final JLabel lifetimeValue = valueLabel();
	private final JLabel sessionEaten = valueLabel();
	private final JLabel sessionValue = valueLabel();
	private final JLabel perHour = valueLabel();

	private final JButton copyButton = new JButton("Copy stats");

	PurpleSweetTrackerPanel(PurpleSweetTrackerPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setMinimumSize(MIN_SIZE);

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JLabel title = new JLabel("Purple Sweet Tracker");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
		title.setForeground(PURPLE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(10));

		content.add(buildAllTimeCard());
		content.add(Box.createVerticalStrut(10));
		content.add(buildSessionCard());
		content.add(Box.createVerticalStrut(10));
		content.add(fill(buildButtonRow()));

		add(content, BorderLayout.NORTH);

		update();
	}

	private JPanel buildAllTimeCard()
	{
		final JPanel card = card();
		card.add(header("All-time"));
		card.add(statRow("Sweets eaten", lifetimeEaten));
		card.add(statRow("Value", lifetimeValue));
		return card;
	}

	private JPanel buildSessionCard()
	{
		final JPanel card = card();
		card.add(header("This session"));
		card.add(statRow("Sweets eaten", sessionEaten));
		card.add(statRow("Value", sessionValue));
		card.add(statRow("Per hour", perHour));
		return card;
	}

	private JPanel buildButtonRow()
	{
		final JPanel row = new JPanel(new GridLayout(1, 2, 8, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JButton reset = new JButton("Reset");
		reset.addActionListener(e -> plugin.reset());

		copyButton.setToolTipText("Copy your sweet stats to the clipboard");
		copyButton.addActionListener(e -> copyStats());

		row.add(reset);
		row.add(copyButton);
		return row;
	}

	private void copyStats()
	{
		final String text = "Purple Sweet Tracker\n"
			+ "All-time: " + QuantityFormatter.formatNumber(plugin.getLifetimeEaten()) + " eaten ("
			+ QuantityFormatter.formatNumber(plugin.getLifetimeValue()) + " gp)\n"
			+ "Session: " + QuantityFormatter.formatNumber(plugin.getSessionEaten()) + " eaten ("
			+ QuantityFormatter.formatNumber(plugin.getSessionValue()) + " gp)\n"
			+ "Per hour: " + QuantityFormatter.formatNumber(plugin.getSweetsPerHour());

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);

		// Brief confirmation, then restore the label.
		copyButton.setText("Copied!");
		final Timer timer = new Timer(1500, e -> copyButton.setText("Copy stats"));
		timer.setRepeats(false);
		timer.start();
	}

	/**
	 * Refresh the displayed numbers. Must be called on the Swing Event Dispatch Thread.
	 */
	void update()
	{
		lifetimeEaten.setText(QuantityFormatter.formatNumber(plugin.getLifetimeEaten()));
		lifetimeValue.setText(QuantityFormatter.formatNumber(plugin.getLifetimeValue()) + " gp");
		lifetimeValue.setForeground(PurpleSweetTrackerPlugin.valueColor(plugin.getLifetimeValue()));
		sessionEaten.setText(QuantityFormatter.formatNumber(plugin.getSessionEaten()));
		sessionValue.setText(QuantityFormatter.formatNumber(plugin.getSessionValue()) + " gp");
		perHour.setText(QuantityFormatter.formatNumber(plugin.getSweetsPerHour()));
	}

	// --------------------------------- Small helpers ---------------------------------

	private static JPanel card()
	{
		final JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		return card;
	}

	private static JLabel header(String text)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	private static JLabel valueLabel()
	{
		final JLabel label = new JLabel("0");
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		return label;
	}

	/** A caption on the left and a value on the right, full width. */
	private static JPanel statRow(String caption, JLabel value)
	{
		final JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		final JLabel cap = new JLabel(caption);
		cap.setForeground(Color.LIGHT_GRAY);
		row.add(cap, BorderLayout.WEST);
		row.add(value, BorderLayout.CENTER);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	/** Stretch a control to full width while keeping its natural height. */
	private static <T extends JComponent> T fill(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
		return component;
	}
}
