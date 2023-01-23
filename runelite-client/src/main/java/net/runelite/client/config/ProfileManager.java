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
import java.util.ArrayList;
import java.util.Collections;
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

	public List<ConfigProfile> listProfiles()
	{
		try (FileInputStream in = new FileInputStream(PROFILES);
			 FileChannel channel = in.getChannel()
		)
		{
			channel.lock();
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
			log.warn("unable to read profiles");
			return Collections.emptyList();
		}
	}

	public ConfigProfile createProfile(String name)
	{
		ConfigProfile profile = new ConfigProfile(System.currentTimeMillis());
		profile.setName(name);
		profile.setSync(false);
		loadEditSave(c -> c.add(profile));
		log.debug("Created profile {}", name);
		return profile;
	}

	private void loadEditSave(Consumer<List<ConfigProfile>> c)
	{
		List<ConfigProfile> profiles;
		try (FileInputStream in = new FileInputStream(PROFILES);
			 FileChannel channel = in.getChannel()
		)
		{
			channel.lock();
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

	public static File profileConfigFile(ConfigProfile profile) {
		return new File(PROFILES_DIR, profile.getName() + ".properties");
	}
}
