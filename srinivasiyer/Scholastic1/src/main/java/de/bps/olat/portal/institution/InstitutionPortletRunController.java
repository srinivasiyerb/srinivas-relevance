/**
 * BPS Bildungsportal Sachsen GmbH<br>
 * Bahnhofstraße 6<br>
 * 09111 Chemnitz<br>
 * Germany<br>
 * Copyright (c) 2005-2008 by BPS Bildungsportal Sachsen GmbH<br>
 * http://www.bps-system.de<br>
 * All rights reserved. Initial Date: 08.07.2005 <br>
 * 
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 **/
package de.bps.olat.portal.institution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.ControllerFactory;
import org.olat.catalog.CatalogEntry;
import org.olat.catalog.CatalogManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.Tracing;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.site.RepositorySite;

public class InstitutionPortletRunController extends BasicController {

	private VelocityContainer portletVC;
	private List<String> polyLinks;
	private Map<Link, PolymorphLink> mapLinks;
	private InstitutionPortletEntry ipe;

	protected InstitutionPortletRunController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		this.portletVC = createVelocityContainer("institutionPortlet");
		String userinst = "unknown";

		try {
			userinst = ureq.getIdentity().getUser().getProperty(UserConstants.INSTITUTIONALNAME, ureq.getLocale()).toLowerCase();
			this.ipe = InstitutionPortlet.getInstitutionPortletEntry(userinst);
		} catch (final Exception e) {
			ipe = null;
		}
		this.portletVC.contextPut("hasInstitution", new Boolean(ipe != null));
		if (ipe == null) {
			Tracing.createLoggerFor(InstitutionPortletRunController.class).warn("unknown institution (" + userinst + ") for user " + ureq.getIdentity().getName());
		} else {
			this.portletVC.contextPut("iname", ipe.getInstitutionName());
			this.portletVC.contextPut("iurl", ipe.getInstitutionUrl());
			this.portletVC.contextPut("ilogo", ipe.getInstitutionLogo());
			// --> just read first supervisor element:
			final InstitutionPortletSupervisorEntry ipse = ipe.getSupervisors().get(0);
			this.portletVC.contextPut("sperson", ipse.getSupervisorPerson());

			Boolean showphone = Boolean.FALSE;
			Boolean showemail = Boolean.FALSE;
			Boolean showurl = Boolean.FALSE;
			Boolean showblog = Boolean.FALSE;
			final String sphone = ipse.getSupervisorPhone();
			final String semail = ipse.getSupervisorMail();
			final String surl = ipse.getSupervisorURL();
			final String sblog = ipse.getSupervisorBlog();
			this.portletVC.contextPut("sphone", sphone);
			this.portletVC.contextPut("semail", semail);
			this.portletVC.contextPut("surl", surl);
			this.portletVC.contextPut("sblog", sblog);

			if (sphone != null && sphone.length() > 0) {
				showphone = Boolean.TRUE;
			}

			if (semail != null && semail.length() > 0) {
				showemail = Boolean.TRUE;
			}

			if (surl != null && surl.length() > 0) {
				showurl = Boolean.TRUE;
			}

			if (sblog != null && sblog.length() > 0) {
				showblog = Boolean.TRUE;
			}

			this.portletVC.contextPut("showphone", showphone);
			this.portletVC.contextPut("showemail", showemail);
			this.portletVC.contextPut("showurl", showurl);
			this.portletVC.contextPut("showblog", showblog);

			this.portletVC.contextPut("surl", ipse.getSupervisorURL());

			this.portletVC.contextPut("hasPolymorphLink", Boolean.FALSE);

			polyLinks = new ArrayList<String>();
			mapLinks = new HashMap<Link, PolymorphLink>();

			final List<PolymorphLink> polyList = ipe.getPolymorphLinks();
			if (polyList != null && polyList.size() > 0) {
				int i = 0;
				for (final PolymorphLink polymorphLink : polyList) {
					if ((polymorphLink.hasConditions() && (polymorphLink.getResultIDForUser(ureq) != null)) || !(polymorphLink.hasConditions())) {
						final Link polyLink = LinkFactory.createCustomLink("institutionPortlet.polymorphLink." + i, "none", polymorphLink.getLinkText(),
								Link.TOOLENTRY_DEFAULT + Link.NONTRANSLATED, portletVC, this);
						polyLink.setCustomEnabledLinkCSS(polymorphLink.getLinkType().equals("course") ? "o_institutionportlet_course"
								: "o_institutionportlet_coursefolder");
						polyLink.setCustomDisabledLinkCSS(polymorphLink.getLinkType().equals("course") ? "o_institutionportlet_course"
								: "o_institutionportlet_coursefolder");
						polyLinks.add(polyLink.getComponentName());
						mapLinks.put(polyLink, polymorphLink);
						i++;
					}
				}
				if (polyLinks.size() > 0) {
					this.portletVC.contextPut("polyLinks", polyLinks);
					this.portletVC.contextPut("numPolyLinks", polyLinks.size());
					this.portletVC.contextPut("hasPolymorphLink", Boolean.TRUE);
				}
			}
		}
		putInitialPanel(this.portletVC);
	}

	/**
	 * @see org.olat.gui.control.DefaultController#event(org.olat.gui.UserRequest, org.olat.gui.components.Component, org.olat.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// nothing to catch
		if (source instanceof Link) {
			final Link link = (Link) source;
			final PolymorphLink polyLink = mapLinks.get(link);

			Long resultIDForUser = null;
			Long defaultID = null;
			final String resultIDForUserS = polyLink.getResultIDForUser(ureq);

			if (resultIDForUserS != null) {
				try {
					resultIDForUser = Long.parseLong(resultIDForUserS);
				} catch (final NumberFormatException e) {
					Tracing.createLoggerFor(InstitutionPortletRunController.class).error(e.getMessage());
				}
			}
			try {
				defaultID = Long.parseLong(polyLink.getDefaultLink());
			} catch (final NumberFormatException e) {
				Tracing.createLoggerFor(InstitutionPortletRunController.class).error(e.getMessage());
			}

			if (polyLink.getLinkType().equals(InstitutionPortlet.TYPE_COURSE)) {
				final RepositoryManager rm = RepositoryManager.getInstance();
				RepositoryEntry re = null;

				// id corresponding to the conditions set for this user
				if (polyLink != null && resultIDForUser != null) {
					re = rm.lookupRepositoryEntry(resultIDForUser);
				}

				// if ressource is not available choose default link
				if (re == null && defaultID != null) {
					re = rm.lookupRepositoryEntry(defaultID);
				}

				if (re != null) {
					if (!rm.isAllowedToLaunch(ureq, re)) {
						getWindowControl().setWarning(translate("warn.cantlaunch"));
					} else {
						final OLATResourceable ores = re.getOlatResource();
						final DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
						DTab dt = dts.getDTab(ores);
						if (dt == null) {
							// does not yet exist -> create and add
							dt = dts.createDTab(ores, re.getDisplayname());
							if (dt == null) { return; }
							final Controller launchController = ControllerFactory.createLaunchController(ores, null, ureq, dt.getWindowControl(), true);
							dt.setController(launchController);
							dts.addDTab(dt);
						}
						dts.activate(ureq, dt, null); // null: do not activate a certain
														// view
					}
				} else {
					getWindowControl().setWarning(translate("warn.cantlaunch"));
				}
			} else if (polyLink.getLinkType().equals(InstitutionPortlet.TYPE_CATALOG)) {
				try {
					final CatalogEntry ce = CatalogManager.getInstance().loadCatalogEntry(resultIDForUser != null ? resultIDForUser : defaultID);
					final DTabs dts = (DTabs) getWindowControl().getWindowBackOffice().getWindow().getAttribute("DTabs");
					dts.activateStatic(ureq, RepositorySite.class.getName(), "search.catalog:" + ce.getKey());
				} catch (final Exception e) {
					Tracing.createLoggerFor(InstitutionPortletRunController.class).error(e.getMessage());
					getWindowControl().setWarning(translate("warn.cantlaunch"));
				}
			}
		}
	}

	/**
	 * @see org.olat.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		if (portletVC != null) {
			portletVC = null;
		}
		if (polyLinks != null) {
			polyLinks = null;
		}
		if (mapLinks != null) {
			mapLinks = null;
		}
		if (ipe != null) {
			ipe = null;
		}
	}

}
