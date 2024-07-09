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
package de.bernd_michaely.common.filesystem.view.base.ctrl;

import de.bernd_michaely.common.filesystem.view.base.UserNodeConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class for UserNodeConfiguration.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class UserNodeConfigurationUtil
{
	/**
	 * Applies path filters and returns a new DirectoryEntry or null. This method
	 * is to be used for node expansion and for watch service.
	 *
	 * @param unc  the UserNodeConfiguration corresponding to the path
	 * @param path the Path to encapsulate
	 * @return a new DirectoryEntry or null
	 * @author Bernd Michaely (info@bernd-michaely.de)
	 */
	static @Nullable
	DirectoryEntry pathToDirectoryEntry(UserNodeConfiguration unc, Path path)
	{
		final var linkOptions = unc.getLinkOptions();
		if (Files.isDirectory(path, linkOptions))
		{
			return unc.isCreatingNodeForDirectory(path) ?
				new DirectoryEntrySubDirectory(path) : null;
		}
		else if (Files.isRegularFile(path, linkOptions))
		{
			return unc.isCreatingNodeForFile(path) ?
				new DirectoryEntryRegularFile(path, unc) : null;
		}
		else
		{
			return null;
		}
	}
}
