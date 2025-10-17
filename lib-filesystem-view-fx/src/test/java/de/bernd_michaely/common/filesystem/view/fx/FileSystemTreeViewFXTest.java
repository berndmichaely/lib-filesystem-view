/* Created on 2023-03-27 */
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.PathView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test class for the FileSystemTreeViewFX.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class FileSystemTreeViewFXTest
{
	private boolean timeout;

	private void launchTestApplication(Runnable runnable)
	{
		final var thread = new Thread(runnable, "TestApplication Launcher");
		thread.setDaemon(true);
		thread.start();
		timeout = false;
		boolean finished = false;
		while (!finished)
		{
			try
			{
				timeout = !TestApplication.awaitCountDownLatchLaunch();
				finished = true;
			}
			catch (InterruptedException ex)
			{
				System.err.println(ex.toString());
			}
		}
	}

	@Test
	@EnabledIfSystemProperty(named = "_custom.test.mode", matches = "demo")
	public void runDemo() throws InterruptedException, IOException
	{
		System.out.println("Run FileSystemTreeViewFX Demo …");
		launchTestApplication(TestApplication::runDemo);
		try (final FileSystemTreeView fstv = TestApplication.getTestApplication().getFileSystemTreeView())
		{
			fstv.expandPath(Paths.get(System.getProperty("user.home")), true, true);
			TestApplication.awaitCountDownLatchShutdown();
		}
	}

	private static class PathListener implements ChangeListener<Path>
	{
		private final CountDownLatch countDownLatch = new CountDownLatch(1);
		private volatile Path path;
		private volatile int numCalls;

		@Override
		public void changed(ObservableValue<? extends Path> observable, Path oldValue, Path newValue)
		{
			this.path = newValue;
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
	public void testComponent() throws IOException
	{
		System.out.println("Testing FileSystemTreeViewFX …");
		launchTestApplication(TestApplication::runTest);
		try
		{
			assertFalse(timeout, "TestApplication launch TIMEOUT");
			final var application = TestApplication.getTestApplication();
			assertNotNull(application);
			try (final var fstv = application.getFileSystemTreeView())
			{
				assertNotNull(fstv);
				assertNotNull(fstv.getComponent());
				final TreeView<PathView> treeView = fstv.getTreeView();
				assertNotNull(treeView);
				final TreeItem<PathView> root = treeView.getTreeItem(0);
				assertNotNull(root);
				assertFalse(root.getValue().toString().isEmpty());
				assertFalse(root.isExpanded(), "root.isExpanded()");
				assertFalse(root.isLeaf(), "root.isLeaf()");
				assertFalse(fstv.isPathSelected());
				final Path tempDirectory = Files.createTempDirectory(
					"UnitTest_" + getClass().getSimpleName());
				try
				{
					// select:
					final var pathListenerSelect = new PathListener();
					fstv.selectedPathProperty().addListener(pathListenerSelect);
					fstv.expandPath(tempDirectory, false, true);
					assertTrue(pathListenerSelect.await(), "Timeout for Select reached");
					assertEquals(tempDirectory, pathListenerSelect.getPath());
					fstv.selectedPathProperty().removeListener(pathListenerSelect);
					assertEquals(0, pathListenerSelect.getCount());
					// unselect:
					final var pathListenerUnselect = new PathListener();
					fstv.selectedPathProperty().addListener(pathListenerUnselect);
					Files.delete(tempDirectory);
					assertTrue(pathListenerUnselect.await(), "Timeout for Unselect reached");
//					assertNull(pathListenerUnselect.getPath());
					fstv.selectedPathProperty().removeListener(pathListenerUnselect);
					assertEquals(0, pathListenerUnselect.getCount());
					// test removePathSelectionListener:
					assertEquals(1, pathListenerSelect.getNumCalls());
					assertEquals(1, pathListenerUnselect.getNumCalls());
				}
				catch (Throwable t)
				{
					t.printStackTrace();
				}
				finally
				{
					assertFalse(Files.deleteIfExists(tempDirectory));
				}
			}
		}
		finally
		{
			Platform.exit();
		}
	}
}
