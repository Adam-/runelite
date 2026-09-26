package net.runelite.client.plugins.gpu;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.plugins.gpu.api.PBOListener;
import net.runelite.client.ui.ClientUI;
import net.runelite.rlawt.AWTContext;
import static net.runelite.client.plugins.gpu.GpuPlugin.scale;
import static org.lwjgl.opengl.GL33C.*;

@Singleton
class PBOManager
{
	private final Client client;
	private final ClientUI clientUI;
	private final CopyOnWriteArrayList<PBOListener> listeners = new CopyOnWriteArrayList<>();
	private final int[] pbos = new int[2];
	private final int[] widths = new int[2];
	private final int[] heights = new int[2];
	private final long[] syncs = new long[2];
	private int index;

	@Inject
	PBOManager(Client client, ClientUI clientUI)
	{
		this.client = client;
		this.clientUI = clientUI;
	}

	void register(PBOListener listener)
	{
		listeners.addIfAbsent(listener);
	}

	void unregister(PBOListener listener)
	{
		listeners.remove(listener);
	}

	void emitFramePbo(AWTContext awtContext)
	{
		if (listeners.isEmpty())
		{
			shutdown();
			return;
		}

		int width = client.getCanvasWidth();
		int height = client.getCanvasHeight();
		if (client.isStretchedEnabled())
		{
			Dimension dim = client.getStretchedDimensions();
			width = dim.width;
			height = dim.height;
		}

		final GraphicsConfiguration graphicsConfiguration = clientUI.getGraphicsConfiguration();
		final AffineTransform t = graphicsConfiguration.getDefaultTransform();
		width = scale(t.getScaleX(), width);
		height = scale(t.getScaleY(), height);

		boolean alloc = false;
		if (pbos[index] == 0)
		{
			pbos[index] = glGenBuffers();
			alloc = true;
		}
		if (syncs[index] != 0L)
		{
			// This frame was not ready when it was due, so drop it rather than wait.
			glDeleteSync(syncs[index]);
			syncs[index] = 0L;
			alloc = true; // Orphan the old storage so reusing this PBO cannot wait on an earlier transfer.
		}

		glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[index]);
		if (alloc || widths[index] != width || heights[index] != height)
		{
			glBufferData(GL_PIXEL_PACK_BUFFER, (long) width * height * Integer.BYTES, GL_STREAM_READ);
		}
		glReadBuffer(awtContext.getBufferMode());
		glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0L);
		glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
		syncs[index] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

		widths[index] = width;
		heights[index] = height;
		index ^= 1;

		if (syncs[index] != 0L)
		{
			int result = glClientWaitSync(syncs[index], 0, 0L);
			if (result == GL_ALREADY_SIGNALED || result == GL_CONDITION_SATISFIED)
			{
				glDeleteSync(syncs[index]);
				syncs[index] = 0L;
				for (PBOListener listener : listeners)
				{
					listener.onFrame(pbos[index], widths[index], heights[index]);
				}
			}
		}
	}

	void shutdown()
	{
		for (int i = 0; i < pbos.length; ++i)
		{
			if (syncs[i] != 0L)
			{
				glDeleteSync(syncs[i]);
				syncs[i] = 0L;
			}
			if (pbos[i] != 0)
			{
				glDeleteBuffers(pbos[i]);
				pbos[i] = 0;
			}
			widths[i] = 0;
			heights[i] = 0;
		}
		index = 0;
	}
}
