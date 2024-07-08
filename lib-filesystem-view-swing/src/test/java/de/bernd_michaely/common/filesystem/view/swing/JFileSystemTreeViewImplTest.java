/* Created on 2023-03-30 */
package de.bernd_michaely.common.filesystem.view.swing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test class for JFileSystemTreeViewImpl.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class JFileSystemTreeViewImplTest
{
	@Test
	@EnabledIfSystemProperty(named = "_custom.test.mode", matches = "demo")
	public void runDemo() throws InterruptedException, InvocationTargetException, IOException
	{
		System.out.println("Run JFileSystemTreeView Demo …");
		try (final JFileSystemTreeView fstv = JTestApplication.runDemo().getFileSystemTreeView())
		{
			fstv.expandPath(Paths.get(System.getProperty("user.home")), true, true);
			JTestApplication.awaitCountDownLatchShutdown();
		}
	}

	private static class PathConsumer implements Consumer<Path>
	{
		private final CountDownLatch countDownLatch = new CountDownLatch(1);
		private volatile Path path;
		private volatile int numCalls;

		@Override
		public void accept(Path path)
		{
			this.path = path;
			++numCalls;
			countDownLatch.countDown();
		}

		private Path getPath()
		{
			return path;
		}

		private int getNumCalls()
		{
			return numCalls;
		}

		private long getCount()
		{
			return countDownLatch.getCount();
		}

		private boolean await()
		{
			for (;;)
			{
				try
				{
					return countDownLatch.await(5, SECONDS);
				}
				catch (InterruptedException ex)
				{
					System.out.println(ex.toString());
				}
			}
		}
	}

	@Test
	@EnabledIfSystemProperty(named = "_custom.test.mode", matches = "test")
	public void testComponent() throws InterruptedException, InvocationTargetException, IOException
	{
		System.out.println("Testing JFileSystemTreeView …");
		final var application = JTestApplication.runTest();
		assertNotNull(application);
		try
		{
			try (final JFileSystemTreeView fstv = application.getFileSystemTreeView())
			{
				assertNotNull(fstv);
				assertNotNull(fstv.getComponent());
				assertNotNull(fstv.getTree());
				assertFalse(fstv.isPathSelected());
				final Path tempDirectory = Files.createTempDirectory(
					"UnitTest_" + getClass().getSimpleName());
				try
				{
					// select:
					final var pathConsumerSelect = new PathConsumer();
					fstv.addPathSelectionListener(pathConsumerSelect);
					fstv.expandPath(tempDirectory, false, true);
					assertTrue(pathConsumerSelect.await(), "Timeout for Select reached");
					assertEquals(tempDirectory, pathConsumerSelect.getPath());
					fstv.removePathSelectionListener(pathConsumerSelect);
					assertEquals(0, pathConsumerSelect.getCount());
					// unselect:
					final var pathConsumerUnselect = new PathConsumer();
					fstv.addPathSelectionListener(pathConsumerUnselect);
					Files.delete(tempDirectory);
					assertTrue(pathConsumerUnselect.await(), "Timeout for Unselect reached");
					assertNull(pathConsumerUnselect.getPath());
					fstv.removePathSelectionListener(pathConsumerUnselect);
					assertEquals(0, pathConsumerUnselect.getCount());
					// test removePathSelectionListener:
					assertEquals(1, pathConsumerSelect.getNumCalls());
					assertEquals(1, pathConsumerUnselect.getNumCalls());
				}
				finally
				{
					assertFalse(Files.deleteIfExists(tempDirectory));
				}
			}
		}
		finally
		{
			application.shutDown();
		}
	}
}
