/* Created on Mar 13, 2023 */
package de.bernd_michaely.common.filesystem.view.swing;

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.IFileSystemTreeView;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JTree;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Interface describing API for file system tree views with additional features
 * specific for Swing.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public interface JFileSystemTreeView extends IFileSystemTreeView
{
	/**
	 * Creates a new instance with the default configuration.
	 *
	 * @return a new instance with the default configuration
	 */
	static JFileSystemTreeView createInstance()
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
	static JFileSystemTreeView createInstance(@Nullable Configuration configuration)
	{
		return new JFileSystemTreeViewImpl(configuration != null ?
			configuration : Configuration.builder().build());
	}

	/**
	 * Returns the main GUI component.
	 *
	 * @return the main GUI component
	 */
	JComponent getComponent();

	/**
	 * Returns the main GUI component to be included in the client GUI. This may
	 * be the main tree control or a containing layout pane, which is
	 * intentionally unspecified and may vary between versions.
	 *
	 * @return the main GUI component
	 */
	JTree getTree();

	/**
	 * Add a path selection listener.
	 *
	 * @param consumer a path selection listener
	 */
	void addPathSelectionListener(Consumer<@Nullable Path> consumer);

	/**
	 * Remove a path selection listener.
	 *
	 * @param consumer a path selection listener
	 * @return true, if a listener has been removed
	 */
	boolean removePathSelectionListener(Consumer<@Nullable Path> consumer);
}
