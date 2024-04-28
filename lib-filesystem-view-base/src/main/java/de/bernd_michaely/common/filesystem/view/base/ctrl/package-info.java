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
/**
 * Internal control sub package of base module of FileSystemTreeView component.
 *
 * <h2>Implementation notes</h2>
 * <h3>UML diagrams</h3>
 * <ul>
 * <li>
 * The following <a href="{@docRoot}/uml/fstv-sequence.svg">sequence diagram
 * <img src="{@docRoot}/uml/fstv-sequence.svg" height=100 alt="sequence diagram"/></a>
 * describes the internal interaction of the FileSystemTreeView base component
 * with UI, FileSystem and WatchService.
 * </li>
 * <li>
 * The following <a href="{@docRoot}/uml/fstv-node-types-class.svg">class
 * diagram
 * <img src="{@docRoot}/uml/fstv-node-types-class.svg" height=100 alt="class diagram"/></a>
 * shows the {@link NodeCtrl} type hierarchy and the corresponding directory
 * entry types.
 * </li>
 * </ul>
 */
package de.bernd_michaely.common.filesystem.view.base.ctrl;
