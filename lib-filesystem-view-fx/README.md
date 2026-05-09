# lib-filesystem-view-fx

![version](doc/shields/lib-filesystem-view-fx.svg "version")

This directory contains the module providing the [JavaFX](https://openjfx.io/) based implementation of the `lib-filesystem-view` library consisting of modules:

  * `lib-filesystem-view-base`
  * **`lib-filesystem-view-fx`**
  * `lib-filesystem-view-swing`

See the [README](../README.md) file in the libraries main directory.

#### Maven coordinates

Releases are available at:

```
de.bernd-michaely::lib-filesystem-view-fx:${version}
```

#### Module info

```java
module de.bernd_michaely.common.filesystem.view.fx
{
  requires de.bernd_michaely.common.filesystem.view.base;
  requires javafx.controls;
  requires org.checkerframework.checker.qual;

  exports de.bernd_michaely.common.filesystem.view.fx;
}
```
