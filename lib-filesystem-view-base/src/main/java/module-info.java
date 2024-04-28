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
 * Base module for a file system view containing the common control logic.
 * <h2>Implementation notes</h2>
 * The following
 * <a href="{@docRoot}/uml/fstv-api-class.svg">UML diagram
 * <img src="{@docRoot}/uml/fstv-api-class.svg" width=100 alt="UML diagram – types of the public API" /></a>
 * shows the essential types of the public API.
 */
module de.bernd_michaely.common.filesystem.view.base
{
	requires org.checkerframework.checker.qual;

	exports de.bernd_michaely.common.filesystem.view.base;
	exports de.bernd_michaely.common.filesystem.view.base.common;
}
