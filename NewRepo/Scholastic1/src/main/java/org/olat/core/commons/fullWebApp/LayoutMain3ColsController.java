/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) 2007 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.core.commons.fullWebApp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.gui.control.generic.layout.MainLayout3ColumnsController;

/**
 * <h3>Description:</h3> This main layout controller provides a three column layout based on the YAML framework. You must use the the BaseFullWebappController as parent
 * controller or implement the necessary YAML HTML wrapper markup code yourself.
 * <p>
 * The meaning of the col1, col2 and col3 are strictly following the YAML concept. This means, that in a brasato web application in most cases the following mapping is
 * applied:
 * <ul>
 * <li>col1: menu</li>
 * <li>col2: toolboxes</li>
 * <li>col3: content area</li>
 * </ul>
 * Read the YAML specification if you don't understand why this is. Rendering is all done using CSS.
 * <p>
 * For information about YAML please see @see http://www.yaml.de
 * <p>
 * <h3>Events thrown by this controller:</h3>
 * <ul>
 * <li>none</li>
 * </ul>
 * <p>
 * Initial Date: 11.10.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class LayoutMain3ColsController extends MainLayoutBasicController implements MainLayout3ColumnsController {
	private VelocityContainer layoutMainVC;
	// current columns components
	private Component[] columns = new Component[3];
	// current css classes for the main div
	private Set<String> mainCssClasses = new HashSet<String>();
	private LayoutMain3ColsConfig localLayoutConfig;
	private String layoutConfigKey = null;
	private Panel panel1, panel2, panel3;

	/**
	 * Constructor for creating a 3 col based menu on the main area. This constructor uses the default column width configuration
	 * 
	 * @param ureq
	 * @param wControl
	 * @param col1 usually the left column
	 * @param col2 usually the right column
	 * @param col3 usually the content column
	 * @param layoutConfigKey identificator for this layout to persist the users column width settings
	 */
	public LayoutMain3ColsController(UserRequest ureq, WindowControl wControl, Component col1, Component col2, Component col3, String layoutConfigKey) {
		this(ureq, wControl, col1, col2, col3, layoutConfigKey, null);
	}

	/**
	 * Constructor for creating a 3 col based menu on the main area
	 * 
	 * @param ureq
	 * @param wControl
	 * @param col1 usually the left column
	 * @param col2 usually the right column
	 * @param col3 usually the content column
	 * @param layoutConfigKey identificator for this layout to persist the users column width settings
	 * @param defaultConfiguration The layout width configuration to be used
	 */
	public LayoutMain3ColsController(UserRequest ureq, WindowControl wControl, Component col1, Component col2, Component col3, String layoutConfigKey,
			LayoutMain3ColsConfig defaultConfiguration) {
		super(ureq, wControl);
		this.layoutMainVC = createVelocityContainer("main_3cols");
		this.layoutConfigKey = layoutConfigKey;

		localLayoutConfig = getGuiPrefs(ureq, defaultConfiguration);

		// Push colums to velocity
		panel1 = new Panel("panel1");
		layoutMainVC.put("col1", panel1);
		setCol1(col1);

		panel2 = new Panel("panel2");
		layoutMainVC.put("col2", panel2);
		setCol2(col2);

		panel3 = new Panel("panel3");
		layoutMainVC.put("col3", panel3);
		setCol3(col3);

		putInitialPanel(layoutMainVC);
	}

	/**
	 * Add a controller to this layout controller that should be cleaned up when this layout controller is diposed. In most scenarios you should hold a reference to the
	 * content controllers that controll the col1, col2 or col3, but in rare cases this is not the case and you have no local reference to your controller. You can then
	 * use this method to add your controller. At the dispose time of the layout controller your controller will be disposed as well.
	 * 
	 * @param toBedisposedControllerOnDispose
	 */
	@Override
	public void addDisposableChildController(Controller toBedisposedControllerOnDispose) {
		listenTo(toBedisposedControllerOnDispose);
	}

	/**
	 * The Controller to be set on the mainPanel in case of disposing this layout controller.
	 * 
	 * @param disposedMessageControllerOnDipsose
	 */
	public void setDisposedMessageController(Controller disposedMessageControllerOnDipsose) {
		this.setDisposedMsgController(disposedMessageControllerOnDipsose);
	}

	/**
	 * Add a css class to the #b_main wrapper div, e.g. for special background formatting
	 * 
	 * @param cssClass
	 */
	@Override
	public void addCssClassToMain(String cssClass) {
		if (mainCssClasses.contains(cssClass)) {
			// do nothing and report as error to console, but no GUI error for user
			getLogger().error("Tried to add CSS class::" + cssClass + " to #b_main but CSS class was already added");
		} else {
			mainCssClasses.add(cssClass);
			// add new CSS classes for main container
			String mainCss = calculateMainCssClasses(mainCssClasses);
			layoutMainVC.contextPut("mainCssClasses", mainCss);
		}
	}

	/**
	 * Remove a CSS class from the #b_main wrapper div
	 * 
	 * @param cssClass
	 */
	@Override
	public void removeCssClassFromMain(String cssClass) {
		if (mainCssClasses.contains(cssClass)) {
			mainCssClasses.remove(cssClass);
			// add new CSS classes for main container
			String mainCss = calculateMainCssClasses(mainCssClasses);
			layoutMainVC.contextPut("mainCssClasses", mainCss);
		} else {
			// do nothing and report as error to console, but no GUI error for user
			getLogger().error("Tried to remove CSS class::" + cssClass + " from #b_main but CSS class was not there");
		}
	}

	@Override
	protected void doDispose() {
		columns = null;
		mainCssClasses = null;
		layoutMainVC = null;
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source == layoutMainVC) {
			String command = event.getCommand();
			String width = ureq.getModuleURI();
			int parsedWidth;
			try {
				parsedWidth = Integer.parseInt(width);
				if (parsedWidth < 1) {
					// do not allow width smaller than 1em - resizer will be lost
					// otherwhise
					parsedWidth = 1;
				}
			} catch (NumberFormatException e) {
				logWarn("Could not parse column width::" + width + " for command::" + command, e);
				parsedWidth = 14; // default value
			}
			if (command.equals("saveCol1Width")) {
				localLayoutConfig.setCol1WidthEM(parsedWidth);
				saveGuiPrefs(ureq, localLayoutConfig);
				layoutMainVC.contextPut("col1CustomCSSStyles", "width: " + localLayoutConfig.getCol1WidthEM() + "em;");
				layoutMainVC.contextPut("col3CustomCSSStyles1", "margin-left: " + localLayoutConfig.getCol1WidthEM() + "em;");
				// don't refresh view in ajax mode!
				layoutMainVC.setDirty(false);

			} else if (command.equals("saveCol2Width")) {
				localLayoutConfig.setCol2WidthEM(parsedWidth);
				saveGuiPrefs(ureq, localLayoutConfig);
				layoutMainVC.contextPut("col2CustomCSSStyles", "width: " + localLayoutConfig.getCol2WidthEM() + "em;");
				layoutMainVC.contextPut("col3CustomCSSStyles2", "margin-right: " + localLayoutConfig.getCol2WidthEM() + "em;");
				// don't refresh view in ajax mode!
				layoutMainVC.setDirty(false);
			}
		}
	}

	private void saveGuiPrefs(UserRequest ureq, LayoutMain3ColsConfig layoutConfig) {
		// save config if not local setting
		if (layoutConfigKey != null && ureq.getUserSession().isAuthenticated() && !ureq.getUserSession().getRoles().isGuestOnly()) {
			ureq.getUserSession().getGuiPreferences().putAndSave(this.getClass(), layoutConfigKey, layoutConfig);
		}
	}

	/**
	 * Internal helper to load the layout config either from the GUI preferences or to generate a volatile one
	 * 
	 * @param ureq
	 * @return the layout column config
	 */
	private LayoutMain3ColsConfig getGuiPrefs(UserRequest ureq, LayoutMain3ColsConfig defaultConfiguration) {
		if (localLayoutConfig != null) { return localLayoutConfig; }
		LayoutMain3ColsConfig layoutConfig = null;
		if (layoutConfigKey != null && ureq.getUserSession().isAuthenticated() && !ureq.getUserSession().getRoles().isGuestOnly()) {
			// try to get persisted layout config
			layoutConfig = (LayoutMain3ColsConfig) ureq.getUserSession().getGuiPreferences().get(this.getClass(), layoutConfigKey);
		}
		if (layoutConfig == null) {
			// user has no config so far, use default configuration if available or create a new one
			layoutConfig = (defaultConfiguration == null ? new LayoutMain3ColsConfig() : defaultConfiguration);
		}
		return layoutConfig;

	}

	/**
	 * @see org.olat.core.gui.control.generic.layout.MainLayout3ColumnsController#hideCol1(boolean)
	 */
	@Override
	public void hideCol1(boolean hide) {
		hideCol(hide, 1);
	}

	/**
	 * @see org.olat.core.gui.control.generic.layout.MainLayout3ColumnsController#hideCol2(boolean)
	 */
	@Override
	public void hideCol2(boolean hide) {
		hideCol(hide, 2);
	}

	/**
	 * @see org.olat.core.gui.control.generic.layout.MainLayout3ColumnsController#hideCol3(boolean)
	 */
	@Override
	public void hideCol3(boolean hide) {
		hideCol(hide, 3);
	}

	/**
	 * Internal method to hide a column without removing the component
	 * 
	 * @param hide
	 * @param column
	 */
	private void hideCol(boolean hide, int column) {
		if (hide) {
			if (columns[column - 1] == null) {
				return;
			} else {
				mainCssClasses.add("b_hidecol" + column);
			}
		} else {
			if (columns[column - 1] == null) {
				return;
			} else {
				mainCssClasses.remove("b_hidecol" + column);
			}
		}
		// add new CSS classes for main container
		String mainCss = calculateMainCssClasses(mainCssClasses);
		layoutMainVC.contextPut("mainCssClasses", mainCss);
	}

	/**
	 * @see org.olat.core.gui.control.generic.layout.MainLayout3ColumnsController#setCol1(org.olat.core.gui.components.Component)
	 */
	@Override
	public void setCol1(Component col1Component) {
		setCol(col1Component, 1);
		panel1.setContent(col1Component);
		// init col width
		layoutMainVC.contextPut("col1CustomCSSStyles", "width: " + localLayoutConfig.getCol1WidthEM() + "em;");
		layoutMainVC.contextPut("col3CustomCSSStyles1", "margin-left: " + localLayoutConfig.getCol1WidthEM() + "em;");
	}

	/**
	 * @see org.olat.core.gui.control.generic.layout.MainLayout3ColumnsController#setCol2(org.olat.core.gui.components.Component)
	 */
	@Override
	public void setCol2(Component col2Component) {
		setCol(col2Component, 2);
		panel2.setContent(col2Component);
		layoutMainVC.contextPut("col2CustomCSSStyles", "width: " + localLayoutConfig.getCol2WidthEM() + "em;");
		layoutMainVC.contextPut("col3CustomCSSStyles2", "margin-right: " + localLayoutConfig.getCol2WidthEM() + "em;");
	}

	/**
	 * @see org.olat.core.gui.control.generic.layout.MainLayout3ColumnsController#setCol3(org.olat.core.gui.components.Component)
	 */
	@Override
	public void setCol3(Component col3Component) {
		setCol(col3Component, 3);
		panel3.setContent(col3Component);

	}

	/**
	 * Internal method to set a new column
	 * 
	 * @param newComponent
	 * @param column
	 */
	private void setCol(Component newComponent, int column) {
		Component oldComp = columns[column - 1];
		// remove old component from velocity first
		if (oldComp == null) {
			// css class to indicate if a column is hidden or shown
			mainCssClasses.remove("b_hidecol" + column);
		} else {
			layoutMainVC.remove(oldComp);
		}

		// add new component to velocity
		if (newComponent == null) {
			// tell YAML layout via css class on main container to not display this
			// column: this will adjust margin of col3 in normal setups
			mainCssClasses.add("b_hidecol" + column);
			layoutMainVC.contextPut("existsCol" + column, Boolean.FALSE);
		} else {
			layoutMainVC.contextPut("existsCol" + column, Boolean.TRUE);
		}

		// add new CSS classes for main container
		String mainCss = calculateMainCssClasses(mainCssClasses);
		layoutMainVC.contextPut("mainCssClasses", mainCss);

		// remember new component
		columns[column - 1] = newComponent;
	}

	/**
	 * Helper to generate the CSS classes that are set on the #b_main container to correctly render the column width and margins according to the YAML spec
	 * 
	 * @param classes
	 * @return
	 */
	private String calculateMainCssClasses(Set<String> classes) {
		String mainCss = "";
		for (Iterator<String> iter = classes.iterator(); iter.hasNext();) {
			String cssClass = iter.next();
			mainCss += cssClass;
			if (iter.hasNext()) {
				mainCss += " ";
			}
		}
		return mainCss;
	}

}
