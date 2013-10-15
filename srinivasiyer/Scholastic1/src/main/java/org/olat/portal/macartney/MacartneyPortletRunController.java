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

package org.olat.portal.macartney;

import java.text.DecimalFormat;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.helpers.Settings;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.resource.OresHelper;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;

/**
 * Description:<br>
 * Run view controller of macartney portlet
 * <P>
 * Initial Date: 11.07.2005 <br>
 * 
 * @author gnaegi
 */
public class MacartneyPortletRunController extends DefaultController {

	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(MacartneyPortlet.class);
	private Translator trans;
	private VelocityContainer macartneyVC;

	private static final String PROPERTY_NAME = "comicStart";
	private static final String PROPERTY_CATEGORY = "macartneyPortal";

	private static final int maxEpisodes = 468;
	private static final long updateInterval = 86400000; // one day in milliseconds

	private static long comicStartDate = 0;

	/**
	 * Constructor
	 * 
	 * @param ureq
	 * @param imageBaseUri
	 */
	// o_clusterOK by:se synchronized on MacartneyPortlet class as olatresourceable
	protected MacartneyPortletRunController(final UserRequest ureq, final WindowControl wControl, final Map configuration) {
		super(wControl);
		this.trans = new PackageTranslator(Util.getPackageName(MacartneyPortlet.class), ureq.getLocale());
		this.macartneyVC = new VelocityContainer("macartneyVC", VELOCITY_ROOT + "/macartneyPortlet.html", trans, this);

		if (comicStartDate == 0) {
			CoordinatorManager.getInstance().getCoordinator().getSyncer()
					.doInSync(OresHelper.createOLATResourceableType(MacartneyPortletRunController.class.getSimpleName()), new SyncerExecutor() {

						@Override
						@SuppressWarnings("synthetic-access")
						public void execute() {
							if (comicStartDate != 0) {
								// then we shouldn't have gotten here in the first place, but we were
								// racing with another userrequest in the same VM!
								// let's quit quickly ;)
								return;
							}
							// load comic start date only once
							final PropertyManager pm = PropertyManager.getInstance();
							Property p = pm.findProperty(null, null, null, PROPERTY_CATEGORY, PROPERTY_NAME);
							if (p == null) {
								// wow first time ever, save current date
								p = pm.createPropertyInstance(null, null, null, PROPERTY_CATEGORY, PROPERTY_NAME, null, null, null, null);
								pm.saveProperty(p);
							}
							comicStartDate = p.getCreationDate().getTime();
						}

					});
		}

		// time between comic start and now
		final long timeDelta = (System.currentTimeMillis() - comicStartDate);
		// number of increments since comic start
		final long incrementNumber = (timeDelta / updateInterval);
		// module with max episodes to start over again when finished
		// + 1 since comic starts at position 1 and not 0
		final long imageNumber = (incrementNumber % maxEpisodes) + 1;

		// calculate current episode url and push to velocity
		final DecimalFormat df = new DecimalFormat("0000");
		final String currentEpisodeImage = "loge_" + df.format(imageNumber) + ".jpg";
		final String configuredImageBaseUri = (String) configuration.get("imageBaseUri");
		if (configuredImageBaseUri.startsWith("http://") || configuredImageBaseUri.startsWith("https://")) {
			// feature: if the configured imageBaseUri contains http: or https: already, don't do
			// magic "trying-to-detect-the-correct-protocol-via-olat.properties" but use it right away
			this.macartneyVC.contextPut("imageBaseUri", configuredImageBaseUri);
		} else {
			// otherwise do the old magic
			this.macartneyVC.contextPut("imageBaseUri", Settings.getURIScheme() + configuredImageBaseUri);
		}
		this.macartneyVC.contextPut("currentEpisodeImage", currentEpisodeImage);

		setInitialComponent(this.macartneyVC);
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no events to catch
	}

	@Override
	protected void doDispose() {
		// nothing to dispose
	}

}
