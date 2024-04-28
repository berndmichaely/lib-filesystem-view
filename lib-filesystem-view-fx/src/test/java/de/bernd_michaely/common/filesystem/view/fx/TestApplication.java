/*
 * Created on Aug 5, 2023
 */
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.UserNodeConfiguration;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import static java.util.concurrent.TimeUnit.*;

/**
 * Test Application class for FileSystemTreeView Unit test.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class TestApplication extends Application
{
	private static final String PARAMETER_NAME_SHOW_WINDOW = "demo-mode";
	private static final CountDownLatch countDownLatchLaunch = new CountDownLatch(1);
	private static CountDownLatch countDownLatchShutdown;
	private static TestApplication testApplication;
	private FileSystemTreeView fileSystemTreeView;

	private boolean isDemoMode()
	{
		return Boolean.parseBoolean(getParameters().getNamed().get(PARAMETER_NAME_SHOW_WINDOW));
	}

	private static class TestUserNodeConfiguration implements UserNodeConfiguration
	{
		private TestUserNodeConfiguration()
		{
		}

		@Override
		public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
		{
			return new TestUserNodeConfiguration();
		}

		@Override
		public boolean isLeafNode(Path fileName)
		{
			return fileName != null ? fileName.toString().equalsIgnoreCase("DCIM") : false;
		}
	}

	@Override
	public void start(Stage stage) throws Exception
	{
		testApplication = this;
		this.fileSystemTreeView = FileSystemTreeView.createInstance(Configuration.builder()
			.setUserNodeConfiguration(new TestUserNodeConfiguration()).build());
		final var treeView = this.fileSystemTreeView.getComponent();
		final var borderPane = new BorderPane();
		final var scene = new Scene(borderPane, 800, 600);
		stage.setScene(scene);
		if (isDemoMode())
		{
			stage.setTitle(FileSystemTreeViewFX.class.getSimpleName() + " – Simple Demo");
			final var listView = new ListView<Path>();
			final var splitPane = new SplitPane(treeView, listView);
			final var label = new Label();
			label.setPadding(new Insets(4, 0, 8, 0));
			borderPane.setPadding(new Insets(8));
			borderPane.setCenter(splitPane);
			borderPane.setTop(label);
			label.textProperty().bind(
				fileSystemTreeView.selectedPathProperty().asString("Selected path : »%s«"));
			fileSystemTreeView.selectedPathProperty().addListener((observable, oldValue, newValue) ->
			{
				if (newValue != null)
				{
					try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(newValue))
					{
						final List<Path> list = new ArrayList<>();
						directoryStream.forEach(path -> list.add(path.getFileName()));
						list.sort(null);
						listView.setItems(FXCollections.observableArrayList(list));
					}
					catch (IOException ex)
					{
						listView.setItems(FXCollections.emptyObservableList());
					}
				}
				else
				{
					listView.setItems(FXCollections.emptyObservableList());
				}
			});
			stage.setOnCloseRequest(event -> countDownLatchShutdown.countDown());
			countDownLatchShutdown = new CountDownLatch(1);
			stage.show();
		}
		else
		{
			borderPane.setCenter(treeView);
		}
		countDownLatchLaunch.countDown();
	}

	static boolean awaitCountDownLatchLaunch() throws InterruptedException
	{
		return countDownLatchLaunch.await(10, SECONDS);
	}

	static void awaitCountDownLatchShutdown() throws InterruptedException
	{
		countDownLatchShutdown.await();
	}

	static void runTest()
	{
		run(false);
	}

	static void runDemo()
	{
		run(true);
	}

	private static void run(boolean showWindow)
	{
		launch("--" + PARAMETER_NAME_SHOW_WINDOW + "=" + showWindow);
	}

	static TestApplication getTestApplication()
	{
		return testApplication;
	}

	FileSystemTreeView getFileSystemTreeView()
	{
		return fileSystemTreeView;
	}
}
