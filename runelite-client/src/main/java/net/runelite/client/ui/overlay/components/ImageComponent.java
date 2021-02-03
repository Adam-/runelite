/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
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
package net.runelite.client.ui.overlay.components;

import com.google.common.base.MoreObjects;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.client.util.ImageUtil;

@RequiredArgsConstructor
public class ImageComponent implements LayoutableRenderableEntity
{
	@NonNull
	private final BufferedImage image;

	private BufferedImage scaledImage;

	@Getter
	private final Rectangle bounds = new Rectangle();

	@Setter
	private Point preferredLocation = new Point();

	@Override
	public Dimension render(Graphics2D graphics)
	{
		BufferedImage i = MoreObjects.firstNonNull(scaledImage, image);
		graphics.drawImage(i, preferredLocation.x, preferredLocation.y, null);
		final Dimension dimension = new Dimension(image.getWidth(), image.getHeight());
		bounds.setLocation(preferredLocation);
		bounds.setSize(dimension);
		return dimension;
	}

	@Override
	public void setPreferredSize(Dimension preferredSize)
	{
		if (preferredSize == null || (preferredSize.width == image.getWidth() && preferredSize.height == image.getHeight()))
		{
			scaledImage = null;
		}
		else
		{
			scaledImage = ImageUtil.resizeImage(image, preferredSize.width, preferredSize.height);
		}
	}
}
