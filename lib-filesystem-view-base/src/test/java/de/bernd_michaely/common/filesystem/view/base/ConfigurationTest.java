/*
 * Copyright 2024 Bernd Michaely (info@bernd-michaely.de).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.bernd_michaely.common.filesystem.view.base;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests class for Configuration.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class ConfigurationTest
{
	private void testNonNullness(Configuration configuration)
	{
		assertNotNull(configuration.fileSystem());
		assertNotNull(configuration.fileNameComparator());
		assertNotNull(configuration.userNodeConfiguration());
	}

	private boolean getRequestWatchServiceDefault()
	{
		return Configuration.getDefault().requestWatchService();
	}

	@Test
	public void testRequestWatchServiceDefault()
	{
		assertTrue(getRequestWatchServiceDefault());
	}

	@Test
	public void testBuilderDefault()
	{
		final Configuration configuration = Configuration.builder().build();
		testNonNullness(configuration);
		assertEquals(configuration, Configuration.getDefault());
		assertEquals(FileSystems.getDefault(), configuration.fileSystem());
		assertEquals(SimpleUserNodeConfiguration.getInstance(), configuration.userNodeConfiguration());
	}

	@Test
	public void testBuilderWithParameter()
	{
		try (final FileSystem fileSystem = Jimfs.newFileSystem())
		{
			final var comparator = ((Comparator<String>) String::compareTo).reversed();
			final var userNodeConfiguration = new UserNodeConfiguration()
			{
				@Override
				public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
				{
					return this;
				}
			};
			final boolean requestWatchService = !getRequestWatchServiceDefault();
			final Configuration configuration = Configuration.builder()
				.setFileSystem(fileSystem)
				.setFileNameComparator(comparator)
				.setRequestingWatchService(requestWatchService)
				.setUserNodeConfiguration(userNodeConfiguration)
				.build();
			testNonNullness(configuration);
			assertNotEquals(FileSystems.getDefault(), configuration.fileSystem());
			assertEquals(fileSystem, configuration.fileSystem());
			assertEquals(comparator, configuration.fileNameComparator());
			assertNotEquals(SimpleUserNodeConfiguration.getInstance(), configuration.userNodeConfiguration());
			assertEquals(userNodeConfiguration, configuration.userNodeConfiguration());
			assertEquals(requestWatchService, configuration.requestWatchService());
		}
		catch (IOException ex)
		{
			throw new IllegalStateException(ex);
		}
	}
}
