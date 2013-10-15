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
package ch.unizh.portal.zsuz;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.portal.AbstractPortlet;
import org.olat.core.gui.control.generic.portal.Portlet;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.id.Identity;
import org.olat.core.id.Persistable;
import org.olat.core.id.Preferences;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;
import org.olat.core.util.Util;

/**
 * Description:<br>
 * TODO: patrickb Class Description for ZentrallStellePortlet
 * <P>
 * Initial Date: 06.06.2008 <br>
 * 
 * @author patrickb
 */
public class ZentralstellePortlet extends AbstractPortlet {

	private ZentralstellePortletRunController runCtrl;
	final static ZentralstelleIrchel drucki = new ZentralstelleIrchel();
	final static ZentralstelleZentrum druckz = new ZentralstelleZentrum();

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#createInstance(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest, java.util.Map)
	 */
	@Override
	@SuppressWarnings({ "unused", "unchecked" })
	public Portlet createInstance(final WindowControl control, final UserRequest ureq, final Map portletConfig) {
		final Portlet p = new ZentralstellePortlet();
		p.setName(this.getName());
		p.setConfiguration(portletConfig);
		p.setTranslator(new PackageTranslator(Util.getPackageName(ZentralstellePortlet.class), ureq.getLocale()));
		return p;
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#disposeRunComponent()
	 */
	@Override
	public void disposeRunComponent() {
		if (this.runCtrl != null) {
			runCtrl.dispose();
			runCtrl = null;
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#getCssClass()
	 */
	@Override
	public String getCssClass() {
		// the zentralstelle icon
		return "o_portlet_zsuz";
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#getDescription()
	 */
	@Override
	public String getDescription() {
		return getTranslator().translate("zsuz.infotext0");
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#getInitialRunComponent(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest)
	 */
	@Override
	public Component getInitialRunComponent(final WindowControl wControl, final UserRequest ureq) {
		if (this.runCtrl != null) {
			runCtrl.dispose();
		}
		runCtrl = new ZentralstellePortletRunController(ureq, wControl);
		return runCtrl.getInitialComponent();
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#getTitle()
	 */
	@Override
	public String getTitle() {
		return getTranslator().translate("zsuz.title");
	}

	/**
	 * @see org.olat.core.gui.control.Disposable#dispose()
	 */
	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	static class ZentralstelleIrchel implements Identity {

		@Override
		public Long getKey() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		@SuppressWarnings("unused")
		public boolean equalsByPersistableKey(final Persistable arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		public Date getLastModified() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Date getCreationDate() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		@SuppressWarnings("unused")
		public void setStatus(final Integer arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		@SuppressWarnings("unused")
		public void setLastLogin(final Date arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public User getUser() {
			return new User() {
				Map<String, String> data = new HashMap<String, String>();
				{
					data.put(UserConstants.FIRSTNAME, "Zsuz Irchel");
					data.put(UserConstants.LASTNAME, "Druckerei Irchel");
					data.put(UserConstants.EMAIL, "drucki@zsuz.uzh.ch");
					data.put(UserConstants.INSTITUTIONALNAME, "Zentralstelle UZH");
					data.put(UserConstants.INSTITUTIONALEMAIL, "drucki@zsuz.uzh.ch");
				}

				@Override
				public Long getKey() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				@SuppressWarnings("unused")
				public boolean equalsByPersistableKey(final Persistable persistable) {
					// TODO Auto-generated method stub
					return false;
				}

				public Date getLastModified() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Date getCreationDate() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				@SuppressWarnings("unused")
				public void setProperty(final String propertyName, final String propertyValue) {
					// TODO Auto-generated method stub

				}

				@Override
				@SuppressWarnings("unused")
				public void setPreferences(final Preferences prefs) {
					// TODO Auto-generated method stub

				}

				@Override
				@SuppressWarnings("unused")
				public String getProperty(final String propertyName, final Locale locale) {
					return data.get(propertyName);
				}

				@Override
				public void setIdentityEnvironmentAttributes(final Map<String, String> identEnvAttribs) {
					throw new AssertException("SETTER not yet implemented, not used in case of ZentralstellePortlet");
				}

				@Override
				public String getPropertyOrIdentityEnvAttribute(final String propertyName, final Locale locale) {
					throw new AssertException("GETTER not yet implemented, not used in case of ZentralstellePortlet");
				}

				@Override
				public Preferences getPreferences() {
					// TODO Auto-generated method stub
					return null;
				}

			};
		}

		@Override
		public Integer getStatus() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return "zentralstelle_druckerei_irchel";
		}

		@Override
		public Date getLastLogin() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setName(final String loginName) {
			// TODO Auto-generated method stub

		}

	}

	static class ZentralstelleZentrum implements Identity {

		@Override
		public Long getKey() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		@SuppressWarnings("unused")
		public boolean equalsByPersistableKey(final Persistable arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		public Date getLastModified() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Date getCreationDate() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		@SuppressWarnings("unused")
		public void setStatus(final Integer arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		@SuppressWarnings("unused")
		public void setLastLogin(final Date arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public User getUser() {
			return new User() {
				Map<String, String> data = new HashMap<String, String>();
				{
					data.put(UserConstants.FIRSTNAME, "Zsuz Zentrum");
					data.put(UserConstants.LASTNAME, "Druckerei Zentrum");
					data.put(UserConstants.EMAIL, "druckz@zsuz.uzh.ch");
					data.put(UserConstants.INSTITUTIONALNAME, "Zentralstelle UZH");
					data.put(UserConstants.INSTITUTIONALEMAIL, "druckz@zsuz.uzh.ch");
				}

				@Override
				public Long getKey() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				@SuppressWarnings("unused")
				public boolean equalsByPersistableKey(final Persistable persistable) {
					// TODO Auto-generated method stub
					return false;
				}

				public Date getLastModified() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Date getCreationDate() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				@SuppressWarnings("unused")
				public void setProperty(final String propertyName, final String propertyValue) {
					// TODO Auto-generated method stub

				}

				@Override
				@SuppressWarnings("unused")
				public void setPreferences(final Preferences prefs) {
					// TODO Auto-generated method stub

				}

				@Override
				@SuppressWarnings("unused")
				public String getProperty(final String propertyName, final Locale locale) {
					return data.get(propertyName);
				}

				@Override
				public void setIdentityEnvironmentAttributes(final Map<String, String> identEnvAttribs) {
					throw new AssertException("SETTER not yet implemented, not used in case of ZentralstellePortlet");
				}

				@Override
				public String getPropertyOrIdentityEnvAttribute(final String propertyName, final Locale locale) {
					throw new AssertException("GETTER not yet implemented, not used in case of ZentralstellePortlet");
				}

				@Override
				public Preferences getPreferences() {
					// TODO Auto-generated method stub
					return null;
				}

			};
		}

		@Override
		public Integer getStatus() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return "zentralstelle_druckerei_zentrum";
		}

		@Override
		public Date getLastLogin() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setName(final String loginName) {
			// TODO Auto-generated method stub

		}

	}

}
