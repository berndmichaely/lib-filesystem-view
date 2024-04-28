/* Created on Aug 28, 2021 */

/**
 * Module implementing a file system tree view for JavaFX.
 */
module de.bernd_michaely.common.filesystem.view.fx
{
	requires de.bernd_michaely.common.filesystem.view.base;
	requires javafx.controls;
	requires org.checkerframework.checker.qual;

	exports de.bernd_michaely.common.filesystem.view.fx;
}
