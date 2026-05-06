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
import java.nio.file.Path;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;

/**
 * TreeCell to customize symbolic links. SymLinks will be shown in italic font,
 * and a tooltip shows the link target.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class TreeCellPathView extends TreeCell<PathView>
{
	private Font fontDefault = Font.getDefault();
	private Font fontSymLink = Font.getDefault();

	private TreeCellPathView()
	{
	}

	/**
	 * Factory method to create TreeCell instances.
	 *
	 * @param treeView the corresponding TreeView
	 * @return a new TreeCell instance
	 * @see TreeView#setCellFactory(javafx.util.Callback)
	 */
	public static TreeCellPathView factory(TreeView<PathView> treeView)
	{
		final var newInstance = new TreeCellPathView();
		final var font = newInstance.getFont();
		newInstance.fontDefault = font;
		newInstance.fontSymLink = Font.font(font.getFamily(), FontPosture.ITALIC, font.getSize());
		return newInstance;
	}

	@SuppressWarnings("argument")
	private void clearItem()
	{
		setText(null);
		setGraphic(null);
	}

	@SuppressWarnings("argument")
	private void clearTooltip()
	{
		setTooltip(null);
	}

	@Override
	protected void updateItem(PathView item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty || item == null)
		{
			clearItem();
			clearTooltip();
		}
		else
		{
			setText(item.toString());
			final Path symbolicLinkTarget = item.getSymbolicLinkTarget();
			if (symbolicLinkTarget != null)
			{
				setFont(fontSymLink);
				setTooltip(new Tooltip(symbolicLinkTarget.toString()));
			}
			else
			{
				setFont(fontDefault);
				clearTooltip();
			}
		}
	}
}
