/*
 * Copyright 2025 Bernd Michaely (info@bernd-michaely.de).
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

import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.Collections.unmodifiableSortedSet;

/**
 * FileSystem stub implementation with modifiable roots for unit tests.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class ModifiableRootsFileSystem extends FileSystem
{
	private boolean open;
	private final SortedSet<Path> fileSystemRoots = new TreeSet<>();
	private final SortedSet<Path> unmodifiableFileSystemRoots = unmodifiableSortedSet(fileSystemRoots);
	private WatchService watchService;

	private class ModifiableRootsFileSystemPath implements Path
	{
		private final FileSystem fileSystem;
		private final String root;
		private Path rootPath;

		private ModifiableRootsFileSystemPath(FileSystem fileSystem, String root)
		{
			this.fileSystem = fileSystem;
			this.root = root;
		}

		@Override
		public int compareTo(Path path)
		{
			if (path instanceof ModifiableRootsFileSystemPath other)
			{
				return this.root.compareTo(other.root);
			}
			else
			{
				throw new ClassCastException(
					"Comparing paths associated with different providers: »%s« and »%s«".formatted(this, path));
			}
		}

		@Override
		public boolean equals(Object object)
		{
			try
			{
				return object instanceof Path other ? this.compareTo(other) == 0 : false;
			}
			catch (ClassCastException ex)
			{
				return false;
			}
		}

		@Override
		public int hashCode()
		{
			return Objects.hashCode(this.root);
		}

		@Override
		public boolean endsWith(Path other)
		{
			if (other != null && this.getFileSystem().provider() == other.getFileSystem().provider())
			{
				return this.getRoot().equals(other.getRoot()) && other.getNameCount() == 0;
			}
			else
			{
				return false;
			}
		}

		@Override
		public Path getFileName()
		{
			return null;
		}

		@Override
		public FileSystem getFileSystem()
		{
			return fileSystem;
		}

		@Override
		public Path getName(int index)
		{
			throw new IllegalArgumentException();
		}

		@Override
		public int getNameCount()
		{
			return 0;
		}

		@Override
		public Path getParent()
		{
			return null;
		}

		@Override
		public Path getRoot()
		{
			if (rootPath == null)
			{
				rootPath = new ModifiableRootsFileSystemPath(getFileSystem(), root);
			}
			return rootPath;
		}

		@Override
		public boolean isAbsolute()
		{
			return true;
		}

		@Override
		public Path normalize()
		{
			return getRoot();
		}

		@Override
		public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
			throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Path relativize(Path other)
		{
			throw new IllegalArgumentException();
		}

		@Override
		public Path resolve(Path other)
		{
			return other.isAbsolute() ? other :
				getPath(root, IntStream.range(0, other.getNameCount())
					.mapToObj(other::getName)
					.map(Path::toString)
					.toArray(String[]::new));
		}

		@Override
		public boolean startsWith(Path other)
		{
			return this.equals(other);
		}

		@Override
		public Path subpath(int beginIndex, int endIndex)
		{
			throw new IllegalArgumentException();
		}

		@Override
		public Path toAbsolutePath()
		{
			return getRoot();
		}

		@Override
		public Path toRealPath(LinkOption... options) throws IOException
		{
			return getRoot();
		}

		@Override
		public URI toUri()
		{
			return URI.create(root);
		}

		@Override
		public String toString()
		{
			return "%s:".formatted(root);
		}
	}

	private static class DummyWatchService implements WatchService
	{
		private volatile boolean open;
		private volatile Thread thread;

		private DummyWatchService(boolean open)
		{
			this.open = open;
		}

		@Override
		public void close()
		{
			if (open)
			{
				open = false;
				if (thread != null)
				{
					thread.interrupt();
					thread = null;
				}
			}
		}

		@Override
		public WatchKey poll()
		{
			if (open)
			{
				return null;
			}
			else
			{
				throw new ClosedWatchServiceException();
			}
		}

		@Override
		public WatchKey poll(long l, TimeUnit tu) throws InterruptedException
		{
			if (open)
			{
				return null;
			}
			else
			{
				throw new ClosedWatchServiceException();
			}
		}

		@Override
		public WatchKey take() throws InterruptedException
		{
			synchronized (this)
			{
				if (open)
				{
					thread = Thread.currentThread();
					while (open)
					{
						try
						{
							wait(); // just waiting infinitely here
						}
						catch (InterruptedException ex)
						{
							if (!open)
							{
								throw ex;
							}
						}
					}
					return null;
				}
				else
				{
					throw new ClosedWatchServiceException();
				}
			}
		}
	}

	ModifiableRootsFileSystem()
	{
		this.open = true;
	}

	SortedSet<Path> getModifiableRoots()
	{
		return fileSystemRoots;
	}

	@Override
	public void close() throws IOException
	{
		open = false;
		if (watchService != null)
		{
			try
			{
				watchService.close();
			}
			finally
			{
				watchService = null;
			}
		}
	}

	@Override
	public Iterable<FileStore> getFileStores()
	{
		if (open)
		{
			return () -> Collections.emptyIterator();
		}
		else
		{
			throw new ClosedFileSystemException();
		}
	}

	@Override
	public Path getPath(String first, String... more)
	{
		return new ModifiableRootsFileSystemPath(this, first);
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Path> getRootDirectories()
	{
		if (open)
		{
			return unmodifiableFileSystemRoots;
		}
		else
		{
			throw new ClosedFileSystemException();
		}
	}

	@Override
	public String getSeparator()
	{
		return "/";
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isOpen()
	{
		return open;
	}

	@Override
	public boolean isReadOnly()
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return a singleton dummy instance
	 * @throws IOException
	 */
	@Override
	public WatchService newWatchService() throws IOException
	{
		if (watchService == null)
		{
			watchService = new DummyWatchService(isOpen());
		}
		return watchService;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return null (as used in ModifiableRootsFileSystemPath::compareTo)
	 * @see ModifiableRootsFileSystemPath#compareTo(Path)
	 */
	@Override
	public FileSystemProvider provider()
	{
		return null;
	}

	@Override
	public Set<String> supportedFileAttributeViews()
	{
		throw new UnsupportedOperationException();
	}
}
