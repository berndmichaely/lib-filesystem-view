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
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.PathView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotLinkException;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.System.Logger.Level.WARNING;

/**
 * TreeCell to customize symbolic links. SymLinks will be shown in italic font,
 * and a tooltip shows the link target.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class TreeCellPathView extends TreeCell<PathView>
{
	private static final System.Logger logger = System.getLogger(TreeCellPathView.class.getName());
	private boolean isSymLinkSupported = true;
	private @Nullable Font fontDefault;
	private @Nullable Font fontSymLink;

	private TreeCellPathView(TreeView<PathView> treeView)
	{
	}

	static TreeCellPathView createInstance(TreeView<PathView> treeView)
	{
		final var treeCell = new TreeCellPathView(treeView);
		treeCell.fontProperty().addListener(treeCell::onFontChange);
		treeCell.onFontChange(null, null, treeCell.getFont());
		return treeCell;
	}

	private void onFontChange(@Nullable ObservableValue<? extends Font> obs,
		@Nullable Font oldValue, @Nullable Font newValue)
	{
		if (newValue != null)
		{
			fontDefault = newValue;
			fontSymLink = Font.font(newValue.getFamily(), FontPosture.ITALIC, newValue.getSize());
		}
		else
		{
			fontDefault = null;
			fontSymLink = null;
		}
	}

	@SuppressWarnings(value = "argument")
	private void clear()
	{
		setText(null);
		setGraphic(null);
	}

	@Override
	protected void updateItem(PathView item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty || item == null)
		{
			clear();
		}
		else
		{
			final String title = item.toString();
			setText(title);
			if (isSymLinkSupported)
			{
				final var path = item.getPath();
				try
				{
					final boolean isSymLink = Files.isSymbolicLink(path);
					if (fontDefault != null && fontSymLink != null)
					{
						setFont(isSymLink ? fontSymLink : fontDefault);
						if (isSymLink)
						{
							setTooltip(new Tooltip(title + " → " + Files.readSymbolicLink(path)));
						}
					}
				}
				catch (UnsupportedOperationException ex)
				{
					isSymLinkSupported = false;
				}
				catch (NotLinkException ex)
				{
				}
				catch (IOException ex)
				{
					logger.log(WARNING, () -> "Trying to read SymLink for path : " + path, ex);
				}
			}
		}
	}
}
