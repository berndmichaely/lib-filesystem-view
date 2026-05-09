# lib-filesystem-view-swing

![version](doc/shields/lib-filesystem-view-swing.svg "version")

This directory contains the module providing the Java Swing based implementation of the `lib-filesystem-view` library consisting of modules:

  * `lib-filesystem-view-base`
  * `lib-filesystem-view-fx`
  * **`lib-filesystem-view-swing`**

See the [README](../README.md) file in the libraries main directory.

There is also the script `demo/FilesystemViewSwingDemo.groovy`, which can be used as demo or template.

#### Maven coordinates

Releases are available at:

```
de.bernd-michaely::lib-filesystem-view-swing:${version}
```

#### Module info

```java
module de.bernd_michaely.common.filesystem.view.swing
{
  requires de.bernd_michaely.common.filesystem.view.base;
  requires java.desktop;
  requires org.checkerframework.checker.qual;

  exports de.bernd_michaely.common.filesystem.view.swing;
}
```
