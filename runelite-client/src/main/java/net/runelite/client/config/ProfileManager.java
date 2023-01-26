package net.runelite.client.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Slf4j
public class ProfileManager
{
	private static final File PROFILES_DIR = new File(RuneLite.RUNELITE_DIR, "profiles2");
	private static final File PROFILES = new File(PROFILES_DIR, "profiles.json");

	private final Gson gson;

	static
	{
		PROFILES_DIR.mkdirs();
	}

	public List<ConfigProfile> listProfiles()
	{
		try (FileInputStream in = new FileInputStream(PROFILES);
			 FileChannel channel = in.getChannel()
		)
		{
			channel.lock(0L, Long.MAX_VALUE, true);
			return gson.fromJson(new InputStreamReader(in),
				new TypeToken<List<ConfigProfile>>()
				{
				}.getType());
		}
		catch (FileNotFoundException ex)
		{
			return Collections.emptyList();
		}
		catch (IOException e)
		{
			log.warn("unable to read profiles", e);
			return Collections.emptyList();
		}
	}

	public ConfigProfile createProfile(String name)
	{
		ConfigProfile profile = new ConfigProfile(System.nanoTime());
		profile.setName(name);
		profile.setSync(false);
		loadEditSave(c -> c.add(profile));
		log.debug("Created profile {}", profile);
		return profile;
	}

	public void removeProfile(ConfigProfile profile)
	{
		loadEditSave(c ->
		{
			if (c.removeIf(p -> p.getId() == profile.getId()))
			{
				log.debug("Removed profile {}", profile);
			}
		});
	}

	public void clone(ConfigProfile from, ConfigProfile to) throws IOException
	{
		// neither are internal profiles
		assert !from.getName().startsWith("$");
		assert !to.getName().startsWith("$");

		// this doesn't work if either from or to are active profiles -
		// either from might have pending changes or to will have an
		// associated ConfigData that isn't reloaded

		File fromFile = profileConfigFile(from);
		File toFile = profileConfigFile(to);

		log.debug("Cloning profile {} -> {}", from, to);

		// backup target properties
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		File backupFile = new File(toFile.getParentFile(), toFile.getName() + "." + dateFormat.format(new Date()));
		try
		{
			Files.copy(toFile.toPath(), backupFile.toPath());
		}
		catch (IOException ex)
		{
			throw new IOException("backup failed", ex);
		}

		// copy source properties to target
		Files.copy(fromFile.toPath(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	private void loadEditSave(Consumer<List<ConfigProfile>> c)
	{
		List<ConfigProfile> profiles;
		try (FileInputStream in = new FileInputStream(PROFILES);
			 FileChannel channel = in.getChannel()
		)
		{
			channel.lock(0L, Long.MAX_VALUE, true);
			profiles = gson.fromJson(new InputStreamReader(in),
				new TypeToken<List<ConfigProfile>>()
				{
				}.getType());
		}
		catch (FileNotFoundException ex)
		{
			profiles = new ArrayList<>();
		}
		catch (IOException e)
		{
			log.warn("unable to read profiles");
			profiles = new ArrayList<>();
		}

		c.accept(profiles);

		try
		{
			File tempFile = File.createTempFile("runelite_profiles", null, PROFILES.getParentFile());
			try (FileOutputStream out = new FileOutputStream(tempFile);
				 FileChannel channel = out.getChannel();
				 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
			{
				channel.lock();
				gson.toJson(profiles, writer);
				channel.force(true);
				// FileChannel.close() frees the lock
			}

			try
			{
				Files.move(tempFile.toPath(), PROFILES.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			catch (AtomicMoveNotSupportedException ex)
			{
				log.debug("atomic move not supported", ex);
				Files.move(tempFile.toPath(), PROFILES.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex)
		{
			log.error("unable to write profiles", ex);
		}
	}

	public static File profileConfigFile(ConfigProfile profile)
	{
		return new File(PROFILES_DIR, profile.getName() + ".properties");
	}
}
