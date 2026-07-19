package net.runelite.client.plugins.gpu.api;

import net.runelite.client.plugins.Plugin;

public interface GpuApi
{
	void registerExtension(Plugin owner, GpuExtension extension);

	void unregisterExtension(Plugin owner, GpuExtension extension);
}
