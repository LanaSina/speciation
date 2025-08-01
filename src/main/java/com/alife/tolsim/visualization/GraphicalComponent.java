package com.alife.tolsim.visualization;

import java.awt.Graphics;

/**
 * Objects that are to be drawn on the UI pannel have to implement this interface
 * @author lana
 *
 */
public interface GraphicalComponent {

	long getID();
	void draw(Graphics g, int gridStep);

	//public boolean onTop = false;
}
