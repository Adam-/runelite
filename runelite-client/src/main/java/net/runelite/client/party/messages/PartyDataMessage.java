/*
 * Copyright (c) 2022, LlemonDuck <napkinorton@gmail.com>
 * Copyright (c) 2026, Adam <Adam@sigterm.info>
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
package net.runelite.client.party.messages;

import com.google.gson.annotations.SerializedName;
import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PartyDataMessage extends PartyMemberMessage
{
	// we use boxed fields to null out properties that haven't changed
	@SerializedName("n")
	private String characterName;

	@SerializedName("hc")
	private Integer healthCurrent;

	@SerializedName("hm")
	private Integer healthMax;

	@SerializedName("pc")
	private Integer prayerCurrent;

	@SerializedName("pm")
	private Integer prayerMax;

	@SerializedName("r")
	private Integer runEnergy;

	@SerializedName("s")
	private Integer specEnergy;

	@SerializedName("v")
	private Boolean vengeanceActive;

	@SerializedName("i")
	private int[] inv;

	@SerializedName("w")
	private int[] worn;

	@SerializedName("p")
	private Long prayers;
}
