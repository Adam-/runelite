package net.runelite.client.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ConfigData
{
	private final File configPath;

	private final Properties properties = new Properties();
	private Map<String, String> patchChanges = new HashMap<>();

	ConfigData(File configPath)
	{
		this.configPath = configPath;

		try (FileInputStream fin = new FileInputStream(configPath))
		{
			properties.load(fin);
		}
		catch (FileNotFoundException ignored)
		{
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}

	String getProperty(String key)
	{
		return properties.getProperty(key);
	}

	Object setProperty(String key, String value)
	{
		patchChanges.put(key, value);
		return properties.setProperty(key, value);
	}

	Object unset(String key)
	{
		patchChanges.remove(key);
		return properties.remove(key);
	}

	Set<Object> keySet()
	{
		return properties.keySet();
	}

	Map<String, String> swapChanges()
	{
		if (patchChanges.isEmpty())
		{
			return Collections.emptyMap();
		}

		Map<String, String> p = patchChanges;
		patchChanges = new HashMap<>();
		return p;
	}

	void patch(Map<String, String> patch) throws IOException
	{
		// load + patch + store instead of just flushing the in-memory properties to disk so that
		// multiple clients editing one config data (such as rs profile config) get their data merged
		// correctly
		Properties tempProps = new Properties();
		try (FileInputStream in = new FileInputStream(configPath);
			 FileChannel channel = in.getChannel())
		{
			channel.lock(0L, Long.MAX_VALUE, true); // avoid file being clobbered while we load it
			tempProps.load(in);
		}
		catch (FileNotFoundException e)
		{
			log.debug("config file {} does not exist", configPath);
		}

		// apply patches
		for (Map.Entry<String, String> entry : patch.entrySet())
		{
			if (entry.getValue() == null)
			{
				tempProps.remove(entry.getKey());
			}
			else
			{
				tempProps.put(entry.getKey(), entry.getValue());
			}
		}

		File tempFile = File.createTempFile("runelite_config", null, configPath.getParentFile());
		try (FileOutputStream out = new FileOutputStream(tempFile);
			 FileChannel channel = out.getChannel();
			 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
		{
			channel.lock();
			tempProps.store(writer, "RuneLite configuration");
			channel.force(true);
			// FileChannel.close() frees the lock
		}

		// this will overwrite modifications to the file in between our load and move, but I think that is unavoidable
		// without either holding a separate lock file the entire time, or reusing the same file and holding a lock on
		// it the entire time.
		try
		{
			Files.move(tempFile.toPath(), configPath.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException ex)
		{
			log.debug("atomic move not supported", ex);
			Files.move(tempFile.toPath(), configPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
