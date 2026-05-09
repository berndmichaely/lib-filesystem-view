/*
 * Copyright 2026 Bernd Michaely (info@bernd-michaely.de).
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
package de.bernd_michaely.common.filesystem.view.swing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * JFileSystemTreeViewVersion Test.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class JFileSystemTreeViewVersionTest
{
	/**
	 * Test of getSwingModuleVersion method, of class JFileSystemTreeViewVersion.
	 */
	@Test
	public void testGetSwingModuleVersion()
	{
		System.out.println("getSwingModuleVersion");
		assertFalse(JFileSystemTreeViewVersion.getSwingModuleVersion().isBlank());
		assertFalse(JFileSystemTreeViewVersion.getSwingModuleVersion().isBlank());
	}
}
