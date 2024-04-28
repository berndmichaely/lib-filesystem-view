# 4. Extensibility

Date: 2021-10-17

## Status

Accepted

Details of Extensibility [2. File System Tree View](0002-file-system-tree-view.md)

## Context

The FSTV should, as already stated, be extendable to less common conditions, e.g. to provide the content of special files like zip files as sub tree.

## Decision

The FSTV will optionally accept a function mapping a file name extension (e.g. `.zip`) to a `java.nio.file.FileSystem`.

## Consequences

The FSTV will be flexible enough to be extended in the future to provide insight directly into archive formats, virtual file systems, ISO images and so on through the standard interface of `FileSystem`.
