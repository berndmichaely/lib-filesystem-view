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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utility class to create paths for unit testing.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class PathFactory
{
  private final FileSystem fileSystem;
  private String rootPath;
  private final SortedSet<Path> paths = new TreeSet<>();
  private int width;
  private char c;

  /**
   * Constructor using {@link FileSystems#getDefault()}.
   */
  public PathFactory()
  {
    this.fileSystem = FileSystems.getDefault();
  }

  /**
   * Common constructor to create paths.
   *
   * @param fileSystem the file system to create paths for
   * @param rootPath   the file system root, e.g. {@code "/"} or {@code "C:\\"}
   */
  public PathFactory(FileSystem fileSystem, String rootPath)
  {
    this.fileSystem = fileSystem;
    this.rootPath = rootPath;
  }

  public FileSystem getFileSystem()
  {
    return fileSystem;
  }

  public String getRootPath()
  {
    return rootPath;
  }

  public void setRootPath(String rootPath)
  {
    this.rootPath = rootPath;
  }

  /**
   * Returns the components of the given path as a String list.
   *
   * @param path the given path
   * @return the components of the given path as a String list
   */
  static List<String> getPathComponents(Path path)
  {
    final var list = new ArrayList<String>(1 + path.getNameCount());
    final Path root = path.getRoot();
    list.add(root != null ? root.toString() : "");
    path.iterator().forEachRemaining(p -> list.add(p.toString()));
    return list;
  }

  /**
   * Same as {@link #createPaths(int, int, char) createPaths(3, 3, 'a')}.
   *
   * @return a set of created paths
   */
  public SortedSet<Path> createPaths()
  {
    return createPaths(3, 3, 'a');
  }

  /**
   * Creates the constructed path set in the initially given file system.
   *
   * @throws IOException
   */
  public void mkdirPaths() throws IOException
  {
//    System.out.println("MkDir Paths:");
    for (Path path : paths)
    {
      Files.createDirectories(path);
//      System.out.println("-> " + path);
    }
  }

  /**
   * Prints the created paths to stdout.
   */
  public void logPaths()
  {
    System.out.println("Create Paths:");
    for (Path path : paths)
    {
      System.out.println("-> " + path);
    }
  }

  private void _createPaths(String[] dirs, int depth)
  {
    for (int w = 0; w < width; w++)
    {
      dirs[depth] = Character.toString(((char) c + w));
      if (depth < dirs.length - 1)
      {
        _createPaths(dirs, depth + 1);
      }
      else
      {
        paths.add(fileSystem.getPath(rootPath, dirs));
      }
    }
  }

  /**
   * Creates a set of paths. E.g. for {@code depth=3, width=2, c='a'} the
   * created paths are:
   * <ul>
   * <li>{@code /a/a/a}</li>
   * <li>{@code /a/a/b}</li>
   * <li>{@code /a/b/a}</li>
   * <li>{@code /a/b/b}</li>
   * <li>{@code /b/a/a}</li>
   * <li>{@code …}</li>
   * <li>{@code /b/b/b}</li>
   * </ul>
   *
   * @param depth the number of elements of the created paths
   * @param width the number of subdirectories
   * @param c     the starting char used for incremental naming
   * @return a set of created paths
   */
  public SortedSet<Path> createPaths(int depth, int width, char c)
  {
    this.width = width;
    this.c = c;
    paths.clear();
    _createPaths(new String[depth], 0);
    return paths;
  }
}
