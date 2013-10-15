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
package org.olat.core.gui.control.generic.textmarker;

import org.olat.core.dispatcher.mapper.GlobalMapperRegistry;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlheader.jscss.JSAndCSSComponent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;

/**
 * Description:<br>
 * Controller to wrap around normal content and let glossary-code run.
 * <P>
 * Initial Date: 29.01.2009 <br>
 * 
 * @author rhaag
 */
public class GlossaryMarkupItemController extends BasicController {

	// it doesn't seem this would ever change, so have it static in core also
	protected static final String INTERNAL_FOLDER_NAME = "_glossary_";

	private String domId;
	private VFSContainer glossaryFolder;
	private VelocityContainer tmContainer = createVelocityContainer("tmContainer");
	private JSAndCSSComponent glossHelpJs;
	private boolean textMarkingEnabled;
	private JSAndCSSComponent glossHLJS;
	private String glossaryId;

	private static Mapper glossaryDefinitionMapper;
	private static String glossaryDefinitionMapperPath;
	private static GlossaryTermMapper glossaryTermMapper;
	private static String glossaryTermMapperPath;
	static {
		glossaryDefinitionMapper = new GlossaryDefinitionMapper();
		glossaryDefinitionMapperPath = GlobalMapperRegistry.getInstance().register(GlossaryDefinitionMapper.class, glossaryDefinitionMapper);

		glossaryTermMapper = new GlossaryTermMapper();
		glossaryTermMapperPath = GlobalMapperRegistry.getInstance().register(GlossaryTermMapper.class, glossaryTermMapper);
	}

	public GlossaryMarkupItemController(UserRequest ureq, WindowControl control, Component tmComponent, VFSContainer glossaryFolder, String glossaryId) {
		super(ureq, control);
		this.domId = "o_tm" + Integer.toString(this.getClass().hashCode());
		this.glossaryFolder = glossaryFolder;
		this.glossaryId = glossaryId;

		init(ureq, tmComponent);
	}

	private void init(UserRequest ureq, Component tmComponent) {
		// add dom id for wrapper div
		tmContainer.contextPut("domId", domId);

		glossHelpJs = new JSAndCSSComponent("glossHelpJs", this.getClass(), new String[] { "glossaryhelper.js" }, "textmarker.css", true);
		tmContainer.put("glossHelpJs", glossHelpJs);
		glossHLJS = new JSAndCSSComponent("glossHLJS", this.getClass(), new String[] { "glossaryhighlighter.js" }, null, true);
		tmContainer.put("glossHLJS", glossHLJS);

		String glossFolderString = ((LocalFolderImpl) glossaryFolder).getBasefile().toString();
		tmContainer.contextPut("glossaryFolder", glossFolderString.replace("/", "."));
		tmContainer.contextPut("glossaryDefinitionMapperPath", glossaryDefinitionMapperPath);
		tmContainer.contextPut("glossaryTermMapperPath", glossaryTermMapperPath);

		// TODO:RH:improvement: use a hash and cache it somewhere in case folderId should not be exposed to user
		// String glossaryId = Encoder.encrypt(((LocalFolderImpl)glossaryFolder).getBasefile().toString());

		tmContainer.contextPut("glossaryId", glossaryId);

		// finally add the wrapped content to the velocity container
		tmContainer.put("tmComponent", tmComponent);
		putInitialPanel(tmContainer);
	}

	/**
	 * @return true: text marking is enabled, false otherwise
	 */
	public boolean isTextMarkingEnabled() {
		return textMarkingEnabled;
	}

	/**
	 * Enable / disable the text marking temporary. E.g. in a test you probably don't want to use the glossary
	 * 
	 * @param textMarkingEnabled true: set text marking enabled, false to disable
	 */
	public void setTextMarkingEnabled(boolean textMarkingEnabled) {
		this.textMarkingEnabled = textMarkingEnabled;
		tmContainer.contextPut("glossaryEnabled", textMarkingEnabled);
		// iframes get cleared by IframeDisplayController, which removes glossaryhighlighter.js, so no more highlighting at all
		getWindowControl().getWindowBackOffice().getWindow().setDirty(true);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		// TODO Auto-generated method stub

	}

}
