/*
 * Copyright (c) 2018, TheStonedTurtle <https://github.com/TheStonedTurtle>
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
package net.runelite.api;

import lombok.Getter;

public enum NpcDeathAnimation
{
	ROCKSLUG(NpcID.ROCKSLUG, AnimationID.ROCKSLUG_DEATH),
	ROCKSLUG_422(NpcID.ROCKSLUG_422, AnimationID.ROCKSLUG_DEATH),
	// This death animation ID may be wrong, but who actually kills this?
	GIANT_ROCKSLUG(NpcID.GIANT_ROCKSLUG, AnimationID.ROCKSLUG_DEATH),

	SMALL_LIZARD(NpcID.SMALL_LIZARD, AnimationID.LIZARD_DEATH),
	SMALL_LIZARD_463(NpcID.SMALL_LIZARD_463, AnimationID.LIZARD_DEATH),
	DESERT_LIZARD(NpcID.DESERT_LIZARD, AnimationID.LIZARD_DEATH),
	DESERT_LIZARD_460(NpcID.DESERT_LIZARD_460, AnimationID.LIZARD_DEATH),
	DESERT_LIZARD_461(NpcID.DESERT_LIZARD_461, AnimationID.LIZARD_DEATH),
	LIZARD(NpcID.LIZARD, AnimationID.LIZARD_DEATH),

	ZYGOMITE(NpcID.ZYGOMITE, AnimationID.ZYGOMITE_DEATH),
	ZYGOMITE_474(NpcID.ZYGOMITE_474, AnimationID.ZYGOMITE_DEATH),
	ANCIENT_ZYGOMITE(NpcID.ANCIENT_ZYGOMITE, AnimationID.ZYGOMITE_DEATH),

	GARGOYLE(NpcID.GARGOYLE, AnimationID.GARGOYLE_DEATH),
	GARGOYLE_413(NpcID.GARGOYLE_413, AnimationID.GARGOYLE_DEATH),
	GARGOYLE_1543(NpcID.GARGOYLE_1543, AnimationID.GARGOYLE_DEATH),
	MARBLE_GARGOYLE(NpcID.MARBLE_GARGOYLE, AnimationID.MARBLE_GARGOYLE_DEATH),
	MARBLE_GARGOYLE_7408(NpcID.MARBLE_GARGOYLE_7408, AnimationID.MARBLE_GARGOYLE_DEATH);

	@Getter
	private final int npcId;

	@Getter
	private final int animationId;

	NpcDeathAnimation(int npcId, int animationId)
	{
		this.npcId = npcId;
		this.animationId = animationId;
	}

	public static int getAnimationId(int npcId)
	{
		for (NpcDeathAnimation o : values())
		{
			if (o.getNpcId() == npcId)
			{
				return o.getAnimationId();
			}
		}
		return -1;
	}
}
