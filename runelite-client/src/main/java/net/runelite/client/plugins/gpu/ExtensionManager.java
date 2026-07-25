package net.runelite.client.plugins.gpu;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.gpu.api.GpuApi;
import net.runelite.client.plugins.gpu.api.GpuExtension;

@RequiredArgsConstructor
class ExtensionManager implements GpuApi
{
	static class Extension
	{
		String owner;
		GpuExtension e;

		Extension(String owner, GpuExtension e)
		{
			this.owner = owner;
			this.e = e;
		}
	}

	private final GpuPlugin plugin;

	final List<Extension> extensions = new CopyOnWriteArrayList<>();

	@Override
	public void registerExtension(Plugin owner, GpuExtension extension)
	{
		extensions.add(new Extension(owner.getName(), extension));
		plugin.recompileShaders();
	}

	@Override
	public void unregisterExtension(Plugin owner, GpuExtension extension)
	{
		extensions.removeIf(e -> e.e == extension);
		plugin.recompileShaders();
	}

	void onContextCreate()
	{
		for (int i = 0; i < extensions.size(); ++i)
		{
			var e = extensions.get(i);
			e.e.onContextCreate();
		}
	}

	void onContextDestroy()
	{
		for (int i = 0; i < extensions.size(); ++i)
		{
			var e = extensions.get(i);
			e.e.onContextDestroy();
		}
	}

	boolean extensionDrawSkybox()
	{
		boolean ret = false;
		for (int i = 0; i < extensions.size(); ++i)
		{
			var e = extensions.get(i);
			ret |= e.e.drawSkybox();
		}
		return ret;
	}
}
