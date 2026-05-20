#!/usr/bin/env groovy

@Grab(group='de.bernd-michaely', module='lib-filesystem-view-swing', version='1.0.0-rc.1')
@Grab(group='com.formdev', module='flatlaf', version='3.7.1')
import com.formdev.flatlaf.*
import de.bernd_michaely.common.filesystem.view.base.*
import de.bernd_michaely.common.filesystem.view.swing.*
//import groovy.transform.Field
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.border.*;
import static javax.swing.WindowConstants.*;

println 'Running Swing based filesystem view demo ...'

class DemoUserNodeConfiguration implements UserNodeConfiguration
{
	private BooleanSupplier showHiddenDirs;

	DemoUserNodeConfiguration(BooleanSupplier showHiddenDirs)
	{
		this.showHiddenDirs = showHiddenDirs;
	}

	@Override
	public boolean isCreatingNodeForFile(Path file)
	{
		return file.getFileName().toString().toLowerCase().endsWith(".zip");
	}

	@Override
	public boolean isCreatingNodeForDirectory(Path directory)
	{
		try
		{
			return showHiddenDirs.getAsBoolean() || !Files.isHidden(directory);
		}
		catch (IOException ex)
		{
			logger.log(WARNING, getClass().getName() + "::isCreatingNodeForDirectory", ex);
			return false;
		}
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
		return new DemoUserNodeConfiguration(showHiddenDirs);
	}

	@Override
	public boolean isLeafNode(Path fileName)
	{
		return fileName.toString().equalsIgnoreCase("DCIM");
	}
}

//FlatLightLaf.setup()
FlatDarkLaf.setup()

final var frame = new JFrame(JFileSystemTreeView.class.getSimpleName() + " – Simple Demo");
final var menuItemHiddenDirs = new JCheckBoxMenuItem();
final var fileSystemTreeView = JFileSystemTreeView.createInstance(
	Configuration.builder()
	.setUserNodeConfiguration(new DemoUserNodeConfiguration(menuItemHiddenDirs::isSelected))
	.setRequestingWatchService(true)
	.build());
final var labelSelectedPath = new JLabel();
final var listDirContent = new JList<Path>();
fileSystemTreeView.addPathSelectionListener(path ->
	{
		String name = "";
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
final JComponent component = fileSystemTreeView.getComponent();
component.setPreferredSize(new Dimension(300, 600));
final var scrollPaneList = new JScrollPane();
scrollPaneList.setPreferredSize(new Dimension(500, 600));
scrollPaneList.setViewportView(listDirContent);
final var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	true, component, scrollPaneList);
final var paneContent = new JPanel(new BorderLayout());
labelSelectedPath.setBorder(new EmptyBorder(0, 8, 8, 8));
splitPane.setBorder(new EmptyBorder(8, 8, 8, 8));
paneContent.add(splitPane, BorderLayout.CENTER);
paneContent.add(labelSelectedPath, BorderLayout.PAGE_END);
frame.getContentPane().add(paneContent);
final JMenu menuFile = new JMenu("File");
final JMenuBar menuBar = new JMenuBar();
menuBar.add(menuFile);
menuFile.add(new JMenuItem(new AbstractAction("Exit")
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				frame.dispose();
			}
		}));
final JMenu menuView = new JMenu("View");
menuItemHiddenDirs.setAction(new AbstractAction("Show hidden directories")
	{
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			fileSystemTreeView.updateTree();
		}
	});
menuView.add(menuItemHiddenDirs);
menuBar.add(menuView);
final JMenu menuHelp = new JMenu("Help");
menuHelp.add(new JMenuItem(new AbstractAction("Info about...")
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				JOptionPane.showMessageDialog(frame, "Demo application for lib-filesystem-view-swing");
			}
		}));
menuBar.add(menuHelp);
frame.setJMenuBar(menuBar);
frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
frame.pack();
frame.setVisible(true);
fileSystemTreeView.expandPath(Paths.get(System.getProperty("user.home")), true, true);
