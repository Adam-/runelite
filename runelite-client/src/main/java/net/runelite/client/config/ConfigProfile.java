package net.runelite.client.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ConfigProfile
{
	@Getter
	private final long id;
	@Getter
	@Setter
	private String name;
	@Getter
	@Setter
	private boolean sync;
}
