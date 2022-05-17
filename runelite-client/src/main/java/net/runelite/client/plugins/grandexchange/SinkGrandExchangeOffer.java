/*
 * Copyright (c) 2021, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.grandexchange;

import lombok.AllArgsConstructor;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

/**
 * A sunk grand exchange offer. These are always sell offers for 1 item that are either SELLING 1/1 or SOLD 1/1.
 */
@AllArgsConstructor
class SinkGrandExchangeOffer implements GrandExchangeOffer
{
	private final int id;
	private final int price;

	@Override
	public int getQuantitySold()
	{
		return 1;
	}

	@Override
	public int getItemId()
	{
		return id;
	}

	@Override
	public int getTotalQuantity()
	{
		return 1;
	}

	@Override
	public int getPrice()
	{
		return price;
	}

	@Override
	public int getSpent()
	{
		return price;
	}

	@Override
	public GrandExchangeOfferState getState()
	{
		// these are briefly SELLING 1/1 before moving to SOLD 1/1 - this move looks spoofed
		// so for simplicity am just using SOLD
		return GrandExchangeOfferState.SOLD;
	}
}
