package com.purplesweet;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum InfoboxContent
{
	AMOUNT("Amount"),
	VALUE("Value");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
