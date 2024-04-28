# 5. Persistency

Date: 2021-10-17

## Status

Accepted

Details of Persistency [2. File System Tree View](0002-file-system-tree-view.md)

## Context

The FSTV should, as already stated, be able to persist the state of its tree view, e.g. between incarnations of an application.

## Decision

The state will be persisted using the following format:

  * The data will be kept as a string list (e.g. a plain text file containing one entry per line).
  * By depth first recursion over the tree view to the leaves, a list of expanded paths will be retrieved and written to the data.
  * An additional string will be appended to the previous list containing the index (in the range of 0 to n-1) of the currently selected path, if any, and otherwise containing a negative value, e.g. `-1`.

## Consequences

Persistence of a FSTV can be achieved by a simple mechanism which will allow an application to integrate a single string list into its own persistence mechanism, in the simplest case writing and reading the date to and from a simple plain text file through standard I/O streams.
