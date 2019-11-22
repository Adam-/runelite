package net.runelite.client.plugins.chatleagueicons;

import java.awt.image.BufferedImage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.util.ImageUtil;

@Getter
@AllArgsConstructor
public enum LeagueIcon
{
	LEAGUE_POINTS("League"),
	LEAGUE_TWISTED("Twisted League");

	private String name;

	@Override
	public String toString()
	{
		return getName();
	}

	BufferedImage loadImage()
	{
		return ImageUtil.getResourceStreamFromClass(getClass(), this.name().toLowerCase() + ".png");
	}
}
