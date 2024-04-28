/* Created on Sep 20, 2021 */

/**
 * Module implementing a file system tree view for Java Swing.
 */
module de.bernd_michaely.common.filesystem.view.swing
{
	requires de.bernd_michaely.common.filesystem.view.base;
	requires java.desktop;
	requires org.checkerframework.checker.qual;

	exports de.bernd_michaely.common.filesystem.view.swing;
}
