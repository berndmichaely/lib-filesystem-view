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

import java.nio.file.Path;

/**
 * A simple node configuration to show non hidden directories only. This class
 * is a singleton.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class SimpleUserNodeConfiguration implements UserNodeConfiguration
{
  private static final SimpleUserNodeConfiguration instance = new SimpleUserNodeConfiguration();

  private SimpleUserNodeConfiguration()
  {
  }

  /**
   * Returns the singleton instance.
   *
   * @return the singleton instance
   */
  public static SimpleUserNodeConfiguration getInstance()
  {
    return instance;
  }

  /**
   * Returns the singleton instance.
   *
   * @param path unused (the configuration is shared for all paths)
   * @return the singleton instance
   */
  @Override
  public UserNodeConfiguration getUserNodeConfigurationFor(Path path)
  {
    return getInstance();
  }
}
