/*
 * Copyright (c) 2024, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.banktags;

import javax.annotation.Nullable;

/**
 * API for the bank tags plugin
 *
 * @see TagManager
 * @see net.runelite.client.plugins.banktags.tabs.TabManager
 * @see net.runelite.client.plugins.banktags.tabs.LayoutManager
 */
public interface BankTagsService
{
	/**
	 * Open the given bank tag. The tag may have an associated {@link net.runelite.client.plugins.banktags.tabs.TagTab},
	 * but this isn't required. If the tag has an associated {@link net.runelite.client.plugins.banktags.tabs.Layout},
	 * the layout will be applied.
	 *
	 * @param tag the tag name
	 */
	void openBankTag(String tag);

	/**
	 * Open the given {@link BankTag}. The bank tag is implemented by the caller.
	 * The tag may have an associated {@link net.runelite.client.plugins.banktags.tabs.TagTab},
	 * but this isn't required. If the tag has an associated {@link net.runelite.client.plugins.banktags.tabs.Layout},
	 * the layout will be applied.
	 *
	 * @param name the tag name
	 * @param bankTag the bank tag
	 */
	void openBankTag(String name, BankTag bankTag);

	/**
	 * Close the currently open {@link BankTag}.
	 */
	void closeBankTag();

	/**
	 * Get the currently open {@link BankTag}
	 * @return
	 */
	@Nullable
	BankTag getActiveTag();
}
