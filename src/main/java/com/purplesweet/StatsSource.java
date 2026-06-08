package com.purplesweet;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StatsSource
{
	LIFETIME("Lifetime"),
	SESSION("Session");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
