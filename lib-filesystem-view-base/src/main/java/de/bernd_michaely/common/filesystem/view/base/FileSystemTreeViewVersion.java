/*
 * Copyright 2026 Bernd Michaely (info@bernd-michaely.de).
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static java.util.function.Predicate.not;

/**
 * Class to retrieve the semantic version of this and related modules.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class FileSystemTreeViewVersion
{
	/**
	 * Protected constructor.
	 */
	protected FileSystemTreeViewVersion()
	{
	}

	/**
	 * Reads a version String from a resource file in the package from the given
	 * class. This method can be used by UI modules to read their own version
	 * resource files.
	 *
	 * @param aClass       class to locate the resource
	 * @param resourceName the name of the resource file
	 * @return the first non blank line of the given resource file
	 * @throws IllegalStateException if there are problems reading the resource
	 */
	protected static String readVersionResource(Class<?> aClass, String resourceName)
	{
		try (var inputStream = aClass.getResourceAsStream(resourceName))
		{
			if (inputStream != null)
			{
				try (var reader = new BufferedReader(new InputStreamReader(inputStream)))
				{
					return reader.lines().filter(not(String::isBlank)).findFirst().orElseThrow(
						() -> new IllegalStateException("resource »%s« invalid".formatted(resourceName))).trim();
				}
			}
			else
			{
				throw new IllegalStateException("resource »%s« not found".formatted(resourceName));
			}
		}
		catch (IOException ex)
		{
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * The name of the resource file containing the semantic version.
	 */
	protected static final String RESOURCE_NAME = "semantic_version.txt";
	private static @MonotonicNonNull String VERSION_MODULE_BASE;

	/**
	 * Returns the semantic version of the base module.
	 *
	 * @return a semantic version String for {@code lib-filesystem-view-base}
	 * @see <a href="https://semver.org">semver.org</a>
	 */
	public static String getBaseModuleVersion()
	{
		if (VERSION_MODULE_BASE == null)
		{
			VERSION_MODULE_BASE = readVersionResource(FileSystemTreeViewVersion.class, RESOURCE_NAME);
		}
		return VERSION_MODULE_BASE;
	}
}
