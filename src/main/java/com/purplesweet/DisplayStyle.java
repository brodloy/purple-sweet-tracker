package com.purplesweet;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DisplayStyle
{
	OVERLAY("Overlay box"),
	INFOBOX("Infobox"),
	BOTH("Both"),
	OFF("Off");

	private final String label;

	boolean showOverlay()
	{
		return this == OVERLAY || this == BOTH;
	}

	boolean showInfobox()
	{
		return this == INFOBOX || this == BOTH;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
