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

import com.google.common.jimfs.Jimfs;
import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.NodeView;
import de.bernd_michaely.common.filesystem.view.base.UserNodeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

import static com.google.common.jimfs.Configuration.*;
import static com.google.common.jimfs.WatchServiceConfiguration.polling;
import static de.bernd_michaely.common.filesystem.view.base.ctrl.NodeCtrlTest.EntryType.*;
import static de.bernd_michaely.common.filesystem.view.base.ctrl.NodeCtrlTest.WatchAction.*;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests class for NodeCtrl.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class NodeCtrlTest
{
	private static final int TIMEOUT_SECONDS = 5;
	private static final int WAIT_TIME_MILLISECONDS = 100;

	/**
	 * Test file in resources.
	 */
	private static final String FILENAME_ZIP_1 = "test1.zip";
	/**
	 * Test file embedded @ {@link #FILENAME_ZIP_1}{@code /test1/d/e/f}.
	 */
	private static final String FILENAME_ZIP_2 = "test2.zip";
	/**
	 * Test file embedded @ {@link #FILENAME_ZIP_2}{@code /test2/g/h/i}.
	 */
	private static final String FILENAME_ZIP_3 = "test3.zip";
	/**
	 * Plain test file embedded @ {@link #FILENAME_ZIP_1}{@code /test1/d}.
	 */
	private static final String FILENAME_TXT_1 = "test1.txt";
	private static final String DIR0 = "unit_tests";
	private static final String DIR1 = "my_user";

	private static class UserNodeConfigurationHiddenDirs implements UserNodeConfiguration
	{
		private static UserNodeConfiguration instance = new UserNodeConfigurationHiddenDirs();

		/**
		 * Returns the singleton instance.
		 *
		 * @return the singleton instance
		 */
		public static UserNodeConfiguration getInstance()
		{
			return instance;
		}

		/**
		 * Returns the singleton instance.
		 *
		 * @param path unused (the configuration is shared for all paths)
		 * @return the singleton instance
		 */
		@Override
		public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
		{
			return getInstance();
		}

		/**
		 * Creates nodes for all directories including hidden directories. (This is
		 * needed in the unit tests, if the system dependant method to create
		 * temporary directories creates a hidden directory, e.g. on Windows under
		 * {@code \AppData}.)
		 */
		@Override
		public boolean isCreatingNodeForDirectory(Path directory)
		{
			return true;
		}
	}

	/**
	 * Creates a Windows type virtual filesystem and initializes it with a few
	 * entries for testing.
	 *
	 * @return the created filesystem
	 * @throws IOException
	 * @see PathFactory#createPaths()
	 */
	private static FileSystem createWindowsFileSystem() throws IOException
	{
		final FileSystem fileSystem = Jimfs.newFileSystem(windows().toBuilder()
			.setRoots("A:\\", "C:\\", "D:\\")
			.setWorkingDirectory("C:\\" + DIR0 + "\\" + DIR1)
			.setWatchServiceConfiguration(polling(WAIT_TIME_MILLISECONDS, MILLISECONDS))
			.build());
		final var pathFactory = new PathFactory(fileSystem, "D:\\");
		pathFactory.createPaths();
		pathFactory.mkdirPaths();
		return fileSystem;
	}

	/**
	 * Creates a Unix type virtual filesystem and initializes it with a few
	 * entries for testing.
	 *
	 * @return the created filesystem
	 * @throws IOException
	 * @see PathFactory#createPaths()
	 */
	private static FileSystem createUnixFileSystem() throws IOException
	{
		final FileSystem fileSystem = Jimfs.newFileSystem(unix().toBuilder()
			.setWorkingDirectory("/" + DIR0 + "/" + DIR1)
			.setWatchServiceConfiguration(polling(WAIT_TIME_MILLISECONDS, MILLISECONDS))
			.build());
		final var pathFactory = new PathFactory(fileSystem, "/");
		pathFactory.createPaths();
		pathFactory.mkdirPaths();
		return fileSystem;
	}

	/**
	 * Returns the filesystem where resources are to be found.
	 *
	 * @return the filesystem for resources
	 */
	private FileSystem getFileSystemResources()
	{
		return FileSystems.getDefault();
	}

	@Test
	public void testFileSystemRootsWindows() throws IOException
	{
		try (final FileSystem fs = Jimfs.newFileSystem(windows().toBuilder()
			.setRoots("C:\\", "D:\\", "U:\\", "V:\\", "W:\\", "A:\\", "R:\\")
			.setWorkingDirectory("C:\\" + DIR0 + "\\" + DIR1)
			.build()))
		{
			assertNotEquals(FileSystems.getDefault(), fs);
			assertTrue(fs.isOpen(), "filesystem is not open");
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(fs)
				.setRequestingWatchService(false)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				assertEquals(List.of("A:\\", "C:\\", "D:\\", "R:\\", "U:\\", "V:\\", "W:\\"),
					fstv.getEntryNames());
				assertFalse(fstv.isPathSelected());
				final Path selectedPath1 = fstv.expandPath(fs.getPath("C:\\"), false, true);
				assertTrue(fstv.isPathSelected());
				assertEquals(fs.getPath("C:\\"), selectedPath1);
				fstv.expandPath(null, false, true);
				assertFalse(fstv.isPathSelected());
				final Path selectedPath2 = fstv.expandPath(fs.getPath("C:\\" + DIR0 + "\\" + DIR1), false, true);
				assertTrue(fstv.isPathSelected());
				assertEquals(fs.getPath("C:\\", DIR0, DIR1), selectedPath2);
			}
		}
	}

	@Test
	public void testFileSystemRootsUnix() throws IOException
	{
		try (final FileSystem fs = Jimfs.newFileSystem(unix().toBuilder()
			.setWorkingDirectory("/" + DIR0 + "/" + DIR1)
			.build()))
		{
			assertNotEquals(FileSystems.getDefault(), fs);
			assertTrue(fs.isOpen(), "filesystem is not open");
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(fs)
				.setRequestingWatchService(false)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				assertEquals(List.of("/"), fstv.getEntryNames());
				assertFalse(fstv.isPathSelected());
				final Path selectedPath = fstv.expandPath(fs.getPath("/"), false, true);
				assertTrue(fstv.isPathSelected());
				assertEquals(fs.getPath("/"), selectedPath);
			}
		}
	}

	@Test
	public void testExpansionAndSelection() throws IOException
	{
		try (final FileSystem fs = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), fs);
			assertTrue(fs.isOpen(), "filesystem is not open");
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(fs)
				.setRequestingWatchService(false)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				// select
				final Path path1 = fs.getPath("/", DIR0, DIR1);
				assertTrue(path1.isAbsolute());
				fstv.expandPath(path1, false, true);
				assertTrue(fstv.isPathSelected());
				assertEquals(path1, fstv.getSelectedPath());
				// select
				final Path path2 = fs.getPath("/", "a", "b", "c");
				assertTrue(path2.isAbsolute());
				fstv.expandPath(path2, false, true);
				assertTrue(fstv.isPathSelected());
				assertEquals(fs.getPath("/a/b/c"), fstv.getSelectedPath());
				// clear selection
				fstv.clearSelection();
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				// expanded paths
				final Path path3 = fs.getPath("/", "c", "b", "a");
				fstv.expandPath(path3, false, false);
				assertTrue(path3.isAbsolute());
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				final List<Path> expectedExpandedPaths = List.of(
					fs.getPath("/a/a"),
					fs.getPath("/a/b/a"),
					fs.getPath("/a/b/b"),
					fs.getPath("/a/b/c"),
					fs.getPath("/a/c"),
					fs.getPath("/b"),
					fs.getPath("/c/a"),
					fs.getPath("/c/b/a"),
					fs.getPath("/c/b/b"),
					fs.getPath("/c/b/c"),
					fs.getPath("/c/c"),
					fs.getPath("/", DIR0, DIR1));
				assertIterableEquals(expectedExpandedPaths, fstv.getExpandedPaths());
				assertEquals(expectedExpandedPaths, List.copyOf(fstv.getExpandedPaths()));
				// get "/" node and collapse it
				final NodeViewImpl nodeView = fstv.getSubNodes().get(0);
				final DirectoryEntry pathView = nodeView.getPathView();
				final NodeCtrl nodeCtrl = pathView.getNodeCtrl();
				assertTrue(nodeCtrl.isExpanded());
				nodeCtrl.setExpanded(false);
				final SortedSet<Path> expandedPaths = fstv.getExpandedPaths();
				assertIterableEquals(List.of(fs.getPath("/")), expandedPaths);
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				// re-expand paths
				fstv.expandPath(path1, false, false);
				fstv.expandPath(path2, false, false);
				fstv.expandPath(path3, false, false);
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				fstv.expandPath(fs.getPath("/", "b"), false, true);
				assertTrue(fstv.isPathSelected());
				assertEquals(fs.getPath("/b"), fstv.getSelectedPath());
				assertIterableEquals(expectedExpandedPaths, fstv.getExpandedPaths());
				assertEquals(expectedExpandedPaths, List.copyOf(fstv.getExpandedPaths()));
			}
		}
	}

	@Test
	public void testCustomFileSystemInvalid() throws IOException
	{
		final var userNodeConfiguration = new UserNodeConfiguration()
		{
			@Override
			public boolean isCreatingNodeForFile(Path file)
			{
				return file.getFileName().toString().toLowerCase().endsWith(".zip");
			}

			@Override
			public FileSystem createFileSystemFor(Path file)
			{
				// assume FileSystem creation failes for whatever reason:
				return null;
			}

			@Override
			public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
			{
				return this; // this class is a singleton
			}
		};
		try (final FileSystem mainFileSystem = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), mainFileSystem);
			assertTrue(mainFileSystem.isOpen(), "filesystem is not open");
			final Path path = mainFileSystem.getPath("/", "a", "zero.zip");
			Files.createFile(path);
			Files.isRegularFile(path);
			assertEquals(0, Files.size(path));
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(mainFileSystem)
				.setRequestingWatchService(false)
				.setUserNodeConfiguration(userNodeConfiguration)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				assertEquals(path, fstv.expandPath(path, false, true));
				final NodeViewImpl nodeView = fstv._expandPath(path);
				assertEquals(0, nodeView.getSubNodes().size());
				assertEquals(path, fstv.getSelectedPath());
			}
		}
	}

	/**
	 * Copy {@link #FILENAME_ZIP_1} from resources to target path.
	 *
	 * @param targetDirectory the path to copy the resource file to
	 * @return a Path to the added file
	 */
	private Path _copyZipResourceFileTo(Path targetDirectory) throws IOException
	{
		final Path copyTarget = targetDirectory.resolve(FILENAME_ZIP_1);
		try (InputStream s = getClass().getResourceAsStream(FILENAME_ZIP_1))
		{
			assertNotNull(s);
			Files.copy(s, copyTarget);
		}
		assertTrue(Files.isRegularFile(copyTarget));
		return copyTarget;
	}

	@Test
	public void testCustomSingleSubRoot() throws IOException, URISyntaxException
	{
		class Counter
		{
			private int value;

			private int getValue()
			{
				return value;
			}

			private void increment()
			{
				value++;
			}
		}
		final var userNodeConfiguration = new UserNodeConfiguration()
		{
			// the boolean value stores the open status of the filesystem:
			private final IdentityHashMap<FileSystem, Counter> fileSystemsOnCloseCounters = new IdentityHashMap<>();

			@Override
			public boolean isCreatingNodeForFile(Path file)
			{
				return file.getFileName().toString().toLowerCase().endsWith(".zip");
			}

			@Override
			public FileSystem createFileSystemFor(Path file)
			{
				FileSystem zipFileSystem;
				try
				{
					zipFileSystem = FileSystems.newFileSystem(file);
				}
				catch (IOException ex)
				{
					System.err.println(ex);
					zipFileSystem = null;
				}
				fileSystemsOnCloseCounters.put(zipFileSystem, new Counter());
				return zipFileSystem;
			}

			@Override
			public void onClosingFileSystem(FileSystem fileSystem)
			{
				fileSystemsOnCloseCounters.get(fileSystem).increment();
			}

			@Override
			public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
			{
				return this; // this class is a singleton
			}
		};
		try (final FileSystem mainFileSystem = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), mainFileSystem);
			assertTrue(mainFileSystem.isOpen(), "filesystem is not open");
			// copy test1.zip from resources to JIMFS:
			_copyZipResourceFileTo(mainFileSystem.getPath("/", "a", "b", "c"));
			final Path fileTest2Zip = mainFileSystem
				.getPath("/", "a", "b", "c", FILENAME_ZIP_1, "test1", "d", "e", "f", FILENAME_ZIP_2);
			assertFalse(Files.isRegularFile(fileTest2Zip));
			// create TreeView on JIMFS:
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(mainFileSystem)
				.setRequestingWatchService(false)
				.setUserNodeConfiguration(userNodeConfiguration)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				// open 3 nested zip filesystems:
				FileSystem prevFileSystem = mainFileSystem;
				char c = 'a';
				Path path = mainFileSystem.getPath("/", "" + c++);
				for (int i = 1; i <= 3; i++)
				{
					final String name = "test" + i;
					path = mainFileSystem.getPath(path.toString(), "" + c++, "" + c++, name + ".zip", name, "" + c++);
					System.out.println("Checking path »" + path + "« … ");
					final Path selectedPath = fstv.expandPath(path, false, true);
					assertTrue(fstv.isPathSelected());
					assertEquals(selectedPath, fstv.getSelectedPath());
					System.out.println("→ selected Path = »" + selectedPath + "«");
					assertNotEquals(prevFileSystem, selectedPath.getFileSystem());
					assertNotEquals(FileSystems.getDefault(), selectedPath.getFileSystem());
					prevFileSystem = selectedPath.getFileSystem();
					// check access to test#.txt file content
					final String fileContent = Files.readString(selectedPath.resolve(name + ".txt")).trim();
					assertEquals("Hello from test" + i + ".txt.", fileContent);
				}
			}
			// closed FileSystemTreeView should leave all generated custom FileSystems closed:
			assertEquals(3, userNodeConfiguration.fileSystemsOnCloseCounters.size());
			// UserNodeConfiguration::onClosingFileSystem has been called once on all filesystems:
			userNodeConfiguration.fileSystemsOnCloseCounters.values().stream()
				.map(Counter::getValue)
				.forEach(counter -> assertEquals(1, counter));
			// all filesystems are actually closed:
			assertTrue(userNodeConfiguration.fileSystemsOnCloseCounters.keySet().stream().noneMatch(FileSystem::isOpen));
		}
	}

	@Test
	public void testCustomMultipleSubRoots() throws IOException
	{
		final var userNodeConfiguration = new UserNodeConfiguration()
		{
			FileSystem jimFileSystem;
			boolean fileSystemClosed;

			@Override
			public boolean isCreatingNodeForFile(Path file)
			{
				return file.getFileName().toString().toLowerCase().endsWith(".jimfs");
			}

			@Override
			public FileSystem createFileSystemFor(Path file)
			{
				try
				{
					jimFileSystem = createWindowsFileSystem();
				}
				catch (IOException ex)
				{
					System.err.println(ex);
					jimFileSystem = null;
				}
				return jimFileSystem;
			}

			@Override
			public void onClosingFileSystem(FileSystem fileSystem)
			{
				fileSystemClosed = true;
			}

			@Override
			public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
			{
				return this; // this class is a singleton
			}
		};
		try (final FileSystem mainFileSystem = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), mainFileSystem);
			assertTrue(mainFileSystem.isOpen(), "filesystem is not open");
			Files.createFile(mainFileSystem.getPath("/", "a", "b", "c", "test.jimfs"));
			final Path path2 = mainFileSystem
				.getPath("/", "a", "b", "c", "test.jimfs", "D:\\", "a", "b", "c");
			assertFalse(Files.isRegularFile(mainFileSystem.getPath(path2.toString(), "test.txt")));
			// create TreeView on JIMFS:
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(mainFileSystem)
				.setRequestingWatchService(false)
				.setUserNodeConfiguration(userNodeConfiguration)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				assertNotEquals(mainFileSystem, userNodeConfiguration.jimFileSystem);
				assertNotEquals(FileSystems.getDefault(), userNodeConfiguration.jimFileSystem);
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				fstv.expandPath(path2, false, true);
				assertTrue(fstv.isPathSelected());
				assertEquals(userNodeConfiguration.jimFileSystem.getPath("D:", "a", "b", "c"),
					fstv.getSelectedPath());
				assertEquals("D:\\a\\b\\c", fstv.getSelectedPath().toString());
				Files.writeString(userNodeConfiguration.jimFileSystem.getPath("D:", "a", "b", "c", "test.txt"),
					"Hello, world!", CREATE_NEW, WRITE);
				assertEquals("Hello, world!", Files.readString(fstv.getSelectedPath().resolve("test.txt")));
			}
			assertTrue(userNodeConfiguration.fileSystemClosed);
			assertFalse(userNodeConfiguration.jimFileSystem.isOpen());
		}
	}

	@Test
	public void testComparator() throws IOException
	{
		record TestConf(Comparator<String> comparator, List<String> expectedResult)
			{
		}
		final Comparator<String> comparator = String::compareTo;
		final Comparator<String> compReversed = comparator.reversed();
		final var testConfigurations = List.of(
			new TestConf(comparator, List.of("a", "b", "c", DIR0)),
			new TestConf(compReversed, List.of(DIR0, "c", "b", "a")));
		try (final FileSystem mainFileSystem = createUnixFileSystem())
		{
			for (TestConf testConfiguration : testConfigurations)
			{
				try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
					.setFileSystem(mainFileSystem)
					.setRequestingWatchService(false)
					.setFileNameComparator(testConfiguration.comparator())
					.build()))
				{
					assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
					fstv.expandPath(mainFileSystem.getPath("/", "a"), false, true);
					final List<String> expected = testConfiguration.expectedResult();
					final List<String> actual = fstv.getSubNodes().get(0).getSubNodes().stream()
						.map(NodeView::toString).toList();
					assertEquals(expected, actual);
					assertIterableEquals(expected, actual);
				}
			}
		}
	}

	enum WatchAction
	{
		ADDED, REMOVED
	}

	enum EntryType
	{
		DIRECTORY, FILE, ROOT
	}

	/**
	 * Tests, whether the watch service of a watched node detects an added or
	 * removed directory entry correctly. If successful, the method blocks until
	 * the expected event has been detected. If unsuccessful, the assertion fails
	 * by a timeout.
	 *
	 * @param nodeView    the node view of a watched tree node
	 * @param watchAction indicates whether subPath is to be added or removed
	 * @param subPath     a subPath to be added to or removed from the nodeView
	 * @param entryType   indicates whether subPath should be a file or directory
	 * @param index       the expected position of the added or removed subPath
	 * @throws IOException
	 */
	private void _watchWatchService(NodeViewImpl nodeView, WatchAction watchAction,
		Path subPath, EntryType entryType, int index)
		throws IOException
	{
		requireNonNull(watchAction, "WatchAction is null");
		requireNonNull(entryType, "EntryType is null");
		final var countDownLatch = new CountDownLatch(1);
		nodeView.setUnitTestCallback((boolean added, DirectoryEntry subDirectoryEntry, int actualIndex) ->
		{
			final String action = added ? "Added  " : "Removed";
			System.out.format("%s »%s« @ %d%n", action, subDirectoryEntry, actualIndex);
			assertEquals(watchAction.equals(ADDED), added);
			assertEquals(subPath, subDirectoryEntry.getPath());
			assertEquals(index, actualIndex);
			countDownLatch.countDown();
		});
		try
		{
			final BooleanSupplier checkEntryExists = () ->
			{
				boolean result = false;
				switch (entryType)
				{
					case DIRECTORY -> result = Files.isDirectory(subPath);
					case FILE -> result = Files.isRegularFile(subPath);
					case ROOT ->
					{
						for (Path rootDirectory : subPath.getFileSystem().getRootDirectories())
						{
							if (subPath.equals(rootDirectory))
							{
								result = true;
							}
						}
					}
					default -> throw new AssertionError("Invalid EntryType");
				}
				return result;
			};
			if (watchAction.equals(ADDED))
			{
				assertFalse(checkEntryExists);
				switch (entryType)
				{
					case DIRECTORY -> assertEquals(subPath, Files.createDirectory(subPath));
					case FILE -> assertEquals(subPath, Files.createFile(subPath));
					case ROOT -> assertTrue(((ModifiableRootsFileSystem) subPath.getFileSystem())
							.getModifiableRoots().add(subPath));
					default -> throw new AssertionError("Invalid EntryType");
				}
				assertTrue(checkEntryExists);
			}
			else
			{
				assertTrue(checkEntryExists);
				switch (entryType)
				{
					case DIRECTORY, FILE -> Files.delete(subPath);
					case ROOT -> assertTrue(((ModifiableRootsFileSystem) subPath.getFileSystem())
							.getModifiableRoots().remove(subPath));
					default -> throw new AssertionError("Invalid EntryType");
				}
				assertFalse(checkEntryExists);
			}
			// wait for subdirectory changes
			boolean finished = false;
			boolean finishedByTimeout = false;
			while (!finished)
			{
				try
				{
					finishedByTimeout = !countDownLatch.await(TIMEOUT_SECONDS, SECONDS);
					finished = true;
				}
				catch (InterruptedException ex)
				{
					System.err.println(ex.toString());
				}
			}
			if (finishedByTimeout)
			{
				System.err.println("Timeout in WatchService test !");
			}
			assertFalse(finishedByTimeout, "finished by timeout");
		}
		finally
		{
			nodeView.setUnitTestCallback(null);
		}
	}

	/**
	 * Waits a moment to avoid overflow events from watch service. It's against
	 * the »Fast Feedback« principle for unit tests, and it's not very reliable,
	 * but how else to do it?
	 */
	private void _wait_(long waitTime)
	{
		if (waitTime > 0)
		{
			System.out.format("(Waiting %d ms to avoid overflow events …)%n", waitTime);
			try
			{
				Thread.sleep(waitTime);
			}
			catch (InterruptedException ex)
			{
				System.err.println(ex.toString());
			}
		}
	}

	private void testWatchService(Path tempDirectoryBase, long waitTime,
		Boolean isWatchingFileSystemRoots) throws IOException
	{
		final Path subDir1 = tempDirectoryBase.resolve("subdir1");
		final Path subDir2 = tempDirectoryBase.resolve("subdir2");
		final Path subDir3 = tempDirectoryBase.resolve("subdir3");
		System.out.println("testWatchService() : tempDirectoryBase is »" + tempDirectoryBase + "«");
		try
		{
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(tempDirectoryBase.getFileSystem())
				.setRequestingWatchService(true)
				.setUserNodeConfiguration(UserNodeConfigurationHiddenDirs.getInstance())
				.build()))
			{
				final NodeViewImpl nodeView = fstv._expandPath(tempDirectoryBase);
				final var watchServiceCtrl = nodeView.getWatchServiceCtrl();
				assertTrue(watchServiceCtrl.isInUse());
				if (isWatchingFileSystemRoots != null)
				{
					assertEquals(isWatchingFileSystemRoots, fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				}
				assertFalse(watchServiceCtrl.isPathWatched(tempDirectoryBase));
				nodeView.setExpanded(true);
				assertTrue(watchServiceCtrl.isPathWatched(tempDirectoryBase));
				_watchWatchService(nodeView, ADDED, subDir2, DIRECTORY, 0);
				_wait_(waitTime);
				_watchWatchService(nodeView, ADDED, subDir1, DIRECTORY, 0);
				_wait_(waitTime);
				_watchWatchService(nodeView, REMOVED, subDir2, DIRECTORY, 1);
				_wait_(waitTime);
				_watchWatchService(nodeView, ADDED, subDir3, DIRECTORY, 1);
				_wait_(waitTime);
				_watchWatchService(nodeView, REMOVED, subDir1, DIRECTORY, 0);
				_wait_(waitTime);
				_watchWatchService(nodeView, REMOVED, subDir3, DIRECTORY, 0);
				nodeView.setExpanded(false);
				assertFalse(watchServiceCtrl.isPathWatched(tempDirectoryBase));
			}
		}
		finally
		{
			Files.deleteIfExists(subDir3);
			Files.deleteIfExists(subDir2);
			Files.deleteIfExists(subDir1);
			Files.deleteIfExists(tempDirectoryBase);
		}
	}

	@Test
	public void testWatchServiceDefaultFs() throws IOException
	{
		testWatchService(Files.createTempDirectory(System.getProperty("user.name") +
			"~" + getClass().getSimpleName() + "_"), WAIT_TIME_MILLISECONDS, null);
	}

	@Test
	public void testWatchServiceJimfsUnix() throws IOException
	{
		try (final var fileSystem = createUnixFileSystem())
		{
			testWatchService(fileSystem.getPath("/", "a", "b", "c"), 0, false);
		}
	}

	@Test
	public void testWatchServiceJimfsWindows() throws IOException
	{
		try (final var fileSystem = createWindowsFileSystem())
		{
			testWatchService(fileSystem.getPath("D:", "a", "b", "c"), 0, true);
		}
	}

	private static final class TestUserNodeConfiguration implements UserNodeConfiguration
	{
		private final Path path;
		private final Map<Path, TestUserNodeConfiguration> userNodeConfigurations;
		private Runnable callback;

		private TestUserNodeConfiguration()
		{
			this.path = null;
			userNodeConfigurations = new HashMap<>();
		}

		private TestUserNodeConfiguration(Path path, Map<Path, TestUserNodeConfiguration> userNodeConfigurations)
		{
			this.path = path;
			this.userNodeConfigurations = userNodeConfigurations;
			System.out.println("Creating UserNodeConfiguration for path »" + this.path + "«");
		}

		@Override
		public boolean isRequestingUpdateNotifier()
		{
			return true;
		}

		@Override
		public void setUpdateNotifier(Runnable callback)
		{
			this.callback = callback;
		}

		@Override
		public TestUserNodeConfiguration getUserNodeConfigurationFor(Path path)
		{
			if (userNodeConfigurations.containsKey(path))
			{
				return userNodeConfigurations.get(path);
			}
			else
			{
				final var configuration = new TestUserNodeConfiguration(path, userNodeConfigurations);
				userNodeConfigurations.put(path, configuration);
				return configuration;
			}
		}

		/**
		 * This method simulates an update request. This usually happens through a
		 * node context menu call.
		 */
		private void testUpdate()
		{
			System.out.println("Calling update for path »" + path + "«");
			if (callback != null)
			{
				callback.run();
			}
		}
	}

	private void _testUpdates(Consumer<FileSystemTreeViewImpl> updateAction,
		List<String> expectedExpandedPaths) throws IOException
	{
		final var userNodeConfiguration = new TestUserNodeConfiguration();
		try (final FileSystem fs = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), fs);
			assertTrue(fs.isOpen(), "filesystem is not open");
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(fs)
				.setRequestingWatchService(false)
				.setUserNodeConfiguration(userNodeConfiguration)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				// expand a path:
				fstv.expandPath(fs.getPath("/", "a", "b", "c"), false, false);
				final List<Path> expectedExpandedPathsBefore = List.of(
					fs.getPath("/a/a"),
					fs.getPath("/a/b/a"),
					fs.getPath("/a/b/b"),
					fs.getPath("/a/b/c"),
					fs.getPath("/a/c"),
					fs.getPath("/b"),
					fs.getPath("/c"),
					fs.getPath("/", DIR0));
				assertEquals(expectedExpandedPathsBefore, List.copyOf(fstv.getExpandedPaths()));
				assertIterableEquals(expectedExpandedPathsBefore, fstv.getExpandedPaths());
				// modify filesystem:
				Files.delete(fs.getPath("/a/b/b"));
				Files.createDirectories(fs.getPath("/a/b/e"));
				Files.createDirectories(fs.getPath("/a/b/c/d"));
				Files.createDirectories(fs.getPath("/d/h/i"));
				Files.createDirectories(fs.getPath("/e"));
				// check updates:
				final List<Path> expectedExpandedPathsAfter = expectedExpandedPaths.stream()
					.map(fs::getPath).toList();
				assertEquals(expectedExpandedPathsBefore, List.copyOf(fstv.getExpandedPaths()));
				// run the update action:
				updateAction.accept(fstv);
				assertNotEquals(expectedExpandedPathsBefore, List.copyOf(fstv.getExpandedPaths()));
				assertEquals(expectedExpandedPathsAfter, List.copyOf(fstv.getExpandedPaths()));
				assertIterableEquals(expectedExpandedPathsAfter, fstv.getExpandedPaths());
			}
		}
	}

	@Test
	public void testUpdateNotifier() throws IOException
	{
		_testUpdates(fstv ->
			((TestUserNodeConfiguration) fstv.getConfiguration().userNodeConfiguration())
				.getUserNodeConfigurationFor(fstv.getConfiguration().fileSystem().getPath("/", "a", "b"))
				.testUpdate(),
			List.of(
				"/a/a",
				"/a/b/a",
				"/a/b/c",
				"/a/b/e",
				"/a/c",
				"/b",
				"/c",
				"/" + DIR0));
	}

	@Test
	public void testUpdateTree() throws IOException
	{
		_testUpdates(FileSystemTreeViewImpl::updateTree,
			List.of(
				"/a/a",
				"/a/b/a",
				"/a/b/c",
				"/a/b/e",
				"/a/c",
				"/b",
				"/c",
				"/d",
				"/e",
				"/" + DIR0));
	}

	/**
	 * Tests, that the leaf status of a node can be changed at runtime.
	 *
	 * @throws IOException
	 */
	@Test
	public void testLeafChange() throws IOException
	{
		class LeafTestUserNodeConfiguration implements UserNodeConfiguration
		{
			private final Path path;
			private boolean leafNodeEnabled;
			private Runnable callbackUpdateNotifier;
			private final boolean isPrototype;

			private LeafTestUserNodeConfiguration(Path path)
			{
				this.path = path;
				this.isPrototype = path != null;
			}

			private boolean isLeafNodeEnabled()
			{
				return leafNodeEnabled;
			}

			private void setLeafNodeEnabled(boolean _leafNodeEnabled)
			{
				leafNodeEnabled = _leafNodeEnabled;
				if (callbackUpdateNotifier != null)
				{
					callbackUpdateNotifier.run();
				}
				else if (!isPrototype)
				{
					throw new IllegalStateException(getClass().getSimpleName() +
						".setLeafNodeEnabled(" + _leafNodeEnabled + ") : »callbackUpdateNotifier« is null");
				}
			}

			@Override
			public boolean isLeafNode(Path path)
			{
				return isLeafNodeEnabled() && path.getFileName() != null &&
					path.getFileName().toString().equals("a");
			}

			@Override
			public boolean isRequestingUpdateNotifier()
			{
				return true;
			}

			@Override
			public void setUpdateNotifier(Runnable callback)
			{
				this.callbackUpdateNotifier = requireNonNull(callback);
			}

			@Override
			public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
			{
				return new LeafTestUserNodeConfiguration(requireNonNull(path));
			}
		}
		try (final FileSystem fs = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), fs);
			assertTrue(fs.isOpen(), "filesystem is not open");
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(fs)
				.setRequestingWatchService(false)
				.setUserNodeConfiguration(new LeafTestUserNodeConfiguration(null))
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				final var path = fs.getPath("/", "a");
				final var nodeView = fstv._expandPath(path);
				final var unc = (LeafTestUserNodeConfiguration) nodeView.getUserNodeConfiguration();
				assertEquals(path, unc.path);
				assertFalse(unc.isLeafNodeEnabled());
				nodeView.setExpanded(true);
				assertEquals(3, nodeView.getSubNodes().size());
				unc.setLeafNodeEnabled(true);
				assertEquals(0, nodeView.getSubNodes().size());
				unc.setLeafNodeEnabled(false);
				assertEquals(3, nodeView.getSubNodes().size());
			}
		}
	}

	private void _testSingleSubRoot(FileSystem mainFileSystem, FileSystemTreeViewImpl fstv)
	{
		final var nodeView = fstv._expandPath(mainFileSystem.getPath("/", "a", "b", "c", FILENAME_ZIP_1));
		nodeView.setExpanded(true);
		final NodeViewImpl subNodeView = (NodeViewImpl) nodeView.getSubNodes().get(0);
		final String subNodePathName = subNodeView.getPathView().getPath().toString();
		System.out.println("subNodePathName = »" + subNodePathName + "«");
		assertNotEquals("/", subNodePathName);
	}

	/**
	 * Test the combination of an enabled watch service for the main filesystem
	 * and a custom sub root. Currently, watch services are not supported for
	 * custom sub roots. The test expands and collapses a sub node of a
	 * {@code *.zip} file, which should not throw any exceptions, ignoring an
	 * enabled global watch service.
	 *
	 * @throws IOException
	 */
	@Test
	public void testCustomSubRootAndWatchService() throws IOException
	{
		final var userNodeConfiguration = new UserNodeConfiguration()
		{
			@Override
			public boolean isCreatingNodeForFile(Path file)
			{
				return file.getFileName().toString().toLowerCase().endsWith(".zip");
			}

			@Override
			public FileSystem createFileSystemFor(Path file)
			{
				try
				{
					return FileSystems.newFileSystem(file);
				}
				catch (IOException ex)
				{
					throw new IllegalStateException(ex);
				}
			}

			@Override
			public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
			{
				return this; // this class is a singleton
			}
		};
		try (final FileSystem mainFileSystem = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), mainFileSystem);
			assertTrue(mainFileSystem.isOpen(), "filesystem is not open");
			_copyZipResourceFileTo(mainFileSystem.getPath("/", "a", "b", "c"));
			// create TreeView on JIMFS:
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(mainFileSystem)
				.setRequestingWatchService(true)
				.setUserNodeConfiguration(userNodeConfiguration)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				assertDoesNotThrow(() ->
					fstv._expandPath(mainFileSystem.getPath("/", "a", "b", "c", FILENAME_ZIP_1, "test1")));
				assertDoesNotThrow(() ->
					fstv._expandPath(mainFileSystem.getPath("/", "a", "b", "c", FILENAME_ZIP_1)).setExpanded(false));
				_testSingleSubRoot(mainFileSystem, fstv);
			}
		}
	}

	@Test
	public void testSingleSubRootSkipping() throws IOException
	{
		final var userNodeConfiguration = new UserNodeConfiguration()
		{
			@Override
			public boolean isCreatingNodeForFile(Path file)
			{
				return file.getFileName().toString().toLowerCase().endsWith(".zip");
			}

			@Override
			public FileSystem createFileSystemFor(Path file)
			{
				try
				{
					return FileSystems.newFileSystem(file);
				}
				catch (IOException ex)
				{
					throw new IllegalStateException(ex);
				}
			}

			@Override
			public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
			{
				return this; // this class is a singleton
			}
		};
		try (final FileSystem mainFileSystem = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), mainFileSystem);
			assertTrue(mainFileSystem.isOpen(), "filesystem is not open");
			_copyZipResourceFileTo(mainFileSystem.getPath("/", "a", "b", "c"));
			// create TreeView on JIMFS:
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(mainFileSystem)
				.setRequestingWatchService(true)
				.setUserNodeConfiguration(userNodeConfiguration)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				assertFalse(fstv.isPathSelected());
				assertNull(fstv.getSelectedPath());
				_testSingleSubRoot(mainFileSystem, fstv);
			}
		}
	}

	/**
	 * Tests, that watch services use the
	 * {@link UserNodeConfiguration#isCreatingNodeForDirectory(Path)} and
	 * {@link UserNodeConfiguration#isCreatingNodeForFile(Path)} path filters as
	 * the node expansion. Note, that only positive tests can be checked – paths
	 * filtered out will naturally never be reported by the watch service, which
	 * turns this into a semi-decidable problem. So the implementation code must
	 * take care to share the same logic.
	 *
	 * @see NodeCtrlDirectory#pathToDirectoryEntry(Path)
	 *
	 * @throws IOException
	 */
	@Test
	public void testWatchServiceAndPathFilter() throws IOException
	{
		final var userNodeConfiguration = new UserNodeConfiguration()
		{
			@Override
			public boolean isCreatingNodeForFile(Path file)
			{
				return file.getFileName().toString().toLowerCase().endsWith(".zip");
			}

			@Override
			public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
			{
				return this; // this class is a singleton
			}
		};
		try (final FileSystem mainFileSystem = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), mainFileSystem);
			assertTrue(mainFileSystem.isOpen(), "filesystem is not open");
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(mainFileSystem)
				.setRequestingWatchService(true)
				.setUserNodeConfiguration(userNodeConfiguration)
				.build()))
			{
				assertFalse(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				final Path targetDirectory = mainFileSystem.getPath("/", "a", "b");
				final NodeViewImpl nodeView = fstv._expandPath(targetDirectory);
				final var watchServiceCtrl = nodeView.getWatchServiceCtrl();
				assertFalse(watchServiceCtrl.isPathWatched(targetDirectory));
				nodeView.setExpanded(true);
				assertTrue(watchServiceCtrl.isPathWatched(targetDirectory));
				final Path targetFile = targetDirectory.resolve("test.zip");
				_watchWatchService(nodeView, ADDED, targetFile, FILE, 3);
				nodeView.setExpanded(false);
				assertFalse(watchServiceCtrl.isPathWatched(targetDirectory));
			}
		}
	}

	private void _testWatchServiceRemoveExpandedNode(Path path, Path subPath, int numSubNodes, int index)
		throws IOException
	{
		final var fileSystem = path.getFileSystem();
		assertEquals(fileSystem, subPath.getFileSystem());
		assertTrue(fileSystem.isOpen(), "filesystem is not open");
		try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
			.setFileSystem(fileSystem)
			.setRequestingWatchService(true)
			.setUserNodeConfiguration(UserNodeConfigurationHiddenDirs.getInstance())
			.build()))
		{
			final NodeViewImpl nodeView = fstv._expandPath(path);
			final var watchServiceCtrl = nodeView.getWatchServiceCtrl();
			assertTrue(watchServiceCtrl.isInUse());
			assertFalse(watchServiceCtrl.isPathWatched(subPath));
			assertFalse(fstv.isPathSelected());
			fstv.expandPath(subPath, true, true);
			assertTrue(fstv.isPathSelected());
			assertEquals(subPath, fstv.getSelectedPath());
			assertTrue(watchServiceCtrl.isPathWatched(subPath));
			assertEquals(numSubNodes, nodeView.getSubNodes().size());
			_watchWatchService(nodeView, REMOVED, subPath, DIRECTORY, index);
			assertEquals(numSubNodes - 1, nodeView.getSubNodes().size());
			assertFalse(watchServiceCtrl.isPathWatched(subPath));
			// automatic unselection of removed entries is not implemented in the
			// FileSystemTreeViewImpl class, so the following test would fail…
			// assertFalse(fstv.isPathSelected());
			// … but this should be tested in each actual UI implementation
			// ––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
			// re-create and re-delete the same subPath:
			fstv.clearSelection();
			assertFalse(fstv.isPathSelected());
			assertTrue(Files.notExists(subPath));
			_watchWatchService(nodeView, ADDED, subPath, DIRECTORY, index);
			assertFalse(watchServiceCtrl.isPathWatched(subPath));
			assertNotEquals(subPath, fstv.getSelectedPath());
			fstv.expandPath(subPath, true, true);
			assertTrue(fstv.isPathSelected());
			assertEquals(subPath, fstv.getSelectedPath());
			assertTrue(watchServiceCtrl.isPathWatched(subPath));
			assertEquals(numSubNodes, nodeView.getSubNodes().size());
			_watchWatchService(nodeView, REMOVED, subPath, DIRECTORY, index);
			assertEquals(numSubNodes - 1, nodeView.getSubNodes().size());
			assertFalse(watchServiceCtrl.isPathWatched(subPath));
		}
	}

	@Test
	public void testWatchServiceRemoveExpandedNode_CustomFileSystem() throws IOException
	{
		try (final var fileSystem = createUnixFileSystem())
		{
			assertNotEquals(FileSystems.getDefault(), fileSystem);
			final Path path = fileSystem.getPath("/", "a", "b");
			final Path subPath = path.resolve("c");
			_testWatchServiceRemoveExpandedNode(path, subPath, 3, 2);
		}
	}

	@Test
	public void testWatchServiceRemoveExpandedNode_DefaultFileSystem() throws IOException
	{
		final Path path = Files.createTempDirectory("UnitTest_" +
			getClass().getSimpleName() + "__testWatchServiceRemoveExpandedNode");
		try
		{
			final Path subPath = path.resolve("sub_path");
			Files.createDirectory(subPath);
			try
			{
				assertTrue(Files.isDirectory(subPath));
				assertEquals(FileSystems.getDefault(), path.getFileSystem());
				_testWatchServiceRemoveExpandedNode(path, subPath, 1, 0);
			}
			finally
			{
				Files.deleteIfExists(subPath);
			}
		}
		finally
		{
			Files.deleteIfExists(path);
		}
	}

	/**
	 * Compares the given paths for equality.
	 * <em>Implementation note:</em>
	 * The comparison intentionally avoids using {@code assertIterableEquals},
	 * since it iterates deeply and the {@code Iterable} {@code Path} does not
	 * take root elements into account. This might lead to false positive tests.
	 */
	private void _testRootDirectories(List<String> expected, ModifiableRootsFileSystem fs)
	{
		final List<Path> listExpected = new ArrayList<>();
		expected.forEach(s -> listExpected.add(fs.getPath(s)));
		final List<Path> listActual = new ArrayList<>();
		fs.getRootDirectories().forEach(listActual::add);
		assertEquals(listExpected, listActual);
	}

	@Test
	public void testFileSystemDummy() throws IOException
	{
		final var fs = new ModifiableRootsFileSystem();
		try (fs)
		{
			assertTrue(fs.isOpen(), "filesystem is not open");
			assertNotEquals(FileSystems.getDefault(), fs);
			_testRootDirectories(List.of(), fs);
			fs.getModifiableRoots().add(fs.getPath("A"));
			_testRootDirectories(List.of("A"), fs);
			fs.getModifiableRoots().add(fs.getPath("A"));
			_testRootDirectories(List.of("A"), fs);
			fs.getModifiableRoots().add(fs.getPath("C"));
			_testRootDirectories(List.of("A", "C"), fs);
			fs.getModifiableRoots().add(fs.getPath("B"));
			_testRootDirectories(List.of("A", "B", "C"), fs);
			fs.getModifiableRoots().remove(fs.getPath("B"));
			_testRootDirectories(List.of("A", "C"), fs);
		}
		assertFalse(fs.isOpen(), "filesystem is not closed");
	}

	@Test
	public void testPathDummy() throws IOException
	{
		final var fs = new ModifiableRootsFileSystem();
		try (fs)
		{
			assertTrue(fs.isOpen(), "filesystem is not open");
			assertNotEquals(FileSystems.getDefault(), fs);
			assertThrows(ClassCastException.class, () -> Path.of("test").compareTo(fs.getPath("test")));
			assertEquals(fs.getPath("A"), fs.getPath("A"));
			assertNotEquals(fs.getPath("A"), fs.getPath("B"));
		}
		assertFalse(fs.isOpen(), "filesystem is not closed");
	}

	/**
	 * Test WatchService for changing FileSystem roots.
	 *
	 * @throws java.io.IOException
	 */
	@Test
	public void testWatchService_FileSystemRoots() throws IOException
	{
		final var fs = new ModifiableRootsFileSystem();
		try (fs)
		{
			assertTrue(fs.isOpen(), "filesystem is not open");
			assertNotEquals(FileSystems.getDefault(), fs);
			fs.getModifiableRoots().addAll(List.of(fs.getPath("A"), fs.getPath("C")));
			// test watching of FileSystems roots:
			try (final var fstv = new FileSystemTreeViewImpl(Configuration.builder()
				.setFileSystem(fs)
				.setRequestingWatchService(true)
				.build()))
			{
				assertEquals(List.of("A:", "C:"), fstv.getEntryNames());
				assertTrue(fstv.getRootNodeCtrl().isWatchingFileSystemRoots());
				final NodeViewImpl rootNodeView = fstv.getRootNodeView();
				assertTrue(rootNodeView.getWatchServiceCtrl().isInUse());
				_watchWatchService(rootNodeView, ADDED, fs.getPath("W"), ROOT, 2);
				_watchWatchService(rootNodeView, REMOVED, fs.getPath("A"), ROOT, 0);
				_watchWatchService(rootNodeView, ADDED, fs.getPath("E"), ROOT, 1);
				_watchWatchService(rootNodeView, REMOVED, fs.getPath("W"), ROOT, 2);
			}
		}
		assertFalse(fs.isOpen(), "filesystem is not closed");
	}
}
