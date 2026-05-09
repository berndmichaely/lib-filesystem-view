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
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.FileSystemTreeViewVersion;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Class to retrieve the semantic version of this module.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class FileSystemTreeViewVersionFX extends FileSystemTreeViewVersion
{
	private static @MonotonicNonNull String VERSION_MODULE_FX;

	/**
	 * Protected constructor.
	 */
	protected FileSystemTreeViewVersionFX()
	{
	}

	/**
	 * Returns the semantic version of the fx module.
	 *
	 * @return a semantic version String for {@code lib-filesystem-view-fx}
	 * @see <a href="https://semver.org">semver.org</a>
	 */
	public static String getFxModuleVersion()
	{
		if (VERSION_MODULE_FX == null)
		{
			VERSION_MODULE_FX = readVersionResource(FileSystemTreeViewVersion.class, RESOURCE_NAME);
		}
		return VERSION_MODULE_FX;
	}
}
