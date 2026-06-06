package com.purplesweet;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PurpleSweetTrackerTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PurpleSweetTrackerPlugin.class);
		RuneLite.main(args);
	}
}
