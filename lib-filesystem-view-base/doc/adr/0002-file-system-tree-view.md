# 2. File System Tree View

Date: 2021-10-17

## Status

Accepted

Details of Modularization [3. Modularization](0003-modularization.md)

Details of Extensibility [4. Extensibility](0004-extensibility.md)

Details of Persistency [5. Persistency](0005-persistency.md)

## Context

Many GUI desktop applications need an efficient, user friendly access to the file system, which is usually provided by a graphical tree view. So, for applications written in Java, it is obviously desirable to have a reusable component available, say a »FileSystemTreeView« component (FSTV).

For such a component to be reusable, it should – beyond the usual functionality a user would expect – satisfy the following conditions:

* It needs support for a dedicated DCF mode (that is, directory recursion stops at "DCIM" directories, and the application shows a logical view of the hidden subdirectory structure).
* It should work on all supported OS's.
* It should be available for all current GUI libraries (that is Swing and OpenJavaFX).
* The state of the tree view should be persistable (see separate ADR).
* It should be modularized (see separate ADR).
* The sub-directories (including file system roots, which is OS dependent) should be watched for changes by use of a watch service.
* The sort order of the directory content should be customizable (at least initially).

Furthermore, there are the following desirable optional properties:

* The sort order of the directory content should be customizable and be changeable at runtime.
* It should be extendable to less common conditions (e.g. to provide the content of special files like zip files as sub tree; see separate ADR).

## Decision

We will implement a reusable Java component providing the primary properties described above. The implementation of the optional properties is left for next versions.

## Consequences

We will have available a ready to use Java component providing the properties described above in the future. It will be usable e.g. as part of a GUI main window or inside a dialog.
