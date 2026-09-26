package net.runelite.client.plugins.gpu;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.gpu.api.GpuApi;
import net.runelite.client.plugins.gpu.api.GpuExtension;
import net.runelite.client.plugins.gpu.api.PBOListener;

@Singleton
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
	private final PBOManager pboManager;

	final List<Extension> extensions = new CopyOnWriteArrayList<>();

	@Inject
	ExtensionManager(GpuPlugin plugin, PBOManager pboManager)
	{
		this.plugin = plugin;
		this.pboManager = pboManager;
	}

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

	@Override
	public void registerPBOListener(PBOListener listener)
	{
		pboManager.register(listener);
	}

	@Override
	public void unregisterPBOListener(PBOListener listener)
	{
		pboManager.unregister(listener);
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

	void onProgramCreate(int program)
	{
		for (int i = 0; i < extensions.size(); ++i)
		{
			var e = extensions.get(i);
			e.e.onProgramCreate(program);
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

	void onPostDrawTopLevel()
	{
		for (int i = 0; i < extensions.size(); ++i)
		{
			var e = extensions.get(i);
			e.e.onPostDrawToplevel();
		}
	}
}
