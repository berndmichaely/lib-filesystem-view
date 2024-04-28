/* Created on Aug 11, 2023 */
package de.bernd_michaely.common.filesystem.view.swing;

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.UserNodeConfiguration;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import static java.lang.System.Logger.Level.*;
import static javax.swing.WindowConstants.*;

/**
 * Test Application class for JFileSystemTreeView Unit test.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class JTestApplication
{
	private static final Logger logger = System.getLogger(JTestApplication.class.getName());
	private static JTestApplication testApplication;
	private static CountDownLatch countDownLatchShutdown;
	private final JFrame frame;
	private final JFileSystemTreeView fileSystemTreeView;

	private static class TestUserNodeConfiguration implements UserNodeConfiguration
	{
		private TestUserNodeConfiguration()
		{
		}

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
				logger.log(WARNING, ex.toString());
				return null;
			}
		}

		@Override
		public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
		{
			return new TestUserNodeConfiguration();
		}

		@Override
		public boolean isLeafNode(Path fileName)
		{
			return fileName.toString().equalsIgnoreCase("DCIM");
		}
	}

	JTestApplication(boolean demoMode)
	{
		this.frame = new JFrame(JFileSystemTreeView.class.getSimpleName() + " – Simple Demo");
		this.fileSystemTreeView = JFileSystemTreeView.createInstance(
			Configuration.builder()
				.setUserNodeConfiguration(new TestUserNodeConfiguration())
				.setRequestingWatchService(true)
				.build());
		final JLabel labelSelectedPath = new JLabel();
		final var listDirContent = new JList<Path>();
		this.fileSystemTreeView.addPathSelectionListener(path ->
		{
			final String name;
			if (path != null)
			{
				try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path))
				{
					final var list = new ArrayList<Path>();
					directoryStream.forEach(p -> list.add(p.getFileName()));
					list.sort(null);
					listDirContent.setListData(list.toArray(Path[]::new));
				}
				catch (IOException ex)
				{
					listDirContent.setListData(new Path[0]);
				}
				name = path.toString();
			}
			else
			{
				listDirContent.setListData(new Path[0]);
				name = "";
			}
			labelSelectedPath.setText(String.format("Selected path : »%s«", name));
		});
		final JComponent component = this.fileSystemTreeView.getComponent();
		component.setPreferredSize(new Dimension(300, 600));
		final JScrollPane scrollPaneList = new JScrollPane();
		scrollPaneList.setPreferredSize(new Dimension(500, 600));
		scrollPaneList.setViewportView(listDirContent);
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			true, component, scrollPaneList);
		final JPanel paneContent = new JPanel(new BorderLayout());
		labelSelectedPath.setBorder(new EmptyBorder(8, 8, 0, 8));
		splitPane.setBorder(new EmptyBorder(8, 8, 8, 8));
		paneContent.add(splitPane, BorderLayout.CENTER);
		paneContent.add(labelSelectedPath, BorderLayout.PAGE_START);
		frame.getContentPane().add(paneContent);
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.pack();
		if (demoMode)
		{
			countDownLatchShutdown = new CountDownLatch(1);
			frame.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosed(WindowEvent e)
				{
					countDownLatchShutdown.countDown();
				}
			});
			frame.setVisible(true);
		}
	}

	static void awaitCountDownLatchShutdown() throws InterruptedException
	{
		countDownLatchShutdown.await();
	}

	JFileSystemTreeView getFileSystemTreeView()
	{
		return fileSystemTreeView;
	}

	static JTestApplication runTest() throws InterruptedException, InvocationTargetException
	{
		SwingUtilities.invokeAndWait(() -> testApplication = new JTestApplication(false));
		return testApplication;
	}

	void shutDown()
	{
		testApplication.frame.dispose();
	}

	static JTestApplication runDemo() throws InterruptedException, InvocationTargetException
	{
		// set look and feel (optional dependency)
		final String classNameFlatLaf = "com.formdev.flatlaf.FlatDarkLaf";
		try
		{
			UIManager.setLookAndFeel((LookAndFeel) Class.forName(classNameFlatLaf)
				.getDeclaredConstructor().newInstance());
		}
		catch (ReflectiveOperationException | UnsupportedLookAndFeelException exFlatLaf)
		{
			logger.log(INFO, "»FlatLaf« not available => falling back to »NimbusLookAndFeel«");
			try
			{
				UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			}
			catch (ReflectiveOperationException | UnsupportedLookAndFeelException exNimbus)
			{
				logger.log(WARNING, exNimbus);
			}
		}
		SwingUtilities.invokeAndWait(() -> testApplication = new JTestApplication(true));
		return testApplication;
	}
}
