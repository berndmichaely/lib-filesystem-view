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

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.AccessDeniedException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.System.Logger.Level.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Class to describe the configuration and availability of a watch service.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class WatchServiceCtrl implements Closeable
{
	private static final Logger logger = System.getLogger(WatchServiceCtrl.class.getName());
	private final @Nullable WatchService watchService;
	private final @Nullable FileSystem fileSystemWatched;
	private final @Nullable Thread threadWatchService;
	private final @Nullable SortedMap<Path, WatchableConfig> callbacks;

	private record WatchableConfig(
		WatchKey watchKey,
		BiConsumer<WatchEvent.Kind<?>, @Nullable Path> callback)
		{
	}

	/**
	 * Creates a new WatchService configuration. Optimistically sets the watch
	 * service initially as available.
	 *
	 * @param requested  true to request a WatchService
	 * @param fileSystem the FileSystem to be watched
	 */
	WatchServiceCtrl(boolean requested, FileSystem fileSystem)
	{
		WatchService _watchService;
		try
		{
			_watchService = requested ? fileSystem.newWatchService() : null;
		}
		catch (UnsupportedOperationException ex)
		{
			_watchService = null;
			logger.log(WARNING, () ->
				"WatchService requested, but not available, for FileSystem: »%s«".formatted(fileSystem));
		}
		catch (IOException ex)
		{
			_watchService = null;
			logger.log(WARNING, ex.toString());
		}
		this.watchService = _watchService;
		if (this.watchService != null)
		{
			this.callbacks = new TreeMap<>();
			final Runnable watchServiceHandler = () ->
			{
				if (this.watchService != null)
				{
					WatchKey watchKey = null;
					boolean isActive = true;
					while (isActive)
					{
						try
						{
							if (watchKey == null)
							{
								watchKey = watchService.take();
							}
							for (var watchEvent : watchKey.pollEvents())
							{
								final var eventKind = watchEvent.kind();
								final boolean isCreate = ENTRY_CREATE.equals(eventKind);
								final boolean isDelete = ENTRY_DELETE.equals(eventKind);
								final boolean isOverflow = OVERFLOW.equals(eventKind);
								if (callbacks != null)
								{
									final var watchableConfig = callbacks.get(watchKey.watchable());
									if (watchableConfig != null)
									{
										if (isCreate || isDelete)
										{
											if (watchEvent.context() instanceof Path entry)
											{
												watchableConfig.callback().accept(eventKind, entry);
											}
										}
										else if (isOverflow)
										{
											watchableConfig.callback().accept(eventKind, null);
										}
									}
								}
							}
							if (!watchKey.reset())
							{
								watchKey = null;
							}
						}
						catch (ClosedWatchServiceException ex)
						{
							isActive = false;
							logger.log(TRACE, () -> "WatchService closed.");
						}
						catch (InterruptedException ex)
						{
							logger.log(TRACE, () -> "watchService.take()", ex);
						}
					}
				}
			};
			final var thread = Thread.ofVirtual().factory().newThread(watchServiceHandler);
			if (thread != null)
			{
				this.threadWatchService = thread;
				this.threadWatchService.setName("DirectoryWatchServiceThread");
				this.fileSystemWatched = fileSystem;
			}
			else
			{
				this.fileSystemWatched = null;
				this.threadWatchService = null;
			}
		}
		else
		{
			this.fileSystemWatched = null;
			this.callbacks = null;
			this.threadWatchService = null;
		}
	}

	/**
	 * Starts the WatchService thread, if a WatchService was requested.
	 */
	void startWatchServiceThread()
	{
		if (threadWatchService != null && !threadWatchService.isAlive())
		{
			logger.log(TRACE, "Starting directory WatchService Thread …");
			threadWatchService.start();
		}
	}

	/**
	 * Returns true, iff watch service is requested and available.
	 *
	 * @return true, iff watch service is requested and available
	 */
	boolean isInUse()
	{
		return watchService != null;
	}

	@EnsuresNonNullIf(expression =
	{
		"#1", "watchService", "callbacks", "fileSystemWatched"
	}, result = true)
	private boolean precondition(Path directory)
	{
		return directory != null && watchService != null && callbacks != null &&
			fileSystemWatched != null && fileSystemWatched.equals(directory.getFileSystem());
	}

	synchronized void registerPath(Path directory, BiConsumer<WatchEvent.Kind<?>, @Nullable Path> callback)
	{
		if (precondition(directory))
		{
			try
			{
				final var watchKey = directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
				callbacks.put(directory, new WatchableConfig(watchKey, callback));
				logger.log(TRACE, () -> "Start watching path »%s«".formatted(directory));
			}
			catch (AccessDeniedException ex)
			{
				logger.log(INFO, () -> "Access denied for path »" + ex.getFile() + "«");
			}
			catch (IOException ex)
			{
				logger.log(WARNING, ex.toString());
			}
		}
	}

	synchronized boolean isPathWatched(Path directory)
	{
		return precondition(directory) && callbacks.containsKey(directory);
	}

	synchronized void unregisterPath(Path directory)
	{
		if (precondition(directory))
		{
			final var watchableConfig = callbacks.remove(directory);
			if (watchableConfig != null)
			{
				logger.log(TRACE, () -> "Stop watching path »%s«".formatted(directory));
				watchableConfig.watchKey().cancel();
			}
		}
	}

	@Override
	public void close() throws IOException
	{
		if (watchService != null)
		{
			try (watchService)
			{
				logger.log(TRACE, "Closing directory WatchService …");
			}
			logger.log(TRACE, "Closing directory WatchService done.");
		}
	}
}
