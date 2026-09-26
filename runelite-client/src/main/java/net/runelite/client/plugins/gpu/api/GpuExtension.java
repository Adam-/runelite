package net.runelite.client.plugins.gpu.api;

public abstract class GpuExtension
{
	public abstract void onContextCreate();

	public abstract void onContextDestroy();

	public abstract void onProgramCreate(int program);

	public String injectShaderExtension(String hook)
	{
		return null;
	}

	public boolean drawSkybox()
	{
		return false;
	}

	public abstract void onPostDrawToplevel();
}
