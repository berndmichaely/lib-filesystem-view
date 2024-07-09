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
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.System.Logger.Level.*;
import static java.util.Collections.unmodifiableSortedSet;

/**
 * Task for reading filesystem roots and directory entries.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class DirectoryReaderTask implements Runnable
{
	private static final System.Logger logger = System.getLogger(DirectoryReaderTask.class.getName());
	private final Consumer<DirectoryReaderTask.TaskResult> consumerResult;
	private final UserNodeConfiguration userNodeConfiguration;
	private final SortedSet<DirectoryEntry> sortedSet;
	private final @Nullable FileSystem fileSystem;
	private final boolean skipSingleRoot;
	private final @Nullable Path directory;

	/**
	 * Encapsulates the taks results.
	 */
	public static record TaskResult(
		/**
		 * The directory entries to be added.
		 */
		SortedSet<DirectoryEntry> sortedSet,
		/**
		 * Indicates, whether watch service must be started after directory entries
		 * have been added
		 */
		boolean startingWatchService)
		{
	}

	/**
	 * Constructor to read FileSystem roots.
	 *
	 * @param fileSystem            the FileSystem to read
	 * @param skipSingleRoot        true, if single FileSystem root should be
	 *                              skipped
	 * @param comparator            the file name comparator to be used
	 * @param userNodeConfiguration the UserNodeConfiguration instance
	 *                              corresponding to the directory node
	 */
	DirectoryReaderTask(Consumer<DirectoryReaderTask.TaskResult> consumerResult,
		FileSystem fileSystem, boolean skipSingleRoot,
		Comparator<DirectoryEntry> comparator, UserNodeConfiguration userNodeConfiguration)
	{
		this.consumerResult = consumerResult;
		this.fileSystem = fileSystem;
		this.skipSingleRoot = skipSingleRoot;
		this.sortedSet = new TreeSet<>(comparator);
		this.userNodeConfiguration = userNodeConfiguration;
		this.directory = null;
	}

	/**
	 * Constructor to read the entries of a directory.
	 *
	 * @param directory             the directory to read the contents from
	 * @param comparator            the file name comparator to be used
	 * @param userNodeConfiguration the UserNodeConfiguration instance
	 *                              corresponding to the directory node
	 */
	DirectoryReaderTask(Consumer<DirectoryReaderTask.TaskResult> consumerResult, Path directory,
		Comparator<DirectoryEntry> comparator, UserNodeConfiguration userNodeConfiguration)
	{
		this.consumerResult = consumerResult;
		this.directory = directory;
		this.sortedSet = new TreeSet<>(comparator);
		this.userNodeConfiguration = userNodeConfiguration;
		this.fileSystem = null;
		this.skipSingleRoot = false;
	}

	@Override
	public void run()
	{
		if (consumerResult != null)
		{
			final boolean startingWatchService = (this.directory != null) ?
				readDirectory(this.directory) : readFileSystem();
			final var result = new TaskResult(unmodifiableSortedSet(sortedSet), startingWatchService);
			consumerResult.accept(result);
		}
	}

	private boolean readFileSystem()
	{
		final var fs = this.fileSystem;
		if (fs != null)
		{
			if (fs.isOpen())
			{
				fs.getRootDirectories().forEach(path ->
					sortedSet.add(new DirectoryEntrySubDirectory(path)));
				final boolean doSkipSingleRoot = skipSingleRoot ?
					sortedSet.size() == 1 && sortedSet.first().getName().equals("/") : false;
				if (doSkipSingleRoot)
				{
					sortedSet.clear();
					return readDirectory(fs.getPath("/"));
				}
			}
			else
			{
				logger.log(WARNING, () -> getClass() +
					"#doUpdateDirectoryEntries(FileSystem) : FileSystem not open : " + fs);
			}
		}
		return false;
	}

	private boolean readDirectory(Path pathDirectory)
	{
		try (final Stream<Path> stream = Files.list(pathDirectory))
		{
			stream.forEach(path ->
			{
				final var entry = UserNodeConfigurationUtil.pathToDirectoryEntry(userNodeConfiguration, path);
				if (entry != null)
				{
					sortedSet.add(entry);
				}
			});
			return true;
		}
		catch (AccessDeniedException ex)
		{
			logger.log(INFO, () -> "Access denied for path »" + ex.getFile() + "«");
			return false;
		}
		catch (IOException ex)
		{
			logger.log(WARNING, ex);
			return false;
		}
	}
}
