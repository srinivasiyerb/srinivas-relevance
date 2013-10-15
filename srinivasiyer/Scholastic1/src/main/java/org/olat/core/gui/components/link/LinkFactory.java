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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.gui.components.link;

import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;

/**
 * Description:<br>
 * offer convenience methods to create and add buttons/links to Velocity Containers quick and easy</li> Typical usage (see also GuiDemoLinksController.java):
 * <ol>
 * <li>instantiate your VelocityContrainter<br>
 * <code>mainVC = createVelocityContainer("guidemo-links");</code></li>
 * <li>create your button<br>
 * <code>button = LinkFactory.createButton("button", mainVC, this);</code>
 * <li>save it as instance variable, that the controller can listeningController catch its events</li>
 * <li>add to your velocity page, that shows up the button <code>$r.render("button")</code></li>
 * <li>and finally listen to the events the button fired in the <code>listeningController.event(UserRequest ureq, Component source, Event event)</code><br>
 * <code>public void event(UserRequest ureq, Component source, Event event) {
 * 	if (source == button){
 * 		// do something
 * 	}
 * }
 * </code></li>
 * </ol>
 * Initial Date: August 10, 2006 <br>
 * 
 * @author Alexander Schneider
 */
public class LinkFactory {

	/**
	 * add a back link to the <code>vc</code> Velocity Container and make the <code>listeningController</code> listen to this back link.
	 * <p>
	 * Follow these instructions to show the back link and catch its events:
	 * <ol>
	 * <li><code>$r.render("backLink")</code> in your velocity page, that the link shows up.</li>
	 * <li>save the returned link as a instance variable <code>myBackLink</code></li>
	 * <li>in the <code>listeningController.event(UserRequest ureq, Component source, Event event)</code> you catch the back link by<br>
	 * <code>if(source == myBackLink){..your stuff here..}</code></li>
	 * </ol>
	 * 
	 * @param vc the VelocityContainer within you put this link
	 * @param listeningController
	 * @return the link component
	 */
	public static Link createLinkBack(VelocityContainer vc, Controller listeningController) {
		Link backLink = new Link("backLink", "back", "back", Link.LINK_BACK, vc, listeningController);
		backLink.setAccessKey("b");
		return backLink;
	}

	/**
	 * add a close icon to the <code>vc</code> Velocity Container and make the <code>listeningController</code> listen to the user's click on the close icon.
	 * <p>
	 * Follow these instructions to show the close icon and catch its events:
	 * <ol>
	 * <li><code>$r.render("closeIcon")</code> in your velocity page, that the link shows up.</li>
	 * <li>save the returned link as a instance variable <code>myCloseIcon</code></li>
	 * <li>in the <code>listeningController.event(UserRequest ureq, Component source, Event event)</code> you catch the close icon by<br>
	 * <code>if(source == close icon){..your stuff here..}</code></li>
	 * </ol>
	 * 
	 * @param title - displayed on hovering over the icon - can be null, then no title is displayed
	 * @param vc
	 * @param listeningController
	 * @return Link which display just the close icon
	 */
	public static Link createIconClose(String title, VelocityContainer vc, Controller listeningController) {
		Link closeIcon = new Link("closeIcon", "close", "", Link.LINK_CUSTOM_CSS + Link.NONTRANSLATED, vc, listeningController);
		closeIcon.setCustomEnabledLinkCSS("b_link_close");
		closeIcon.setCustomDisabledLinkCSS("b_link_close");
		closeIcon.setTooltip(title, false);
		if (title != null) {
			closeIcon.setTitle(title);
		}
		return closeIcon;
	}

	/**
	 * add a link to the <code>vc</code> Velocity Container and make the <code>listeningController</code> listen to this link.
	 * <p>
	 * Follow these instructions to show the link and catch its events:
	 * <ol>
	 * <li><code>$r.render("myLink")</code> in your velocity page, that the link shows up.</li>
	 * <li>save the returned link as a instance variable <code>myLink</code></li>
	 * <li>in the <code>listeningController.event(UserRequest ureq, Component source, Event event)</code> you catch the back link by<br>
	 * <code>if(source == myLink){..your stuff here..}</code></li>
	 * </ol>
	 * 
	 * @param one string for name of component, command and i18n key
	 * @param vc the VelocityContainer within you put this link
	 * @param listeningController
	 * @return the link component
	 */
	public static Link createLink(String name, VelocityContainer vc, Controller listeningController) {
		return new Link(name, name, name, Link.LINK, vc, listeningController);
	}

