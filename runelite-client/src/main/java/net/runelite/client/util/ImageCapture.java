/*
 * Copyright (c) 2018, Lotto <https://github.com/devLotto>
 * Copyright (c) 2019, Alexsuperfly <https://github.com/Alexsuperfly>
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
package net.runelite.client.util;

import com.google.common.base.Strings;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.client.Notifier;
import static net.runelite.client.RuneLite.SCREENSHOT_DIR;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.ui.ClientUI;

@Slf4j
@Singleton
public class ImageCapture
{
	private static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	private final Client client;
	private final Notifier notifier;
	private final ClientUI clientUi;

	@Inject
	private ImageCapture(
		final Client client,
		final Notifier notifier,
		final ClientUI clientUi
	)
	{
		this.client = client;
		this.notifier = notifier;
		this.clientUi = clientUi;
	}

	/**
	 * Add the client frame to a screenshot
	 *
	 * @param image the screenshot
	 * @return
	 */
	public BufferedImage addClientFrame(Image image)
	{
		// create a new image, paint the client ui to it, and then draw the screenshot to that
		final AffineTransform transform = OSType.getOSType() == OSType.MacOS ? new AffineTransform() :
			clientUi.getGraphicsConfiguration().getDefaultTransform();

		// scaled client dimensions
		int clientWidth = getScaledValue(transform.getScaleX(), clientUi.getWidth());
		int clientHeight = getScaledValue(transform.getScaleY(), clientUi.getHeight());

		final BufferedImage screenshot = new BufferedImage(clientWidth, clientHeight, BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics = (Graphics2D) screenshot.getGraphics();
		AffineTransform originalTransform = graphics.getTransform();
		// scale g2d for the paint() call
		graphics.setTransform(transform);

		// Draw the client frame onto the screenshot
		try
		{
			SwingUtilities.invokeAndWait(() -> clientUi.paint(graphics));
		}
		catch (InterruptedException | InvocationTargetException e)
		{
			log.warn("unable to paint client UI on screenshot", e);
		}

		// Find the position of the canvas inside the frame
		final Point canvasOffset = clientUi.getCanvasOffset();
		final int gameOffsetX = getScaledValue(transform.getScaleX(), canvasOffset.getX());
		final int gameOffsetY = getScaledValue(transform.getScaleY(), canvasOffset.getY());

		// Draw the original screenshot onto the new screenshot
		graphics.setTransform(originalTransform); // the original screenshot is already scaled
		graphics.drawImage(image, gameOffsetX, gameOffsetY, null);
		graphics.dispose();

		return screenshot;
	}

	private static int getScaledValue(final double scale, final int value)
	{
		return (int) (value * scale + .5);
	}

	/**
	 * Save a screenshot to disk. And optionally send a notification and copy it to clipboard.
	 *
	 * @param screenshot screenshot
	 * @param fileName Filename to use, without file extension.
	 * @param subDir Directory within the player screenshots dir to store the captured screenshot to.
	 * @param notify Send a notification to the system tray when the image is captured.
	 * @param saveToClipboard Whether to also save the screenshot to clipboard
	 */
	public void saveScreenshot(
		BufferedImage screenshot,
		String fileName,
		@Nullable String subDir,
		boolean notify,
		boolean saveToClipboard
	)
	{
		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			// Prevent the screenshot from being captured
			log.debug("Login screenshot prevented");
			return;
		}

		File playerFolder;
		if (client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null)
		{
			String playerDir = client.getLocalPlayer().getName();
			RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
			if (profileType != RuneScapeProfileType.STANDARD)
			{
				playerDir += "-" + Text.titleCase(profileType);
			}

			if (!Strings.isNullOrEmpty(subDir))
			{
				playerDir += File.separator + subDir;
			}

			playerFolder = new File(SCREENSHOT_DIR, playerDir);
		}
		else
		{
			playerFolder = SCREENSHOT_DIR;
		}

		playerFolder.mkdirs();

		fileName += (fileName.isEmpty() ? "" : " ") + format(new Date());

		File screenshotFile = new File(playerFolder, fileName + ".png");
		// To make sure that screenshots don't get overwritten, check if file exists,
		// and if it does create file with same name and suffix.
		int i = 1;
		while (screenshotFile.exists())
		{
			screenshotFile = new File(playerFolder, fileName + String.format("(%d)", i++) + ".png");
		}

		try
		{
			ImageIO.write(screenshot, "PNG", screenshotFile);
		}
		catch (IOException ex)
		{
			log.error("error writing screenshot", ex);
			return;
		}

		if (saveToClipboard)
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			TransferableBufferedImage transferableBufferedImage = new TransferableBufferedImage(screenshot);
			clipboard.setContents(transferableBufferedImage, null);

			if (notify)
			{
				notifier.notify("A screenshot was saved and inserted into your clipboard!", TrayIcon.MessageType.INFO);
			}
		}
		else if (notify)
		{
			notifier.notify("A screenshot was saved to " + screenshotFile, TrayIcon.MessageType.INFO);
		}
	}

	/**
	 * Saves a screenshot to the screenshots folder as a PNG, and fires a ScreenshotTaken
	 * event afterward.
	 * @deprecated This method formerly could upload the image to Imgur, which is no longer supported. Use saveScreenshot instead.
	 * @see #saveScreenshot(BufferedImage, String, String, boolean, boolean)
	 */
	@Deprecated
	public void takeScreenshot(BufferedImage screenshot, String fileName, @Nullable String subDir, boolean notify, ImageUploadStyle imageUploadStyle)
	{
		saveScreenshot(screenshot, fileName, subDir, notify, imageUploadStyle == ImageUploadStyle.CLIPBOARD);
	}

	/**
	  * Saves a screenshot to the screenshots folder as a PNG, and fires a ScreenshotTaken
	  * event afterward.
	 * @deprecated This method formerly could upload the image to Imgur, which is no longer supported. Use saveScreenshot instead.
	 * @see #saveScreenshot(BufferedImage, String, String, boolean, boolean)
	 */
	@Deprecated
	public void takeScreenshot(BufferedImage screenshot, String fileName, boolean notify, ImageUploadStyle imageUploadStyle)
	{
		takeScreenshot(screenshot, fileName, null, notify, imageUploadStyle);
	}

	private static String format(Date date)
	{
		synchronized (TIME_FORMAT)
		{
			return TIME_FORMAT.format(date);
		}
	}
}
