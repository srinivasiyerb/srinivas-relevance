package org.olat.commons.info.portlet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.commons.info.manager.InfoMessageFrontendManager;
import org.olat.commons.info.model.InfoMessage;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.BaseTableDataModelWithoutFilter;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.CustomCellRenderer;
import org.olat.core.gui.components.table.CustomRenderColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableDataModel;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.portal.AbstractPortletRunController;
import org.olat.core.gui.control.generic.portal.PortletDefaultTableDataModel;
import org.olat.core.gui.control.generic.portal.PortletEntry;
import org.olat.core.gui.control.generic.portal.PortletToolSortingControllerImpl;
import org.olat.core.gui.control.generic.portal.SortingCriteria;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.SubscriptionInfo;
import org.olat.core.util.notifications.items.SubscriptionListItem;
import org.olat.home.site.HomeSite;

import com.ibm.icu.util.Calendar;

/**
 * Description:<br>
 * Show the last five infos
 * <P>
 * Initial Date: 27 juil. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoMessagePortletRunController extends AbstractPortletRunController implements GenericEventListener {

	private final Link showAllLink;
	private TableController tableController;
	private final VelocityContainer portletVC;

	public InfoMessagePortletRunController(final WindowControl wControl, final UserRequest ureq, final Translator trans, final String portletName) {
		super(wControl, ureq, trans, portletName);

		portletVC = createVelocityContainer("infosPortlet");
		showAllLink = LinkFactory.createLink("portlet.showall", portletVC, this);

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("portlet.no_messages"));
		tableConfig.setDisplayTableHeader(false);
		tableConfig.setCustomCssClass("b_portlet_table");
		tableConfig.setDisplayRowCount(false);
		tableConfig.setPageingEnabled(false);
		tableConfig.setDownloadOffered(false);
		tableConfig.setSortingEnabled(false);

		removeAsListenerAndDispose(tableController);
		tableController = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		tableController.addColumnDescriptor(new CustomRenderColumnDescriptor("peekview.title", 0, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_LEFT,
				new InfoNodeRenderer(Formatter.getInstance(getLocale()))));

		listenTo(tableController);

		sortingTermsList.add(SortingCriteria.DATE_SORTING);
		sortingCriteria = getPersistentSortingConfiguration(ureq);
		sortingCriteria.setSortingTerm(SortingCriteria.DATE_SORTING);
		reloadModel(sortingCriteria);

		portletVC.put("table", tableController.getInitialComponent());

		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, getIdentity(), InfoMessageFrontendManager.oresFrontend);

		putInitialPanel(portletVC);
	}

	@Override
	protected SortingCriteria createDefaultSortingCriteria() {
		final SortingCriteria sortingCriteria = new SortingCriteria(this.sortingTermsList);
		sortingCriteria.setAscending(false);
		return sortingCriteria;
	}

	@Override
	public synchronized void doDispose() {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(this, InfoMessageFrontendManager.oresFrontend);
		super.doDispose();
	}

	@Override
	public void event(final Event event) {
		if ("new_info_message".equals(event.getCommand())) {
			reloadModel(sortingCriteria);
		}
	}

	@Override
	protected Comparator<InfoPortletEntry> getComparator(final SortingCriteria criteria) {
		return new InfoPortletEntryComparator(criteria);
	}

	/**
	 * @param items
	 * @return
	 */
	private List<PortletEntry> convertToPortletEntryList(final List<InfoSubscriptionItem> infos) {
		final List<PortletEntry> convertedList = new ArrayList<PortletEntry>();
		long i = 0;
		for (final InfoSubscriptionItem info : infos) {
			convertedList.add(new InfoPortletEntry(i++, info));
		}
		return convertedList;
	}

	@Override
	protected void reloadModel(final SortingCriteria criteria) {
		final List<SubscriptionInfo> infos = NotificationsManager.getInstance().getSubscriptionInfos(getIdentity(), "InfoMessage");
		final List<InfoSubscriptionItem> items = new ArrayList<InfoSubscriptionItem>();
		for (final SubscriptionInfo info : infos) {
			for (final SubscriptionListItem item : info.getSubscriptionListItems()) {
				items.add(new InfoSubscriptionItem(info, item));
			}
		}
		List<PortletEntry> entries = convertToPortletEntryList(items);
		entries = getSortedList(entries, criteria);
		final InfosTableModel model = new InfosTableModel(entries);
		tableController.setTableDataModel(model);
	}

	@Override
	protected void reloadModel(final List<PortletEntry> sortedItems) {
		final InfosTableModel model = new InfosTableModel(sortedItems);
		tableController.setTableDataModel(model);
	}

	protected PortletToolSortingControllerImpl createSortingTool(final UserRequest ureq, final WindowControl wControl) {
		if (portletToolsController == null) {
			final List<PortletEntry> empty = Collections.<PortletEntry> emptyList();
			final PortletDefaultTableDataModel defaultModel = new PortletDefaultTableDataModel(empty, 2) {
				@Override
				public Object getValueAt(final int row, final int col) {
					return null;
				}
			};
			portletToolsController = new PortletToolSortingControllerImpl(ureq, wControl, getTranslator(), sortingCriteria, defaultModel, empty);
			portletToolsController.setConfigManualSorting(false);
			portletToolsController.setConfigAutoSorting(true);
			portletToolsController.addControllerListener(this);
		}
		return portletToolsController;
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == showAllLink) {
			final DateFormat format = new SimpleDateFormat("yyyyMMdd");
			final Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.MONTH, -1);
			// the end is businessPath compatible
			final String activationCmd = "adminnotifications.[news:0][type=" + InfoMessage.class.getSimpleName() + ":0][date=" + format.format(cal.getTime()) + ":0]";
			final DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
			dts.activateStatic(ureq, HomeSite.class.getName(), activationCmd);
		}
	}

	public class InfosTableModel extends BaseTableDataModelWithoutFilter implements TableDataModel {
		private final List<PortletEntry> infos;

		public InfosTableModel(final List<PortletEntry> infos) {
			this.infos = infos;
		}

		@Override
		public int getColumnCount() {
			return 1;
		}

		@Override
		public int getRowCount() {
			return infos.size();
		}

		@Override
		public Object getValueAt(final int row, final int col) {
			final InfoPortletEntry entry = (InfoPortletEntry) infos.get(row);
			switch (col) {
				case 0:
					return entry.getValue();
				default:
					return entry;
			}
		}
	}

	public class InfoNodeRenderer implements CustomCellRenderer {
		private final Formatter formatter;

		public InfoNodeRenderer(final Formatter formatter) {
			this.formatter = formatter;
		}

		@Override
		public void render(final StringOutput sb, final Renderer renderer, final Object val, final Locale locale, final int alignment, final String action) {
			if (val instanceof InfoSubscriptionItem) {
				final InfoSubscriptionItem isi = (InfoSubscriptionItem) val;
				final SubscriptionListItem item = isi.getItem();
				final SubscriptionInfo info = isi.getInfo();
				// title
				final String title = info.getTitle(SubscriptionInfo.MIME_PLAIN);

				String tip = null;
				final boolean tooltip = StringHelper.containsNonWhitespace(item.getDescriptionTooltip());
				if (tooltip) {
					final StringBuilder tipSb = new StringBuilder();
					tipSb.append("<b>").append(title).append(":</b>").append("<br/>").append(Formatter.escWithBR(Formatter.truncate(item.getDescriptionTooltip(), 256)));
					tip = StringEscapeUtils.escapeHtml(tipSb.toString());
					sb.append("<span ext:qtip=\"").append(tip).append("\">");
				} else {
					sb.append("<span>");
				}
				sb.append(Formatter.truncate(title, 30)).append("</span>&nbsp;");
				// link
				final String infoTitle = Formatter.truncate(item.getDescription(), 30);
				sb.append("<a href=\"").append(item.getLink()).append("\" class=\"o_portlet_infomessage_link\"");
				if (tooltip) {
					sb.append("ext:qtip=\"").append(tip).append("\"");
				}
				sb.append(">").append(infoTitle).append("</a>");
			} else {
				sb.append("-");
			}
		}
	}
}
