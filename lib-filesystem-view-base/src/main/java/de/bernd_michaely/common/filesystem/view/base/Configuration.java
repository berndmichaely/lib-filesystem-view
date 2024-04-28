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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Comparator;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Configuration with builder for file system tree view factories.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public record Configuration(
  FileSystem fileSystem,
  boolean requestWatchService,
  Comparator<String> fileNameComparator,
  UserNodeConfiguration userNodeConfiguration)
  {
  /**
   * Builder for file system tree view configurations.
   */
  public static class Builder
  {
    private @Nullable FileSystem fileSystem;
    private boolean requestingWatchService = true;
    private @Nullable Comparator<String> fileNameComparator;
    private @Nullable UserNodeConfiguration userNodeConfiguration;

    private Builder()
    {
    }

    /**
     * Sets a file system. In case of null, which is the default, the
     * {@link java.nio.file.FileSystems#getDefault() default file system} will
     * be used.
     *
     * @param fileSystem the file system to use
     * @return this builder
     */
    public Builder setFileSystem(@Nullable FileSystem fileSystem)
    {
      this.fileSystem = fileSystem;
      return this;
    }

    /**
     * Set to true to request a watch service.
     *
     * @param requestingWatchService true to request a watch service
     * @return this builder
     */
    public Builder setRequestingWatchService(boolean requestingWatchService)
    {
      this.requestingWatchService = requestingWatchService;
      return this;
    }

    /**
     * Sets a filename comparator. In case of null, which is the default, the
     * default {@link String#compareTo(String)} will be used.
     *
     * @param fileNameComparator the filename comparator to use
     * @return this builder
     */
    public Builder setFileNameComparator(@Nullable Comparator<String> fileNameComparator)
    {
      this.fileNameComparator = fileNameComparator;
      return this;
    }

    /**
     * Sets a user node configuration.
     *
     * @param userNodeConfiguration the UserNodeConfiguration to use
     * @return this builder
     */
    public Builder setUserNodeConfiguration(@Nullable UserNodeConfiguration userNodeConfiguration)
    {
      this.userNodeConfiguration = userNodeConfiguration;
      return this;
    }

    /**
     * Returns the final Configuration. All references built with this builder
     * will be non null.
     *
     * @return the final Configuration
     */
    public Configuration build()
    {
      final FileSystem fs = fileSystem != null ? fileSystem : FileSystems.getDefault();
      final Comparator<String> comp = fileNameComparator != null ? fileNameComparator :
        (name1, name2) -> fs.getPath(name1).compareTo(fs.getPath(name2));
      final UserNodeConfiguration configuration = userNodeConfiguration != null ? userNodeConfiguration :
        SimpleUserNodeConfiguration.getInstance();
      return new Configuration(fs, requestingWatchService, comp, configuration);
    }
  }

  /**
   * Returns a builder instance.
   *
   * @return a builder instance
   */
  public static Builder builder()
  {
    return new Builder();
  }
}
