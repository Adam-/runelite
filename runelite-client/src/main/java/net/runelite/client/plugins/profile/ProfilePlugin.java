package net.runelite.client.plugins.profile;

import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import javax.inject.Inject;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Profiles",
	description = "Configuration profile management",
	loadWhenOutdated = true
)
public class ProfilePlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		ProfilePanel pluginPanel = new ProfilePanel();
		pluginPanel.rebuild();

		final BufferedImage icon = new BufferedImage(12,12,TYPE_INT_RGB);// ImageUtil.loadImageResource(getClass(), ICON_FILE);

		navigationButton = NavigationButton.builder()
			.tooltip("Profiles")
			.icon(icon)
			.priority(5)
			.panel(pluginPanel)
			.build();

		clientToolbar.addNavigation(navigationButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
	}
}
