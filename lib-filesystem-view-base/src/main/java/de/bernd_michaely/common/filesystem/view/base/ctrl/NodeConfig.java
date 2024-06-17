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

import de.bernd_michaely.common.filesystem.view.base.NodeView;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import de.bernd_michaely.common.filesystem.view.base.UserNodeConfiguration;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Class to describe a configuration global to all tree nodes.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class NodeConfig
{
	private final Function<PathView, NodeView> nodeViewFactory;
	private final WatchServiceCtrl watchServiceCtrl;
	private final Supplier<Comparator<String>> fileNameComparatorSupplier;
	private final Supplier<Comparator<DirectoryEntry>> directoryEntryComparatorSupplier;
	private final UserNodeConfiguration userNodeConfiguration;

	NodeConfig(Function<PathView, NodeView> nodeViewFactory, WatchServiceCtrl watchServiceCtrl,
		Comparator<String> fileNameComparator, UserNodeConfiguration userNodeConfiguration)
	{
		this.nodeViewFactory = requireNonNull(nodeViewFactory,
			getClass().getName() + " : nodeViewFactory is null");
		this.watchServiceCtrl = requireNonNull(watchServiceCtrl,
			getClass().getName() + " : watchServiceConfig is null");
		this.fileNameComparatorSupplier = () -> fileNameComparator;
		this.directoryEntryComparatorSupplier = () ->
			(entry1, entry2) -> fileNameComparatorSupplier.get().compare(entry1.getName(), entry2.getName());
		this.userNodeConfiguration = userNodeConfiguration;
	}

	Function<PathView, NodeView> getNodeViewFactory()
	{
		return nodeViewFactory;
	}

	WatchServiceCtrl getWatchServiceCtrl()
	{
		return watchServiceCtrl;
	}

	Supplier<Comparator<String>> getFileNameComparatorSupplier()
	{
		return fileNameComparatorSupplier;
	}

	Supplier<Comparator<DirectoryEntry>> getDirectoryEntryComparatorSupplier()
	{
		return directoryEntryComparatorSupplier;
	}

	/**
	 * Returns a new UserNodeConfiguration for the given path.
	 *
	 * @param path the path to create a new UserNodeConfiguration for
	 * @return a new UserNodeConfiguration for the given path (might or might not
	 *         be a singleton)
	 */
	UserNodeConfiguration getUserNodeConfiguration(Path path)
	{
		return userNodeConfiguration.getUserNodeConfigurationFor(path);
	}
}
