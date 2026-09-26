package net.runelite.client.plugins.gpu.api;

@FunctionalInterface
public interface PBOListener
{
	/**
	 * Called with a PBO containing a previously rendered frame. The pixels are
	 * stored bottom-up as {@code GL_BGRA}/{@code GL_UNSIGNED_INT_8_8_8_8_REV}.
	 * The PBO is owned by the GPU plugin and is only available for the duration
	 * of this callback.
	 *
	 * @param pbo OpenGL pixel buffer object
	 * @param width frame width in pixels
	 * @param height frame height in pixels
	 */
	void onFrame(int pbo, int width, int height);
}
