# 3. Modularization

Date: 2021-10-17

## Status

Accepted

Details of Modularization [2. File System Tree View](0002-file-system-tree-view.md)

## Context

The FSTV should, as already stated, be available for multiple GUI libraries, in particular Swing and OpenJavaFX. Nonetheless a single application will typically use only one of the available versions (an exception may be a demo application showing all versions in one).

The GUI libraries bring their own dependencies:

  * Swing needs the `java.desktop` module which in turn needs `java.xml` and `java.datatransfer`
  * OpenJavaFX needs (at least) the even platform dependent modules `javafx.base`, `javafx.controls` and `javafx.graphics`.

For this reason, the different GUI versions should be implemented in separate modules to keep their dependencies apart.

On the other hand, the data model and the largest part of the control logic should be GUI agnostic. They should be kept in a separate base module to ease unit testing and minimize effort of constructing GUI elements for a specific library.

## Decision

We will implement the following JPMS module and packages:

### Base module

Name: `de.bernd_michaely.common.filesystem.view.base`

This module contains the data model and the largest part of the control logic and unit tests.

### OpenJavaFX module

Name: `de.bernd_michaely.common.filesystem.view.fx`

This module contains a minimum of code to construct the JavaFX specific GUI elements and their event handling calling the necessary callback methods.

### Swing module

Name: `de.bernd_michaely.common.filesystem.view.swing`

This module contains a minimum of code to construct the Swing specific GUI elements and their event handling calling the necessary callback methods.

## Consequences

We will have available the different versions of the FSTV component in well defined and separated modules. A single usage of a version will have only the module dependencies it really needs.

Furthermore the extraction of a base module containing the largest part of the control logic minimizes the effort to adapt the component to a specific GUI library and also provides an optimal base for unit testing and therefore maintainability.
