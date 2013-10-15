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

package org.olat.core.gui.components.velocity;

import java.util.Iterator;
import java.util.Map;

import org.apache.velocity.context.Context;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.RenderingState;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.render.velocity.VelocityHelper;
import org.olat.core.gui.render.velocity.VelocityRenderDecorator;
import org.olat.core.gui.translator.Translator;

/**
 * Renderer for the VelocityContainer <br>
 * 
 * @author Felix Jost
 */
public class VelocityContainerRenderer implements ComponentRenderer {

	private final String theme;

	/**
	 * @param theme if null, then pages will be searched under the path defined by the page, e.g. /_theme/index.html instead of /index.html (an underscore is added)
	 */
	public VelocityContainerRenderer(String theme) {
		super();
		this.theme = theme;
	}

	/**
	 * @see org.olat.core.gui.render.ui.ComponentRenderer#render(org.olat.core.gui.render.Renderer, org.olat.core.gui.render.StringOutput,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.render.URLBuilder, org.olat.core.gui.translator.Translator, org.olat.core.gui.render.RenderResult,
	 *      java.lang.String[])
	 */
	@Override
	public void render(Renderer renderer, StringOutput target, Component source, URLBuilder ubu, Translator translator, RenderResult renderResult, String[] args) {
		VelocityContainer vc = (VelocityContainer) source;
		String pagePath = vc.getPage();
		Context ctx = vc.getContext();

		// the component id of the urlbuilder will be overwritten by the recursive render call for
		// subcomponents (see Renderer)
		Renderer fr = Renderer.getInstance(vc, translator, ubu, renderResult, renderer.getGlobalSettings());
		VelocityRenderDecorator vrdec = new VelocityRenderDecorator(fr, vc);
		ctx.put("r", vrdec);
		VelocityHelper vh = VelocityHelper.getInstance();
		String mm = vh.mergeContent(pagePath, ctx, theme);

		// experimental!!!
		// surround with red border if recording mark indicates so.
		if (vc.markingCommandString != null) {
			target.append("<table style=\"border:3px solid red; background-color:#E0E0E0; padding:4px; margin:0px;\"><tr><td>").append(mm).append("</td></tr></table>");
		} else {
			target.append(mm);
		}
	}

	/**
	 * @see org.olat.core.gui.render.ui.ComponentRenderer#renderHeaderIncludes(org.olat.core.gui.render.Renderer, org.olat.core.gui.render.StringOutput,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.render.URLBuilder, org.olat.core.gui.translator.Translator)
	 */
	@Override
	public void renderHeaderIncludes(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu, Translator translator, RenderingState rstate) {
		VelocityContainer vc = (VelocityContainer) source;
		// the velocity container itself needs no headerincludes, but ask the
		// children also
		Map comps = vc.getComponents();
		for (Iterator iter = comps.values().iterator(); iter.hasNext();) {
			Component child = (Component) iter.next();
			renderer.renderHeaderIncludes(sb, child, rstate);
		}
	}

	/**
	 * @see org.olat.core.gui.render.ui.ComponentRenderer#renderBodyOnLoadJSFunctionCall(org.olat.core.gui.render.Renderer, org.olat.core.gui.render.StringOutput,
	 *      org.olat.core.gui.components.Component)
	 */
	@Override
	public void renderBodyOnLoadJSFunctionCall(Renderer renderer, StringOutput sb, Component source, RenderingState rstate) {
		VelocityContainer vc = (VelocityContainer) source;
		// the velocity container itself needs no headerincludes, but ask the
		// children also
		Map comps = vc.getComponents();
		for (Iterator iter = comps.values().iterator(); iter.hasNext();) {
			Component child = (Component) iter.next();
			renderer.renderBodyOnLoadJSFunctionCall(sb, child, rstate);
		}
	}
}