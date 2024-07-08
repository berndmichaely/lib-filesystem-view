/* Created on Oct 5, 2021 */
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.IFileSystemTreeView;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import java.nio.file.Path;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Interface describing API for file system tree views with additional JavaFX
 * related features.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public interface FileSystemTreeView extends IFileSystemTreeView
{
	/**
	 * Creates a new instance with the default configuration.
	 *
	 * @return a new instance with the default configuration
	 */
	static FileSystemTreeView createInstance()
	{
		return createInstance(null);
	}

	/**
	 * Creates a new instance with the given configuration.
	 *
	 * @param configuration the given configuration. If null, a default
	 *                      configuration will be used.
	 * @return a new instance with the given configuration
	 */
	static FileSystemTreeView createInstance(@Nullable Configuration configuration)
	{
		return new FileSystemTreeViewFX(configuration != null ?
			configuration : Configuration.getDefault());
	}

	/**
	 * Property indicating the currently selected path.
	 *
	 * @return the selected path, null, if none is selected
	 */
	ReadOnlyObjectProperty<@Nullable Path> selectedPathProperty();

	/**
	 * Returns the currently selected path.
	 *
	 * @return the selected path, null, if none is selected
	 */
	@Override
	default @Nullable
	Path getSelectedPath()
	{
		return selectedPathProperty().get();
	}

	/**
	 * Property indicating the currently selected path.
	 *
	 * @return the selected path, null, if none is selected
	 */
	ReadOnlyBooleanProperty pathSelectedProperty();

	/**
	 * Returns true, iff a path is currently selected.
	 *
	 * @return true, iff a path is currently selected
	 */
	@Override
	default boolean isPathSelected()
	{
		return pathSelectedProperty().get();
	}

	/**
	 * Returns the main GUI component to be included in the client GUI. This may
	 * be the main tree control or a containing layout pane, which is
	 * intentionally unspecified and may vary between versions.
	 *
	 * @return the main GUI component
	 */
	Region getComponent();

	/**
	 * Returns the main tree control.
	 *
	 * @return the main tree control
	 */
	TreeView<PathView> getTreeView();
}
