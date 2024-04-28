# [lib-filesystem-view-base](https://github.com/berndmichaely/jem/lib-common/lib-filesystem-view/lib-filesystem-view-base)

by [Bernd Michaely](https://bernd-michaely.de/en)

This directory contains the base module of the `lib-filesystem-view` library consisting of modules:

* **`lib-filesystem-view-base`**
* `lib-filesystem-view-fx`
* `lib-filesystem-view-swing`

This abstract base module

* is user interface independant (that is it is not limited to any particular UI like Swing or JavaFX, or even to a *graphical* UI)
* contains the main controller providing
    * the filesystem data model for the tree view, integrated with a watch service
    * embedded filesystems
* contains most unit tests.

This module is a dependency of the `-swing` and `-fx` modules and won't be used directly, besides you want to provide your own implementation (see below).

See the [README](../README.md) file in the libraries main directory.

## Providing your own implementation

The essential steps are:

* Extending the `IFileSystemTreeView` interface, providing an additional factory mechanism for your own component (and possibly some additional functionality, e.g. JavaFX properties)
* Providing a class implementating this interface, containing a UI tree view component representing the data model provided by the `-base` module
* Providing an implementation of the `NodeView` interface, containing essentially some factory methods to provide the specific UI components

Examples are provides by the `-swing` and `-fx` modules as well as the unit tests of the `-base` module.

