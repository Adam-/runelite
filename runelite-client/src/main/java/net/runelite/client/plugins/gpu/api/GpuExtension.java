package net.runelite.client.plugins.gpu.api;

public abstract class GpuExtension
{
	public String getShaderExtension(String hook)
	{
		return null;
	}

	public boolean drawSkybox()
	{
		return false;
	}
}
