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
package org.olat.portfolio.manager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.services.tagging.manager.TaggingManager;
import org.olat.core.commons.services.tagging.model.Tag;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.model.EPFilterSettings;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.structel.EPStructureToArtefactLink;

/**
 * Description:<br>
 * EPArtefactManager manage the artefacts
 * <P>
 * Initial Date: 11.06.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPArtefactManager extends BasicManager {

	private static final String ARTEFACT_FULLTEXT_ON_FS = "ARTEFACT_FULLTEXT_ON_FS";
	// those are here as instance variable, as mocking the tests won't be possible
	// without!
	// it also helps to find failures in loading a manager, as already spring
	// would warn and not
	// only later on, when a click happens.
	private DB dbInstance;
	private PortfolioModule portfolioModule;
	private TaggingManager taggingManager;
	// end.

	private static final int ARTEFACT_FULLTEXT_DB_FIELD_LENGTH = 16384;
	public static final String ARTEFACT_CONTENT_FILENAME = "artefactContent.html";
	private static final String ARTEFACT_INTERNALDATA_FOLDER = "data";

	private VFSContainer artefactsRoot;

	/**
	 * 
	 */
	public EPArtefactManager() {
		//
	}

	/**
	 * [used by Spring]
	 * 
	 * @param dbInstance
	 */
	public void setDbInstance(final DB dbInstance) {
		this.dbInstance = dbInstance;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param portfolioModule
	 */
	public void setPortfolioModule(final PortfolioModule portfolioModule) {
		this.portfolioModule = portfolioModule;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param taggingManager
	 */
	public void setTaggingManager(final TaggingManager taggingManager) {
		this.taggingManager = taggingManager;
	}

	/**
	 * load the persisted artefact from FS
	 */
	void loadFile() {
		//
	}

	/**
	 * convert html/text to PDF and save in Filesystem
	 */
	void persistAsPDF() {
		//
	}

	/**
	 * Used by the indexer to retrieve all the artefacts
	 * 
	 * @param artefactIds List of ids to seek (optional)
	 * @param firstResult First position
	 * @param maxResults Max number of returned artefacts (0 or below for all)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<AbstractArtefact> getArtefacts(final Identity author, final List<Long> artefactIds, final int firstResult, final int maxResults) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select artefact from ").append(AbstractArtefact.class.getName()).append(" artefact");
		boolean where = false;
		if (author != null) {
			where = true;
			sb.append(" where artefact.author=:author");
		}
		if (artefactIds != null && !artefactIds.isEmpty()) {
			if (where) {
				sb.append(" and ");
			} else {
				sb.append(" where ");
			}
			sb.append(" artefact.id in (:artefactIds)");
		}
		final DBQuery query = dbInstance.createQuery(sb.toString());
		if (maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		if (firstResult >= 0) {
			query.setFirstResult(firstResult);
		}
		if (author != null) {
			query.setEntity("author", author);
		}
		if (artefactIds != null && !artefactIds.isEmpty()) {
			query.setParameterList("artefactIds", artefactIds);
		}

		final List<AbstractArtefact> artefacts = query.list();
		return artefacts;
	}

	protected boolean isArtefactClosed(final AbstractArtefact artefact) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select count(link) from ").append(EPStructureToArtefactLink.class.getName()).append(" link ").append(" inner join link.structureElement structure ")
				.append(" inner join structure.root rootStructure").append(" where link.artefact=:artefact and rootStructure.status='closed'");

		final DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("artefact", artefact);
		final Number count = (Number) query.uniqueResult();
		return count.intValue() > 0;
	}

	protected List<AbstractArtefact> getArtefactPoolForUser(final Identity ident) {
		final long start = System.currentTimeMillis();
		final StringBuilder sb = new StringBuilder();
		sb.append("select artefact from ").append(AbstractArtefact.class.getName()).append(" artefact").append(" where author=:author");
		final DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("author", ident);
		@SuppressWarnings("unchecked")
		final List<AbstractArtefact> artefacts = query.list();
		if (artefacts.isEmpty()) { return null; }
		final long duration = System.currentTimeMillis() - start;
		if (isLogDebugEnabled()) {
			logDebug("loading the full artefact pool took " + duration + "ms");
		}
		return artefacts;
	}

	protected VFSContainer getArtefactsRoot() {
		if (artefactsRoot == null) {
			final VFSContainer root = portfolioModule.getPortfolioRoot();
			final VFSItem artefactsItem = root.resolve("artefacts");
			if (artefactsItem == null) {
				artefactsRoot = root.createChildContainer("artefacts");
			} else if (artefactsItem instanceof VFSContainer) {
				artefactsRoot = (VFSContainer) artefactsItem;
			} else {
				logError("The root folder for artefact is a file and not a folder", null);
			}
		}
		return artefactsRoot;
	}

	protected VFSContainer getArtefactsTempContainer(final Identity ident) {
		final VFSContainer artRoot = new OlatRootFolderImpl(File.separator + "tmp", null);
		VFSItem tmpI = artRoot.resolve("portfolio");
		if (tmpI == null) {
			tmpI = artRoot.createChildContainer("portfolio");
		}
		VFSItem userTmp = tmpI.resolve(ident.getName());
		if (userTmp == null) {
			userTmp = ((VFSContainer) tmpI).createChildContainer(ident.getName());
		}
		final String idFolder = String.valueOf(((System.currentTimeMillis() % 1000l)) * 100);
		final VFSContainer thisTmp = ((VFSContainer) userTmp).createChildContainer(idFolder);
		return thisTmp;
	}

	protected List<String> getArtefactTags(final AbstractArtefact artefact) {
		// wrap concrete artefact as abstract-artefact to get the correct resName for the tag
		if (artefact.getKey() == null) { return null; }
		final OLATResourceable artefactOres = OresHelper.createOLATResourceableInstance(AbstractArtefact.class, artefact.getKey());
		final List<String> tags = taggingManager.getTagsAsString(null, artefactOres, null, null);
		return tags;
	}

	protected void setArtefactTag(final Identity identity, final AbstractArtefact artefact, final String tag) {
		// wrap concrete artefact as abstract-artefact to get the correct resName for the tag
		final OLATResourceable artefactOres = OresHelper.createOLATResourceableInstance(AbstractArtefact.class, artefact.getKey());
		taggingManager.createAndPersistTag(identity, tag, artefactOres, null, null);
	}

	protected void setArtefactTags(final Identity identity, final AbstractArtefact artefact, final List<String> tags) {
		if (tags == null) { return; }
		// wrap concrete artefact as abstract-artefact to get the correct resName for the tag
		final OLATResourceable artefactOres = OresHelper.createOLATResourceableInstance(AbstractArtefact.class, artefact.getKey());
		final List<Tag> oldTags = taggingManager.loadTagsForResource(artefactOres, null, null);
		final List<String> oldTagStrings = new ArrayList<String>();
		final List<String> tagsToAdd = new ArrayList<String>(tags.size());
		tagsToAdd.addAll(tags);
		if (oldTags != null) { // there might be no tags yet
			for (final Tag oTag : oldTags) {
				if (tags.contains(oTag.getTag())) {
					// still existing, nothing to do
					oldTagStrings.add(oTag.getTag());
					tagsToAdd.remove(oTag.getTag());
				} else {
					// tag was deleted, remove it
					taggingManager.deleteTag(oTag);
				}
			}
		}
		// look for all given tags, add the ones yet missing
		for (final String tag : tagsToAdd) {
			if (StringHelper.containsNonWhitespace(tag)) {
				taggingManager.createAndPersistTag(identity, tag, artefactOres, null, null);
			}
		}
	}

	/**
	 * Create and persist an artefact of the given type
	 * 
	 * @param type
	 * @return The persisted artefact
	 */
	protected AbstractArtefact createAndPersistArtefact(final Identity identity, final String type) {
		final EPArtefactHandler<?> handler = portfolioModule.getArtefactHandler(type);
		if (handler != null && handler.isEnabled()) {
			final AbstractArtefact artefact = handler.createArtefact();
			artefact.setAuthor(identity);

			dbInstance.saveObject(artefact);
			saveArtefactFulltextContent(artefact);
			return artefact;
		} else {
			return null;
		}
	}

	protected AbstractArtefact updateArtefact(final AbstractArtefact artefact) {
		if (artefact == null) { return null; }

		String tmpFulltext = artefact.getFulltextContent();
		if (StringHelper.containsNonWhitespace(tmpFulltext) && artefact.getFulltextContent().equals(ARTEFACT_FULLTEXT_ON_FS)) {
			tmpFulltext = getArtefactFullTextContent(artefact);
		}
		artefact.setFulltextContent("");
		if (artefact.getKey() == null) {
			dbInstance.saveObject(artefact);
		} else {
			dbInstance.updateObject(artefact);
		}
		artefact.setFulltextContent(tmpFulltext);
		saveArtefactFulltextContent(artefact);

		return artefact;
	}

	// decides itself if fulltext fits into db or will be written on fs
	protected boolean saveArtefactFulltextContent(final AbstractArtefact artefact) {
		final String fullText = artefact.getFulltextContent();
		if (StringHelper.containsNonWhitespace(fullText)) {
			if (fullText.length() > ARTEFACT_FULLTEXT_DB_FIELD_LENGTH) {
				// save the real content on FS
				try {
					final VFSContainer container = getArtefactContainer(artefact);
					VFSLeaf artData = (VFSLeaf) container.resolve(ARTEFACT_CONTENT_FILENAME);
					if (artData == null) {
						artData = container.createChildLeaf(ARTEFACT_CONTENT_FILENAME);
					}
					VFSManager.copyContent(new ByteArrayInputStream(fullText.getBytes()), artData, true);
					artefact.setFulltextContent(ARTEFACT_FULLTEXT_ON_FS);
					dbInstance.updateObject(artefact);
				} catch (final Exception e) {
					logError("could not really save the fulltext content of an artefact", e);
					return false;
				}
			} else {
				// if length is shorter, but still a file there -> delete it (but only if loading included the long version from fs before, else its overwritten!)
				VFSLeaf artData = (VFSLeaf) getArtefactContainer(artefact).resolve(ARTEFACT_INTERNALDATA_FOLDER + "/" + ARTEFACT_CONTENT_FILENAME); // v.1 had /data/ in
																																					// path
				if (artData != null) {
					artData.delete();
				}
				artData = (VFSLeaf) getArtefactContainer(artefact).resolve(ARTEFACT_CONTENT_FILENAME);
				if (artData != null) {
					artData.delete();
				}
				dbInstance.updateObject(artefact); // persist fulltext in db
			}
		}
		return true;
	}

	protected String getArtefactFullTextContent(final AbstractArtefact artefact) {
		VFSLeaf artData = (VFSLeaf) getArtefactContainer(artefact).resolve(ARTEFACT_CONTENT_FILENAME);
		if (artData == null) {
			artData = (VFSLeaf) getArtefactContainer(artefact).resolve(ARTEFACT_INTERNALDATA_FOLDER + "/" + ARTEFACT_CONTENT_FILENAME); // fallback to
		}
		// v.1
		if (artData != null) {
			return FileUtils.load(artData.getInputStream(), "utf-8");
		} else {
			return artefact.getFulltextContent();
		}
	}

	/**
	 * This is an optimized method to filter a list of artefact by tags and return the tags of this list of artefacts. This prevent to search two times or more the list
	 * of tags of an artefact.
	 * 
	 * @param identity
	 * @param tags
	 * @return the filtered artefacts and their tags
	 */
	protected EPArtefactTagCloud getArtefactsAndTagCloud(final Identity identity, final List<String> tags) {
		final List<AbstractArtefact> artefacts = getArtefactPoolForUser(identity);
		final EPFilterSettings filterSettings = new EPFilterSettings();
		filterSettings.setTagFilter(tags);

		final Set<String> newTags = new HashSet<String>();
		filterArtefactsByTags(artefacts, filterSettings, newTags);

		return new EPArtefactTagCloud(artefacts, newTags);
	}

	protected List<AbstractArtefact> filterArtefactsByFilterSettings(final List<AbstractArtefact> allArtefacts, final EPFilterSettings filterSettings) {
		final long start = System.currentTimeMillis();
		if (allArtefacts == null) { return null; }
		final List<AbstractArtefact> filteredArtefactList = new ArrayList<AbstractArtefact>(allArtefacts.size());
		filteredArtefactList.addAll(allArtefacts);
		if (filterSettings != null && !filterSettings.isFilterEmpty()) {
			if (filteredArtefactList.size() != 0) {
				filterArtefactsByTags(filteredArtefactList, filterSettings, null);
			}
			if (filteredArtefactList.size() != 0) {
				filterArtefactsByType(filteredArtefactList, filterSettings.getTypeFilter());
			}
			if (filteredArtefactList.size() != 0) {
				filterArtefactsByString(filteredArtefactList, filterSettings.getTextFilter());
			}
			if (filteredArtefactList.size() != 0) {
				filterArtefactsByDate(filteredArtefactList, filterSettings.getDateFilter());
			}
		}
		final long duration = System.currentTimeMillis() - start;
		if (isLogDebugEnabled()) {
			logDebug("filtering took " + duration + "ms");
		}
		return filteredArtefactList;
	}

	/**
	 * @param allArtefacts
	 * @param filterSettings (containing tags to filter for or boolean if filter should keep only artefacts without a tag)
	 * @param collect the tags found in the filtered artefacts
	 * @return filtered artefact list
	 */
	private void filterArtefactsByTags(final List<AbstractArtefact> artefacts, final EPFilterSettings filterSettings, final Set<String> cloud) {
		final List<String> tags = filterSettings.getTagFilter();
		// either search for artefacts with given tags, or such with no one!
		final List<AbstractArtefact> toRemove = new ArrayList<AbstractArtefact>();
		if (tags != null && tags.size() != 0) {
			// TODO: epf: RH: fix needed, as long as tags with uppercase initial are
			// allowed!
			for (final AbstractArtefact artefact : artefacts) {
				final List<String> artefactTags = getArtefactTags(artefact);
				if (!artefactTags.containsAll(tags)) {
					toRemove.add(artefact);
				} else if (cloud != null) {
					cloud.addAll(artefactTags);
				}
			}
			artefacts.removeAll(toRemove);
		} else if (filterSettings.isNoTagFilterSet()) {
			for (final AbstractArtefact artefact : artefacts) {
				if (!getArtefactTags(artefact).isEmpty()) {
					toRemove.add(artefact);
				}
			}
			artefacts.removeAll(toRemove);
		}
	}

	private void filterArtefactsByType(final List<AbstractArtefact> artefacts, final List<String> type) {
		if (type != null && type.size() != 0) {
			final List<AbstractArtefact> toRemove = new ArrayList<AbstractArtefact>();
			for (final AbstractArtefact artefact : artefacts) {
				if (!type.contains(artefact.getResourceableTypeName())) {
					toRemove.add(artefact);
				}
			}
			artefacts.removeAll(toRemove);
		}
	}

	/**
	 * date comparison will first set startDate to 00:00:00 and set endDate to 23:59:59 else there might be no results if start = end date. dateList must be set according
	 * to: dateList(0) = startDate dateList(1) = endDate
	 */
	private void filterArtefactsByDate(final List<AbstractArtefact> artefacts, final List<Date> dateList) {
		if (dateList != null && dateList.size() != 0) {
			if (dateList.size() == 2) {
				Date startDate = dateList.get(0);
				Date endDate = dateList.get(1);
				final Calendar cal = Calendar.getInstance();
				if (startDate == null) {
					cal.set(1970, 1, 1);
				} else {
					cal.setTime(startDate);
				}
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				startDate = cal.getTime();
				cal.setTime(endDate);
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 59);
				cal.set(Calendar.SECOND, 59);
				endDate = cal.getTime();
				final List<AbstractArtefact> toRemove = new ArrayList<AbstractArtefact>();
				for (final AbstractArtefact artefact : artefacts) {
					final Date creationDate = artefact.getCreationDate();
					if (!(creationDate.before(endDate) && creationDate.after(startDate))) {
						toRemove.add(artefact);
					}
				}
				artefacts.removeAll(toRemove);
			} else {
				throw new AssertException("provided DateList must contain exactly two Date-objects");
			}
		}
	}

	private void filterArtefactsByString(final List<AbstractArtefact> artefacts, final String textFilter) {
		if (StringHelper.containsNonWhitespace(textFilter)) {
			final List<AbstractArtefact> toRemove = new ArrayList<AbstractArtefact>();
			for (final AbstractArtefact artefact : artefacts) {
				final String textCompare = artefact.getTitle() + artefact.getDescription() + artefact.getFulltextContent();
				if (!textCompare.toLowerCase().contains(textFilter.toLowerCase())) {
					toRemove.add(artefact);
				}
			}
			artefacts.removeAll(toRemove);
		}
	}

	/**
	 * Load the artefact by its primary key
	 * 
	 * @param key The primary key
	 * @return The artefact or null if nothing found
	 */
	protected AbstractArtefact loadArtefactByKey(final Long key) {
		if (key == null) { throw new NullPointerException(); }

		final StringBuilder sb = new StringBuilder();
		sb.append("select artefact from ").append(AbstractArtefact.class.getName()).append(" artefact").append(" where artefact=:key");

		final DBQuery query = dbInstance.createQuery(sb.toString());
		query.setLong("key", key);

		@SuppressWarnings("unchecked")
		final List<AbstractArtefact> artefacts = query.list();
		// if not found, it is an empty list
		if (artefacts.isEmpty()) { return null; }
		return artefacts.get(0);
	}

	protected List<AbstractArtefact> loadArtefactsByBusinessPath(final String businessPath, final Identity author) {
		if (author == null) { return null; }
		if (!StringHelper.containsNonWhitespace(businessPath)) { return null; }
		final StringBuilder sb = new StringBuilder();
		sb.append("select artefact from ").append(AbstractArtefact.class.getName()).append(" artefact")
				.append(" where artefact.businessPath=:bpath and artefact.author=:ident");

		final DBQuery query = dbInstance.createQuery(sb.toString());
		query.setString("bpath", businessPath);
		query.setEntity("ident", author);

		@SuppressWarnings("unchecked")
		final List<AbstractArtefact> artefacts = query.list();
		// if not found, it is an empty list
		if (artefacts.isEmpty()) { return null; }
		return artefacts;
	}

	protected void deleteArtefact(final AbstractArtefact artefact) {
		getArtefactContainer(artefact).delete();
		// wrap concrete artefact as abstract-artefact to get the correct resName for the tag
		final OLATResourceable artefactOres = OresHelper.createOLATResourceableInstance(AbstractArtefact.class, artefact.getKey());
		taggingManager.deleteTags(artefactOres, null, null);

		dbInstance.deleteObject(artefact);
		logInfo("Deleted artefact " + artefact.getTitle() + " with key: " + artefact.getKey());
	}

	protected VFSContainer getArtefactContainer(final AbstractArtefact artefact) {
		final Long key = artefact.getKey();
		if (key == null) { throw new AssertException("artefact not yet persisted -> no key available!"); }
		VFSContainer container = null;
		final VFSItem item = getArtefactsRoot().resolve(key.toString());
		if (item == null) {
			container = getArtefactsRoot().createChildContainer(key.toString());
		} else if (item instanceof VFSContainer) {
			container = (VFSContainer) item;
		} else {
			logError("Cannot create a container for artefact: " + artefact, null);
		}
		return container;
	}

}
