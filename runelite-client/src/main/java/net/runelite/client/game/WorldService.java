package net.runelite.client.game;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldClient;
import net.runelite.http.api.worlds.WorldResult;

@Singleton
@Slf4j
public class WorldService
{
	private static final int WORLD_FETCH_TIMER = 10; // minutes

	private final ScheduledExecutorService scheduledExecutorService;
	private final WorldClient worldClient;

	@Getter
	@Nullable
	private WorldResult worlds;

	@Inject
	private WorldService(ScheduledExecutorService scheduledExecutorService, WorldClient worldClient)
	{
		this.scheduledExecutorService = scheduledExecutorService;
		this.worldClient = worldClient;

		scheduledExecutorService.scheduleWithFixedDelay(this::tick, 0, WORLD_FETCH_TIMER, TimeUnit.MINUTES);
	}

	private void tick()
	{
		log.debug("Fetching worlds");

		try
		{
			WorldResult worldResult = worldClient.lookupWorlds();
			worldResult.getWorlds().sort(Comparator.comparingInt(World::getId));
			worlds = worldResult;
		}
		catch (IOException ex)
		{
			log.warn("Error looking up worlds", ex);
		}
	}

	public void refresh()
	{
		scheduledExecutorService.execute(this::tick);
	}
}
