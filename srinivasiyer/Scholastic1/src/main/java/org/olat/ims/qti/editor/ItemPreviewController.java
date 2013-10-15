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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.ims.qti.editor;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPaneChangedEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Formatter;
import org.olat.core.util.Util;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.ims.qti.container.qtielements.RenderInstructions;
import org.olat.ims.qti.editor.beecom.objects.Item;

/**
 * Initial Date: Oct 21, 2004 <br>
 * 
 * @author mike
 */
public class ItemPreviewController extends DefaultController implements ControllerEventListener {
	/*
	 * Logging, Velocity
	 */
	private static final String PACKAGE = Util.getPackageName(ItemPreviewController.class);
	private static final String VC_ROOT = Util.getPackageVelocityRoot(PACKAGE);

	private final Panel mainPanel;
	private VelocityContainer main;
	private final Item item;
	RenderInstructions renderInstructions;
	private final QTIEditorPackage qtiPackage;

	/**
	 * @param item
	 * @param qtiPackage
	 * @param translator
	 */
	public ItemPreviewController(final WindowControl wControl, final Item item, final QTIEditorPackage qtiPackage, final Translator translator) {
		super(wControl);
		this.item = item;
		this.qtiPackage = qtiPackage;
		renderInstructions = new RenderInstructions();
		renderInstructions.put(RenderInstructions.KEY_STATICS_PATH, qtiPackage.getMediaBaseURL() + "/");
		renderInstructions.put(RenderInstructions.KEY_LOCALE, translator.getLocale());
		renderInstructions.put(RenderInstructions.KEY_RENDER_TITLE, Boolean.TRUE);
		main = new VelocityContainer("vcItemPreview", VC_ROOT + "/tab_itemPreview.html", translator, this);
		main.contextPut("itemPreview", getQuestionPreview(item));
		mainPanel = new Panel("itemPreviewPanel");
		mainPanel.setContent(main);
		setInitialComponent(mainPanel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (event instanceof TabbedPaneChangedEvent) {
			final TabbedPaneChangedEvent tpcEvent = (TabbedPaneChangedEvent) event;
			if (tpcEvent.getNewComponent() == mainPanel) {
				main.contextPut("itemPreview", getQuestionPreview(item));
			}
		}
	}

	private String getQuestionPreview(final Item theItem) {
		final Element el = DocumentFactory.getInstance().createElement("dummy");
		theItem.addToElement(el);
		final StringBuilder sb = new StringBuilder();
		final org.olat.ims.qti.container.qtielements.Item foo = new org.olat.ims.qti.container.qtielements.Item((Element) el.elements().get(0));
		foo.render(sb, renderInstructions);
		final String previewWithFormattedMathElements = Formatter.formatLatexFormulas(sb.toString());
		final Filter filter = FilterFactory.getBaseURLToMediaRelativeURLFilter(qtiPackage.getMediaBaseURL());
		return filter.filter(previewWithFormattedMathElements);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		main = null;
	}

}