# FileSystemTreeView component – Usage

## Basic Usage

Basically, use the factory methods of the main interfaces to create a new instance, that is

`JFileSystemTreeView.createInstance()` for the `-swing` version or

`FileSystemTreeView.createInstance()` for the `-fx` version.

The `createInstance()` methods optionally take a `Configuration` argument, which can be configured via the `builder()`. The parameterless versions behave like passing `Configuration.builder().build()`.

Most enhanced configuration options can be provided by passing an implementation of the `UserNodeConfiguration` interface.

## Advanced Usage

### The Configuration Builder

The `Configuration Builder` allows to set a few properties for the created instance, all with a useful default:

* a specific (global root) **FileSystem** to use,
* requesting of a **WatchService**,
* setting a specific **filename comparator** and
* an instance of a custom implementation of the **`UserNodeConfiguration`** `interface`, which allows for further advanced configuration.

### The UserNodeConfiguration interface

#### Factory method

The interface provides the `UserNodeConfiguration getUserNodeConfigurationFor(Path)` factory method to create new instances for new treenodes on demand. This method may be implemented in several ways, e.g.:

* as a *Singleton* for simple configurations (see "The SimpleUserNodeConfiguration class" below)
* as a POJO, simply returning a new instance per call, to provide a per node configuration
* as a *Prototype* to pass around an initially given global configuration object in addition.

#### Tree node creation

When you think about what would be configurable in general for a tree node, you may identify essentially two main things:

1. A tree node for a specific directory or file may be *created* or not, and
2. if a tree node is created, it my be conceptually an *inner* or a *leaf* node, that is it may be allowed to have children or not.

The first aspect can be controlled via the `isCreatingNodeForDirectory(Path directory)` and `isCreatingNodeForFile(Path file)`, the second via the `isLeafNode(Path)` Path predicates.

A *directory node* unsurprisingly will create subnodes for the directory entries. A *file node* can mount a subtree for a `java.nio.file.FileSystem` created on demand, e.g. a Zip FileSystem from  a `*.zip` file. (You might even place your custom configuration file describing any resource, e.g. `device=/dev/sdx7` to integrate some native functionality.)

Creation of custom FileSystems is controlled via the `createFileSystemFor(Path file)` method (obviously the `isCreatingNodeForFile(Path file)` must return `true` for that path), and the `onClosingFileSystem(FileSystem)` method, which allows for cleanup of resources when a treenode is collapsed.

#### Notifications

When a client wants to change the state of the component (e.g. to switch between *showing* or *hiding* hidden directories), it needs a way to notify the component about the change.

The client can request to recieve a notification callback object by overwriting the `boolean isRequestingUpdateNotifier()` method to return true.

In this case, the component will call the `setUpdateNotifier(Runnable callback)` method  for each created node to provide a callback object the client has to remember. Whenever the client wants the component to adjust itself to a changed state, it has to call the `run()` method of the callback.

#### Listing

```java
public interface UserNodeConfiguration
{
  LinkOption[] DEFAULT_LINK_OPTIONS = new LinkOption[0];

  default LinkOption[] getLinkOptions()
  {
    return DEFAULT_LINK_OPTIONS;
  }

  default boolean isCreatingNodeForDirectory(Path directory)
  {
    try
    {
      return !Files.isHidden(directory);
    }
    catch (IOException ex)
    {
      return false;
    }
  }

  default boolean isCreatingNodeForFile(Path file)
  {
    return false;
  }

  default @Nullable
  FileSystem createFileSystemFor(Path file)
  {
    return null;
  }

  default void onClosingFileSystem(FileSystem fileSystem)
  {
  }

  default boolean isLeafNode(Path path)
  {
    return false;
  }

  default boolean isRequestingUpdateNotifier()
  {
    return false;
  }

  default void setUpdateNotifier(Runnable callback)
  {
  }

  UserNodeConfiguration getUserNodeConfigurationFor(Path path);
}
```

### The SimpleUserNodeConfiguration class

The `SimpleUserNodeConfiguration` class provides a simple default configuration which is used by default in the configuration builder.

Since the `UserNodeConfiguration` interface has useful default implementations for all methods but the factory method, the `SimpleUserNodeConfiguration` class only adds an implementation for this method to return a singleton instance.

