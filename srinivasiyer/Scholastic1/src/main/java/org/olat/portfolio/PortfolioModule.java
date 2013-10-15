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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.portfolio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.configuration.AbstractOLATModule;
import org.olat.core.configuration.ConfigOnOff;
import org.olat.core.configuration.PersistedProperties;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.portfolio.model.artefacts.AbstractArtefact;

/**
 * Description:<br>
 * The PortfolioModule contains the configurations for the e-Portfolio feature
 * <P>
 * Initial Date: 11.06.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class PortfolioModule extends AbstractOLATModule implements ConfigOnOff {

	private final List<EPArtefactHandler<?>> artefactHandlers = new ArrayList<EPArtefactHandler<?>>();

	private boolean enabled;
	private VFSContainer portfolioRoot;
	private List<String> availableMapStyles = new ArrayList<String>();
	private boolean offerPublicMapList;

	@Override
	public void init() {
		// portfolio enabled/disabled
		final String enabledObj = getStringPropertyValue("portfolio.enabled", true);
		if (StringHelper.containsNonWhitespace(enabledObj)) {
			enabled = "true".equals(enabledObj);
		}

		for (final EPArtefactHandler<?> handler : artefactHandlers) {
			final String enabledHandler = getStringPropertyValue("handler." + handler.getClass().getName(), true);
			if (StringHelper.containsNonWhitespace(enabledHandler)) {
				((EPAbstractHandler<?>) handler).setEnabled("true".equals(enabledHandler));
			}
		}

		final String styles = getStringPropertyValue("portfolio.map.styles", true);
		if (StringHelper.containsNonWhitespace(styles)) {
			this.availableMapStyles = new ArrayList<String>();
			for (final String style : styles.split(",")) {
				availableMapStyles.add(style);
			}
		}

		final String offerPublicSetting = getStringPropertyValue("portfolio.offer.public.map.list", true);
		if (StringHelper.containsNonWhitespace(offerPublicSetting)) {
			setOfferPublicMapList("true".equals(offerPublicSetting));
		}

		logInfo("ePortfolio is enabled: " + Boolean.toString(enabled));
	}

	@Override
	protected void initDefaultProperties() {
		enabled = getBooleanConfigParameter("portfolio.enabled", true);

		for (final EPArtefactHandler<?> handler : artefactHandlers) {
			final boolean enabledHandler = getBooleanConfigParameter("handler." + handler.getClass().getName(), true);
			((EPAbstractHandler<?>) handler).setEnabled(enabledHandler);
		}

		this.availableMapStyles = new ArrayList<String>();
		final String styles = this.getStringConfigParameter("portfolio.map.styles", "default", false);
		if (StringHelper.containsNonWhitespace(styles)) {
			for (final String style : styles.split(",")) {
				availableMapStyles.add(style);
			}
		}

		setOfferPublicMapList(getBooleanConfigParameter("portfolio.offer.public.map.list", true));
	}

	@Override
	protected void initFromChangedProperties() {
		init();
	}

	@Override
	public void setPersistedProperties(final PersistedProperties persistedProperties) {
		this.moduleConfigProperties = persistedProperties;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		if (this.enabled != enabled) {
			setStringProperty("portfolio.enabled", Boolean.toString(enabled), true);
		}
	}

	/**
	 * Return all the configured artefact handlers, enabled or not
	 * 
	 * @return
	 */
	public List<EPArtefactHandler<?>> getAllAvailableArtefactHandlers() {
		final List<EPArtefactHandler<?>> handlers = new ArrayList<EPArtefactHandler<?>>(artefactHandlers.size());
		handlers.addAll(artefactHandlers);
		return handlers;
	}

	public void setEnableArtefactHandler(final EPArtefactHandler<?> handler, final boolean enabled) {
		setStringProperty("handler." + handler.getClass().getName(), Boolean.toString(enabled), true);
	}

	/**
	 * Return the enabled artefact handler
	 * 
	 * @return
	 */
	public List<EPArtefactHandler<?>> getArtefactHandlers() {
		final List<EPArtefactHandler<?>> handlers = new ArrayList<EPArtefactHandler<?>>(artefactHandlers.size());
		for (final EPArtefactHandler<?> handler : artefactHandlers) {
			if (handler.isEnabled()) {
				handlers.add(handler);
			}
		}
		return handlers;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param artefacthandlers
	 */
	public void setArtefactHandlers(final List<EPArtefactHandler<?>> artefacthandlers) {
		this.artefactHandlers.addAll(artefacthandlers);
	}

	public EPArtefactHandler<?> getArtefactHandler(final String type) {
		for (final EPArtefactHandler<?> handler : artefactHandlers) {
			if (type.equals(handler.getType())) { return handler; }
		}
		logWarn("Either tried to get a disabled handler or could not return a handler for artefact-type: " + type, null);
		return null;
	}

	public EPArtefactHandler<?> getArtefactHandler(final AbstractArtefact artefact) {
		return getArtefactHandler(artefact.getResourceableTypeName());
	}

	public void addArtefactHandler(final EPArtefactHandler<?> artefacthandler) {
		artefactHandlers.add(artefacthandler);

		final String settingName = "handler." + artefacthandler.getClass().getName();
		final String propEnabled = getStringPropertyValue(settingName, true);
		if (StringHelper.containsNonWhitespace(propEnabled)) {
			// system properties settings
			((EPAbstractHandler<?>) artefacthandler).setEnabled("true".equals(propEnabled));
		} else {
			// default settings
			final boolean defEnabled = getBooleanConfigParameter(settingName, true);
			((EPAbstractHandler<?>) artefacthandler).setEnabled(defEnabled);
		}
	}

	public boolean removeArtefactHandler(final EPArtefactHandler<?> artefacthandler) {
		return artefactHandlers.remove(artefacthandler);
	}

	public VFSContainer getPortfolioRoot() {
		if (portfolioRoot == null) {
			portfolioRoot = new OlatRootFolderImpl(File.separator + "portfolio", null);
		}
		return portfolioRoot;
	}

	/**
	 * @param availableMapStyles The availableMapStyles to set.
	 */
	public void setAvailableMapStylesStr(final String availableMapStylesStr) {
		this.availableMapStyles = new ArrayList<String>();
		if (StringHelper.containsNonWhitespace(availableMapStylesStr)) {
			final String[] styles = availableMapStylesStr.split(",");
			for (final String style : styles) {
				availableMapStyles.add(style);
			}
		}
	}

	/**
	 * @return Returns the availableMapStyles.
	 */
	public List<String> getAvailableMapStyles() {
		return availableMapStyles;
	}

	public void setAvailableMapStylesS(final List<String> availableMapStyles) {
		this.availableMapStyles = availableMapStyles;
	}

	/**
	 * @param offerPublicMapList The offerPublicMapList to set.
	 */
	public void setOfferPublicMapList(final boolean offerPublicMapList) {
		this.offerPublicMapList = offerPublicMapList;
	}

	/**
	 * Return setting for public map list. Systems with more than 500 public maps, should disable this feature as it gets too slow!
	 * 
	 * @return Returns the offerPublicMapList.
	 */
	public boolean isOfferPublicMapList() {
		return offerPublicMapList;
	}
}
