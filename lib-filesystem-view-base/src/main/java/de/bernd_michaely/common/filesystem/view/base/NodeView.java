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
package de.bernd_michaely.common.filesystem.view.base;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

/**
 * Encapsulation of the actual tree node UI component.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public interface NodeView
{
	/**
	 * The implementation must insert the given SubNodeViews at the given indices
	 * in the given sort order.
	 *
	 * @param mapSubNodeViews sorted map containing the NodeViews to add with
	 *                        indices in sort order
	 */
	void insertSubNodes(SortedMap<Integer, NodeView> mapSubNodeViews);

	/**
	 * The implementation must append the given subNodeViews in the collections
	 * iteration order. It is assumed that this sub node list is currently empty.
	 *
	 * @param subNodeViews the given sub NodeViews
	 */
	void addAllSubNodes(Collection<NodeView> subNodeViews);

	/**
	 * The implementation must remove the sub nodes at the given indices in the
	 * order given by the list.
	 *
	 * @param indices the given indices to remove in list order
	 */
	void removeSubNodes(List<Integer> indices);

	/**
	 * The implementation must remove all sub nodes.
	 */
	void clear();

	/**
	 * Sets the expanded state of the node view.
	 *
	 * @param expanded the expanded state
	 */
	void setExpanded(boolean expanded);

	/**
	 * Determines, whether this node is conceptually a leaf node.
	 *
	 * @param leafNode false to indicate, that this node should be conceptually a
	 *                 leaf node, true, if it is allowed to have sub nodes
	 */
	void setLeafNode(boolean leafNode);
}