	/**
	 * add a customized link to the <code>vc</code> Velocity Container and make the <code>listeningController</code> listen to this link. A customized link means that you
	 * can configure everything by yourself using the constants of the link component e.g. <code>Link.NONTRANSLATED</code>
	 * <p>
	 * Follow these instructions to show the customized link and catch its events:
	 * <ol>
	 * <li><code>$r.render("myCustomizedLink")</code> in your velocity page, that the link shows up.</li>
	 * <li>save the returned link as a instance variable <code>myCustomizedLink</code></li>
	 * <li>in the <code>listeningController.event(UserRequest ureq, Component source, Event event)</code> you catch the back link by<br>
	 * <code>if(source == myLink){..your stuff here..}</code></li>
	 * </ol>
	 * 
	 * @param name of the link component
	 * @param cmd command, null or empty string are not allowed
	 * @param key if it's already translated, use at the next parameter Link.NONTRANSLATED, null is allowed
	 * @param presentation
	 * @param vc the VelocityContainer within you put this link
	 * @param listeningController
	 * @return the link component
	 */

	public static Link createCustomLink(String name, String cmd, String key, int presentation, VelocityContainer vc, Controller listeningController) {
		return new Link(name, cmd, key, presentation, vc, listeningController);
	}

	/**
	 * add a button to the <code>vc</code> Velocity Container and make the <code>listeningController</code> listen to this button.
	 * <p>
	 * Follow these instructions to show the button and catch its events:
	 * <ol>
	 * <li><code>$r.render("myButton")</code> in your velocity page, that the button shows up.</li>
	 * <li>save the returned link as a instance variable <code>myButton</code></li>
	 * <li>in the <code>listeningController.event(UserRequest ureq, Component source, Event event)</code> you catch the button by<br>
	 * <code>if(source == myButton){..your stuff here..}</code></li>
	 * </ol>
	 * 
	 * @param one string for name of component, command and i18n key
	 * @param vc the VelocityContainer within you put this link
	 * @param listeningController
	 * @return the link component
	 */
	public static Link createButton(String name, VelocityContainer vc, Controller listeningController) {
		return new Link(name, name, name, Link.BUTTON, vc, listeningController);
	}

	/**
	 * add a small button to the <code>vc</code> Velocity Container and make the <code>listeningController</code> listen to this small button.
	 * <p>
	 * Follow these instructions to show the small button and catch its events:
	 * <ol>
	 * <li><code>$r.render("mySmallButton")</code> in your velocity page, that the small button shows up.</li>
	 * <li>save the returned link as a instance variable <code>mySmallButton</code></li>
	 * <li>in the <code>listeningController.event(UserRequest ureq, Component source, Event event)</code> you catch the small button by<br>
	 * <code>if(source == mySmallButton){..your stuff here..}</code></li>
	 * </ol>
	 * 
	 * @param one string for name of component, command and i18n key
	 * @param vc the VelocityContainer within you put this link
	 * @param listeningController
	 * @return the link component
	 */
	public static Link createButtonSmall(String name, VelocityContainer vc, Controller listeningController) {
		return new Link(name, name, name, Link.BUTTON_SMALL, vc, listeningController);
	}

	/**
	 * add a xsmall button to the <code>vc</code> Velocity Container and make the <code>listeningController</code> listen to this xsmall button.
	 * <p>
	 * Follow these instructions to show the xsmall button and catch its events:
	 * <ol>
	 * <li><code>$r.render("myXSmallButton")</code> in your velocity page, that the xsmall button shows up.</li>
	 * <li>save the returned link as a instance variable <code>myXSmallButton</code></li>
	 * <li>in the <code>listeningController.event(UserRequest ureq, Component source, Event event)</code> you catch the xsmall button by<br>
	 * <code>if(source == myXSmallButton){..your stuff here..}</code></li>
	 * </ol>
	 * 
	 * @param one string for name of component, command and i18n key
	 * @param vc the VelocityContainer within you put this link
	 * @param listeningController
	 * @return the link component
	 */
	public static Link createButtonXSmall(String name, VelocityContainer vc, Controller listeningController) {
		return new Link(name, name, name, Link.BUTTON_XSMALL, vc, listeningController);
	}

	/**
	 * @param link the Link to be deajaxified
	 * @return the given Link changed so that it renders its url always in standard mode even if ajax-mode is on
	 */
	public static Link deAjaxify(Link link) {
		link.setAjaxEnabled(false);
		return link;
	}

	public static Link markDownloadLink(Link link) {
		link.setStartsDownload();
		return link;
	}

}
