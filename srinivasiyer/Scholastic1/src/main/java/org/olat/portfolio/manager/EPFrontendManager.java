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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.Policy;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.services.search.AbstractOlatDocument;
import org.olat.core.commons.services.search.ResultDocument;
import org.olat.core.commons.services.search.SearchResults;
import org.olat.core.commons.services.tagging.manager.TaggingManager;
import org.olat.core.commons.services.tagging.model.Tag;
import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.Coordinator;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.assessment.AssessmentNotificationsHandler;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.group.BusinessGroup;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.model.EPFilterSettings;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.structel.EPPage;
import org.olat.portfolio.model.structel.EPStructureElement;
import org.olat.portfolio.model.structel.EPStructuredMap;
import org.olat.portfolio.model.structel.EPTargetResource;
import org.olat.portfolio.model.structel.ElementType;
import org.olat.portfolio.model.structel.PortfolioStructure;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.resource.OLATResource;
import org.olat.search.service.indexer.identity.PortfolioArtefactIndexer;
import org.olat.search.service.searcher.SearchClientProxy;

/**
 * Description:<br>
 * Manager for common used tasks for ePortfolio. Should be used for all calls from controllers. will itself use all other managers to manipulate artefacts or
 * structureElements and policies.
 * <P>
 * Initial Date: 11.06.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPFrontendManager extends BasicManager {

	private final Coordinator coordinator;
	private final BaseSecurity securityManager;
	private final EPArtefactManager artefactManager;
	private final EPStructureManager structureManager;
	private final TaggingManager taggingManager;
	private final AssessmentNotificationsHandler assessmentNotificationsHandler;
	private final DB dbInstance;
	private SearchClientProxy searchClient;
	private final EPSettingsManager settingsManager;
	private final EPPolicyManager policyManager;
	private PortfolioModule portfolioModule;

	/**
	 * [for Spring]
	 */
	public EPFrontendManager(final EPArtefactManager artefactManager, final EPStructureManager structureManager, final EPSettingsManager settingsManager,
			final EPPolicyManager policyManager, final CoordinatorManager coordinatorManager, final BaseSecurity securityManager, final TaggingManager taggingManager,
			final DB dbInstance, final AssessmentNotificationsHandler assessmentNotificationsHandler) {
		this.artefactManager = artefactManager;
		this.structureManager = structureManager;
		this.securityManager = securityManager;
		this.coordinator = coordinatorManager.getCoordinator();
		this.taggingManager = taggingManager;
		this.assessmentNotificationsHandler = assessmentNotificationsHandler;
		this.dbInstance = dbInstance;
		this.settingsManager = settingsManager;
		this.policyManager = policyManager;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param searchClient
	 */
	public void setSearchClient(final SearchClientProxy searchClient) {
		this.searchClient = searchClient;
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
	 * Create and persist an artefact of the given type
	 * 
	 * @param type
	 * @return The persisted artefact
	 */
	public AbstractArtefact createAndPersistArtefact(final Identity identity, final String type) {
		return artefactManager.createAndPersistArtefact(identity, type);
	}

	/**
	 * Persists the artefact and returns the new version
	 * 
	 * @param artefact
	 * @return The last version of the artefact
	 */
	public AbstractArtefact updateArtefact(final AbstractArtefact artefact) {
		return artefactManager.updateArtefact(artefact);
	}

	/**
	 * delete an artefact and also its vfs-artefactContainer
	 * 
	 * @param artefact
	 */
	public void deleteArtefact(AbstractArtefact artefact) {
		final List<PortfolioStructure> linksToArtefact = structureManager.getAllReferencesForArtefact(artefact);
		for (final PortfolioStructure portfolioStructure : linksToArtefact) {
			structureManager.removeArtefactFromStructure(artefact, portfolioStructure);
		}
		// load again as session might be closed between
		artefact = artefactManager.loadArtefactByKey(artefact.getKey());
		artefactManager.deleteArtefact(artefact);
	}

	public boolean isArtefactClosed(final AbstractArtefact artefact) {
		return artefactManager.isArtefactClosed(artefact);
	}

	public PortfolioStructure removeArtefactFromStructure(final AbstractArtefact artefact, final PortfolioStructure structure) {
		return structureManager.removeArtefactFromStructure(artefact, structure);
	}

	/**
	 * Create and persist a link between a structure element and an artefact.
	 * 
	 * @param author The author of the link
	 * @param artefact The artefact to link
	 * @param structure The structure element
	 * @return The link
	 */
	public boolean addArtefactToStructure(final Identity author, final AbstractArtefact artefact, final PortfolioStructure structure) {
		return structureManager.addArtefactToStructure(author, artefact, structure);
	}

	/**
	 * move artefact from old to new structure do so by removing and re-adding to new target
	 * 
	 * @param artefact
	 * @param oldParStruct
	 * @param newParStruct
	 * @return true if adding was successful
	 */
	public boolean moveArtefactFromStructToStruct(final AbstractArtefact artefact, final PortfolioStructure oldParStruct, final PortfolioStructure newParStruct) {
		return structureManager.moveArtefactFromStructToStruct(artefact, oldParStruct, newParStruct);
	}

	/**
	 * move a structure to a new parent-structure and removes old link
	 * 
	 * @param structToBeMvd
	 * @param oldParStruct
	 * @param newParStruct
	 * @return true if no exception occured
	 */
	public boolean moveStructureToNewParentStructure(final PortfolioStructure structToBeMvd, final PortfolioStructure oldParStruct, final PortfolioStructure newParStruct) {
		return structureManager.moveStructureToNewParentStructure(structToBeMvd, oldParStruct, newParStruct);
	}

	/**
	 * set the reflexion for the link structureElement <-> artefact this can be a different reflexion than the one of the artefact. Reflects why the artefact was added to
	 * this structure!
	 * 
	 * @param artefact
	 * @param structure
	 * @param reflexion
	 * @return
	 */
	public boolean setReflexionForArtefactToStructureLink(final AbstractArtefact artefact, final PortfolioStructure structure, final String reflexion) {
		return structureManager.setReflexionForArtefactToStructureLink(artefact, structure, reflexion);
	}

	/**
	 * get the reflexion set on the link structureElement <-> artefact this can be a different reflexion than the one of the artefact. Reflects why the artefact was added
	 * to this structure!
	 * 
	 * @param artefact
	 * @param structure
	 * @return String reflexion
	 */
	public String getReflexionForArtefactToStructureLink(final AbstractArtefact artefact, final PortfolioStructure structure) {
		return structureManager.getReflexionForArtefactToStructureLink(artefact, structure);
	}

	/**
	 * counts amount of artefact in all structures and every child element
	 * 
	 * @param structure
	 * @return
	 */
	public int countArtefactsRecursively(final PortfolioStructure structure) {
		return structureManager.countArtefactsRecursively(structure, 0);
	}

	public int countArtefactsInMap(final PortfolioStructureMap map) {
		return structureManager.countArtefactsRecursively(map);
	}

	/**
	 * looks if the given artefact exists in the PortfolioStructure
	 * 
	 * @param artefact
	 * @param structure
	 * @return
	 */
	public boolean isArtefactInStructure(final AbstractArtefact artefact, final PortfolioStructure structure) {
		return structureManager.isArtefactInStructure(artefact, structure);
	}

	/**
	 * load all artefacts with given businesspath from given identity this mostly is just to lookup for existance of already collected artefacts from same source
	 * 
	 * @param businessPath
	 * @param author
	 * @return
	 */
	public List<AbstractArtefact> loadArtefactsByBusinessPath(final String businessPath, final Identity author) {
		return artefactManager.loadArtefactsByBusinessPath(businessPath, author);
	}

	/**
	 * List artefacts for indexing
	 * 
	 * @param author (optional)
	 * @param firstResult (optional)
	 * @param maxResults (optional)
	 * @return
	 */
	public List<AbstractArtefact> getArtefacts(final Identity author, final int firstResult, final int maxResults) {
		return artefactManager.getArtefacts(author, null, firstResult, maxResults);
	}

	/**
	 * Load the artefact by its primary key
	 * 
	 * @param key The primary key
	 * @return The artefact or null if nothing found
	 */
	public AbstractArtefact loadArtefactByKey(final Long key) {
		return artefactManager.loadArtefactByKey(key);
	}

	/**
	 * get the users choice of attributes or a default
	 * 
	 * @return
	 */
	public Map<String, Boolean> getArtefactAttributeConfig(final Identity ident) {
		return settingsManager.getArtefactAttributeConfig(ident);
	}

	/**
	 * persist the users chosen attributes to show as a property
	 * 
	 * @param ident
	 * @param artAttribConfig
	 */
	public void setArtefactAttributeConfig(final Identity ident, final Map<String, Boolean> artAttribConfig) {
		settingsManager.setArtefactAttributeConfig(ident, artAttribConfig);
	}

	/**
	 * get all persisted filters from a given user
	 * 
	 * @param ident
	 * @return filtersettings or list with an empty filter, if none were found
	 */
	public List<EPFilterSettings> getSavedFilterSettings(final Identity ident) {
		return settingsManager.getSavedFilterSettings(ident);
	}

	/**
	 * persist users filter settings as property, only save such with a name
	 * 
	 * @param ident
	 * @param filterList
	 */
	public void setSavedFilterSettings(final Identity ident, final List<EPFilterSettings> filterList) {
		settingsManager.setSavedFilterSettings(ident, filterList);
	}

	/**
	 * remove a given filter from users list
	 * 
	 * @param ident
	 * @param filterName
	 */
	public void deleteFilterFromUsersList(final Identity ident, final String filterID) {
		settingsManager.deleteFilterFromUsersList(ident, filterID);
	}

	/**
	 * get the last selected PortfolioStructure of this user
	 * 
	 * @param ident Identity
	 * @return the loaded PortfolioStructure
	 */
	public PortfolioStructure getUsersLastUsedPortfolioStructure(final Identity ident) {
		final Long structKey = settingsManager.getUsersLastUsedPortfolioStructureKey(ident);
		if (structKey != null) {
			final PortfolioStructure struct = structureManager.loadPortfolioStructureByKey(structKey);
			return struct;
		}
		return null;
	}

	/**
	 * get the users prefered viewing mode for artefacts (either table / preview)
	 * 
	 * @param ident
	 * @return
	 */
	public String getUsersPreferedArtefactViewMode(final Identity ident, final String context) {
		return settingsManager.getUsersPreferedArtefactViewMode(ident, context);
	}

	/**
	 * persist the users prefered viewing mode for artefacts (either table / preview)
	 * 
	 * @param ident
	 * @param preferedMode
	 */
	public void setUsersPreferedArtefactViewMode(final Identity ident, final String preferedMode, final String context) {
		settingsManager.setUsersPreferedArtefactViewMode(ident, preferedMode, context);
	}

	/**
	 * persist the last uses PortfolioStructure to use it later on
	 * 
	 * @param ident Identity
	 * @param struct
	 */
	public void setUsersLastUsedPortfolioStructure(final Identity ident, final PortfolioStructure struct) {
		settingsManager.setUsersLastUsedPortfolioStructure(ident, struct);
	}

	/**
	 * returns an array of tags for given artefact
	 * 
	 * @param artefact
	 * @return null if none are found
	 */
	public List<String> getArtefactTags(final AbstractArtefact artefact) {
		return artefactManager.getArtefactTags(artefact);
	}

	/**
	 * add a tag to an artefact (will save a tag pointing to this artefact)
	 * 
	 * @param identity
	 * @param artefact
	 * @param tag
	 */
	public void setArtefactTag(final Identity identity, final AbstractArtefact artefact, final String tag) {
		artefactManager.setArtefactTag(identity, artefact, tag);
	}

	/**
	 * add a List of tags to an artefact
	 * 
	 * @param identity
	 * @param artefact
	 * @param tags
	 */
	public void setArtefactTags(final Identity identity, final AbstractArtefact artefact, final List<String> tags) {
		artefactManager.setArtefactTags(identity, artefact, tags);
	}

	/**
	 * get all maps wherein (or in sub-structures) the given artefact is linked.
	 * 
	 * @param artefact
	 * @return
	 */
	public List<PortfolioStructure> getReferencedMapsForArtefact(final AbstractArtefact artefact) {
		return structureManager.getReferencedMapsForArtefact(artefact);
	}

	/**
	 * get all artefacts for the given identity this represents the artefact pool
	 * 
	 * @param ident
	 * @return
	 */
	public List<AbstractArtefact> getArtefactPoolForUser(final Identity ident) {
		return artefactManager.getArtefactPoolForUser(ident);
	}

	public EPArtefactTagCloud getArtefactsAndTagCloud(final Identity identity, final List<String> tags) {
		return artefactManager.getArtefactsAndTagCloud(identity, tags);
	}

	/**
	 * filter the provided list of artefacts with different filters
	 * 
	 * @param allArtefacts the list to manipulate on
	 * @param filterSettings Settings for the filter to work on
	 * @return
	 */
	public List<AbstractArtefact> filterArtefactsByFilterSettings(final EPFilterSettings filterSettings, final Identity identity, final Roles roles) {
		final List<Long> artefactKeys = fulltextSearchAfterArtefacts(filterSettings, identity, roles);
		if (artefactKeys == null || artefactKeys.isEmpty()) {
			final List<AbstractArtefact> allArtefacts = artefactManager.getArtefactPoolForUser(identity);
			return artefactManager.filterArtefactsByFilterSettings(allArtefacts, filterSettings);
		}

		final List<AbstractArtefact> artefacts = artefactManager.getArtefacts(identity, artefactKeys, 0, 500);
		// remove the text-filter when the lucene-search got some results before
		final EPFilterSettings settings = filterSettings.cloneAfterFullText();
		return artefactManager.filterArtefactsByFilterSettings(artefacts, settings);
	}

	private List<Long> fulltextSearchAfterArtefacts(final EPFilterSettings filterSettings, final Identity identity, final Roles roles) {
		final String query = filterSettings.getTextFilter();
		if (StringHelper.containsNonWhitespace(query)) {
			try {
				final List<String> queries = new ArrayList<String>();
				appendAnd(queries, AbstractOlatDocument.RESERVED_TO, ":\"", identity.getKey().toString(), "\"");
				appendAnd(queries, "(", AbstractOlatDocument.DOCUMENTTYPE_FIELD_NAME, ":(", PortfolioArtefactIndexer.TYPE, "*))");
				final SearchResults searchResults = searchClient.doSearch(query, queries, identity, roles, 0, 1000, false);

				final List<Long> keys = new ArrayList<Long>();
				if (searchResults != null) {
					final String marker = AbstractArtefact.class.getSimpleName();
					for (final ResultDocument doc : searchResults.getList()) {
						final String businessPath = doc.getResourceUrl();
						int start = businessPath.indexOf(marker);
						if (start > 0) {
							start += marker.length() + 1;
							final int stop = businessPath.indexOf(']', start);
							if (stop < businessPath.length()) {
								final String keyStr = businessPath.substring(start, stop);
								try {
									keys.add(Long.parseLong(keyStr));
								} catch (final Exception e) {
									logError("Not a primary key: " + keyStr, e);
								}
							}
						}
					}
				}
				return keys;
			} catch (final Exception e) {
				logError("", e);
				return Collections.emptyList();
			}
		} else {
			return Collections.emptyList();
		}
	}

	private void appendAnd(final List<String> queries, final String... strings) {
		final StringBuilder query = new StringBuilder();
		for (final String string : strings) {
			query.append(string);
		}

		if (query.length() > 0) {
			queries.add(query.toString());
		}
	}

	/**
	 * returns defined amount of users mostly used tags, sorted by occurrence of tag
	 * 
	 * @param ident
	 * @param amount nr of tags to return, if 0: the default (5) will be returned, if -1: you will get all
	 * @return a combined map with tags including occurrence and tag format: "house (7), house"
	 */
	public Map<String, String> getUsersMostUsedTags(final Identity ident, Integer amount) {
		amount = (amount == 0) ? 5 : amount;
		final List<String> outp = new ArrayList<String>();

		final Map<String, String> res = new HashMap<String, String>();
		final List<Map<String, Integer>> bla = taggingManager.getUserTagsWithFrequency(ident);
		for (final Map<String, Integer> map : bla) {
			final String caption = map.get("tag") + " (" + map.get("nr") + ")";
			outp.add(caption);
			res.put(caption, String.valueOf(map.get("tag")));
			if (amount == res.size()) {
				break;
			}
		}

		return res;
	}

	/**
	 * get all tags a user owns, ordered and without duplicates
	 * 
	 * @param ident
	 * @return
	 */
	public List<String> getUsersTags(final Identity ident) {
		return taggingManager.getUserTagsAsString(ident);
	}

	/**
	 * get all tags restricted to Artefacts a user owns, ordered and without duplicates
	 * 
	 * @param ident
	 * @return
	 */
	public List<String> getUsersTagsOfArtefactType(final Identity ident) {
		return taggingManager.getUserTagsOfTypeAsString(ident, AbstractArtefact.class.getSimpleName());
	}

	/**
	 * lookup resources for a given tags
	 * 
	 * @param tagList
	 * @return
	 */
	public Set<OLATResourceable> getResourcesByTags(final List<Tag> tagList) {
		return taggingManager.getResourcesByTags(tagList);
	}

	/**
	 * get all tags for a given resource
	 * 
	 * @param ores
	 * @return
	 */
	public List<Tag> loadTagsForResource(final OLATResourceable ores) {
		return taggingManager.loadTagsForResource(ores, null, null);
	}

	/**
	 * sync map with its former source (template)
	 */
	public boolean synchronizeStructuredMapToUserCopy(final PortfolioStructureMap map) {
		final EPStructuredMap userMap = (EPStructuredMap) map;
		final EPStructureManager structMgr = structureManager; // only remove
																// synthetic access
																// warnings

		final Boolean synched = coordinator.getSyncer().doInSync(map.getOlatResource(), new SyncerCallback<Boolean>() {
			@Override
			public Boolean execute() {
				if (userMap.getStructuredMapSource() == null) { return Boolean.FALSE; }
				// need to reload it, I don't know why
				final Long templateKey = userMap.getStructuredMapSource().getKey();
				userMap.setLastSynchedDate(new Date());
				final PortfolioStructure template = structMgr.loadPortfolioStructureByKey(templateKey);
				structMgr.syncStructureRecursively(template, userMap, true);
				return Boolean.TRUE;
			}
		});

		return synched.booleanValue();
	}

	/**
	 * Assign a structure map to user. In other words, make a copy of the template and set the user as an author.
	 * 
	 * @param identity
	 * @param portfolioStructureStructuredMapTemplate
	 */
	// TODO: when implementing transactions, pay attention to this
	public PortfolioStructureMap assignStructuredMapToUser(final Identity identity, final PortfolioStructureMap mapTemplate, final OLATResourceable targetOres,
			final String targetSubPath, final String targetBusinessPath, final Date deadline) {
		// doInSync is here to check for nested doInSync exception in first place
		final Identity author = identity;
		// only remove synthetic access warnings
		final EPStructureManager structMgr = structureManager;
		final PortfolioStructureMap template = mapTemplate;
		final OLATResourceable ores = targetOres;
		final String subPath = targetSubPath;

		final PortfolioStructureMap map = coordinator.getSyncer().doInSync(template.getOlatResource(), new SyncerCallback<PortfolioStructureMap>() {
			@Override
			public PortfolioStructureMap execute() {
				final String title = template.getTitle();
				final String description = template.getDescription();
				final PortfolioStructureMap copy = structMgr.createPortfolioStructuredMap(template, author, title, description, ores, subPath, targetBusinessPath);
				if (copy instanceof EPStructuredMap) {
					((EPStructuredMap) copy).setDeadLine(deadline);
				}
				structMgr.copyStructureRecursively(template, copy, true);
				return copy;
			}
		});
		return map;
	}

	/**
	 * Low level function to copy the structure of elements, with or without the artefacts
	 * 
	 * @param source
	 * @param target
	 * @param withArtefacts
	 */
	public void copyStructureRecursively(final PortfolioStructure source, final PortfolioStructure target, final boolean withArtefacts) {
		structureManager.copyStructureRecursively(source, target, withArtefacts);
	}

	/**
	 * Return the structure elements of the given type without permission control. Need this for indexing.
	 * 
	 * @param firstResult
	 * @param maxResults
	 * @param type
	 * @return
	 */
	public List<PortfolioStructure> getStructureElements(final int firstResult, final int maxResults, final ElementType... type) {
		return structureManager.getStructureElements(firstResult, maxResults, type);
	}

	/**
	 * get all Structure-Elements linked to identity over a security group (owner)
	 * 
	 * @param ident
	 * @return
	 */
	public List<PortfolioStructure> getStructureElementsForUser(final Identity identity, final ElementType... type) {
		return structureManager.getStructureElementsForUser(identity, type);
	}

	/**
	 * Get all Structure-Elements linked which the identity can see over a policy,
	 * 
	 * @param ident The identity which what see maps
	 * @param chosenOwner Limit maps from this identity
	 * @param type Limit maps to this or these types
	 * @return
	 */
	public List<PortfolioStructure> getStructureElementsFromOthers(final Identity ident, final Identity chosenOwner, final ElementType... type) {
		return structureManager.getStructureElementsFromOthers(ident, chosenOwner, type);
	}

	/**
	 * Get all Structure-Elements linked which the identity can see over a policy, WITHOUT those that are public to all OLAT users ( GROUP_OLATUSERS ) !! this should be
	 * used, to save performance when there are a lot of public shared maps!!
	 * 
	 * @param ident The identity which what see maps
	 * @param chosenOwner Limit maps from this identity
	 * @param type Limit maps to this or these types
	 * @return
	 */
	public List<PortfolioStructure> getStructureElementsFromOthersWithoutPublic(final Identity ident, final Identity choosenOwner, final ElementType... types) {
		return structureManager.getStructureElementsFromOthersWithoutPublic(ident, choosenOwner, types);
	}

	/**
	 * Return the list of artefacts glued to this structure element
	 * 
	 * @param structure
	 * @return A list of artefacts
	 */
	public List<AbstractArtefact> getArtefacts(final PortfolioStructure structure) {
		return structureManager.getArtefacts(structure);
	}

	/**
	 * Check the collect restriction against the structure element
	 * 
	 * @param structure
	 * @return
	 */
	public boolean checkCollectRestriction(final PortfolioStructure structure) {
		return structureManager.checkCollectRestriction(structure);
	}

	public boolean checkCollectRestrictionOfMap(final PortfolioStructureMap structure) {
		return checkAllCollectRestrictionRec(structure);
	}

	protected boolean checkAllCollectRestrictionRec(final PortfolioStructure structure) {
		boolean allOk = structureManager.checkCollectRestriction(structure);
		final List<PortfolioStructure> children = structureManager.loadStructureChildren(structure);
		for (final PortfolioStructure child : children) {
			allOk &= checkAllCollectRestrictionRec(child);
		}
		return allOk;
	}

	/**
	 * Create a map for a user
	 * 
	 * @param root
	 * @param identity
	 * @param title
	 * @param description
	 * @return
	 */
	public PortfolioStructureMap createAndPersistPortfolioDefaultMap(final Identity identity, final String title, final String description) {
		final PortfolioStructureMap map = structureManager.createPortfolioDefaultMap(identity, title, description);
		structureManager.savePortfolioStructure(map);
		return map;
	}

	/**
	 * Create a map for a group
	 * 
	 * @param root
	 * @param group
	 * @param title
	 * @param description
	 * @return
	 */
	public PortfolioStructureMap createAndPersistPortfolioDefaultMap(final BusinessGroup group, final String title, final String description) {
		final PortfolioStructureMap map = structureManager.createPortfolioDefaultMap(group, title, description);
		structureManager.savePortfolioStructure(map);
		return map;
	}

	/**
	 * Create a structured map, based on template.
	 * 
	 * @param identity The author/owner of the map
	 * @param title
	 * @param description
	 * @return The structure element
	 */
	public PortfolioStructureMap createAndPersistPortfolioStructuredMap(final PortfolioStructureMap template, final Identity identity, final String title,
			final String description, final OLATResourceable targetOres, final String targetSubPath, final String targetBusinessPath) {
		final PortfolioStructureMap map = structureManager.createPortfolioStructuredMap(template, identity, title, description, targetOres, targetSubPath,
				targetBusinessPath);
		structureManager.savePortfolioStructure(map);
		return map;
	}

	/**
	 * create a structure-element
	 * 
	 * @param root
	 * @param title
	 * @param description
	 * @return
	 */
	public PortfolioStructure createAndPersistPortfolioStructureElement(final PortfolioStructure root, final String title, final String description) {
		final EPStructureElement newStruct = (EPStructureElement) structureManager.createPortfolioStructure(root, title, description);
		if (root != null) {
			structureManager.addStructureToStructure(root, newStruct);
		}
		structureManager.savePortfolioStructure(newStruct);
		return newStruct;
	}

	/**
	 * create a page
	 * 
	 * @param root
	 * @param title
	 * @param description
	 * @return
	 */
	public PortfolioStructure createAndPersistPortfolioPage(final PortfolioStructure root, final String title, final String description) {
		final EPPage newPage = (EPPage) structureManager.createPortfolioPage(root, title, description);
		if (root != null) {
			structureManager.addStructureToStructure(root, newPage);
		}
		structureManager.savePortfolioStructure(newPage);
		return newPage;
	}

	/**
	 * This method is reserved to the repository. It removes the template completely
	 * 
	 * @param pStruct
	 */
	public void deletePortfolioMapTemplate(final OLATResourceable res) {
		structureManager.deletePortfolioMapTemplate(res);
	}

	/**
	 * delete a portfoliostructure recursively with its childs
	 * 
	 * @param pStruct
	 */
	public void deletePortfolioStructure(final PortfolioStructure pStruct) {
		structureManager.removeStructureRecursively(pStruct);
	}

	/**
	 * save or update a structure
	 * 
	 * @param pStruct
	 */
	public void savePortfolioStructure(final PortfolioStructure pStruct) {
		structureManager.savePortfolioStructure(pStruct);
	}

	/**
	 * Number of children
	 */
	public int countStructureChildren(final PortfolioStructure structure) {
		return structureManager.countStructureChildren(structure);
	}

	/**
	 * Load a protfolio structure by its resource
	 * 
	 * @param ores
	 * @return
	 */
	public PortfolioStructure loadPortfolioStructure(final OLATResourceable ores) {
		return structureManager.loadPortfolioStructure(ores);
	}

	/**
	 * Load a portfolio structure by its primary key
	 * 
	 * @param key cannot be null
	 * @return The structure element or null if not found
	 */
	public PortfolioStructure loadPortfolioStructureByKey(final Long key) {
		return structureManager.loadPortfolioStructureByKey(key);
	}

	/**
	 * Retrieve the parent of the structure
	 * 
	 * @param structure
	 * @return
	 */
	public PortfolioStructure loadStructureParent(final PortfolioStructure structure) {
		return structureManager.loadStructureParent(structure);
	}

	/**
	 * Retrieve the children structures
	 * 
	 * @param structure
	 * @return
	 */
	public List<PortfolioStructure> loadStructureChildren(final PortfolioStructure structure) {
		return structureManager.loadStructureChildren(structure);
	}

	/**
	 * @param structure
	 * @param firstResult
	 * @param maxResults
	 * @return
	 */
	public List<PortfolioStructure> loadStructureChildren(final PortfolioStructure structure, final int firstResult, final int maxResults) {
		return structureManager.loadStructureChildren(structure, firstResult, maxResults);
	}

	public PortfolioStructureMap loadPortfolioStructureMap(final Identity identity, final PortfolioStructureMap template, final OLATResourceable targetOres,
			final String targetSubPath, final String targetBusinessPath) {
		// sync the map with the template on opening it in gui, not on loading!
		return structureManager.loadPortfolioStructuredMap(identity, template, targetOres, targetSubPath, targetBusinessPath);
	}

	/**
	 * get the "already in use" state of a structuredMapTemplate
	 * 
	 * @param template
	 * @param targetOres
	 * @param targetSubPath
	 * @param targetBusinessPath
	 * @return
	 */
	public boolean isTemplateInUse(final PortfolioStructureMap template, final OLATResourceable targetOres, final String targetSubPath, final String targetBusinessPath) {
		return structureManager.isTemplateInUse(template, targetOres, targetSubPath, targetBusinessPath);
	}

	/**
	 * get root vfs-container where artefact file-system data is persisted
	 * 
	 * @return
	 */
	public VFSContainer getArtefactsRoot() {
		return artefactManager.getArtefactsRoot();
	}

	/**
	 * get vfs-container of a specific artefact
	 * 
	 * @param artefact
	 * @return
	 */
	public VFSContainer getArtefactContainer(final AbstractArtefact artefact) {
		return artefactManager.getArtefactContainer(artefact);
	}

	/**
	 * get a temporary folder to store files while in wizzard
	 * 
	 * @param ident
	 * @return
	 */
	public VFSContainer getArtefactsTempContainer(final Identity ident) {
		return artefactManager.getArtefactsTempContainer(ident);
	}

	/**
	 * as large fulltext-content of an artefact is persisted on filesystem, use this method to get fulltext
	 * 
	 * @param artefact
	 * @return
	 */
	public String getArtefactFullTextContent(final AbstractArtefact artefact) {
		return artefactManager.getArtefactFullTextContent(artefact);
	}

	/**
	 * Check if the identity is the owner of this portfolio resource.
	 * 
	 * @param identity
	 * @param ores
	 * @return
	 */
	public boolean isMapOwner(final Identity identity, final OLATResourceable ores) {
		return structureManager.isMapOwner(identity, ores);
	}

	/**
	 * Check if the identity is owner of the portfolio resource or in a valid policy.
	 * 
	 * @param identity
	 * @param ores
	 * @return
	 */
	public boolean isMapVisible(final Identity identity, final OLATResourceable ores) {
		return structureManager.isMapVisible(identity, ores);
	}

	public boolean isMapShared(final PortfolioStructureMap map) {
		final OLATResource resource = map.getOlatResource();
		final List<Policy> policies = securityManager.getPoliciesOfResource(resource, null);
		for (final Policy policy : policies) {
			if (policy.getPermission().contains(Constants.PERMISSION_READ)) { return true; }
		}
		return false;
	}

	/**
	 * Return a list of wrapper containing the read policies of the map
	 * 
	 * @param map
	 */
	public List<EPMapPolicy> getMapPolicies(final PortfolioStructureMap map) {
		return policyManager.getMapPolicies(map);
	}

	/**
	 * Update the map policies of a map. The missing policies are deleted!
	 * 
	 * @param map
	 * @param policyWrappers
	 */
	public void updateMapPolicies(final PortfolioStructureMap map, final List<EPMapPolicy> policyWrappers) {
		policyManager.updateMapPolicies(map, policyWrappers);
	}

	/**
	 * submit and close a structured map from a portfolio task
	 * 
	 * @param map
	 */
	public void submitMap(final PortfolioStructureMap map) {
		submitMap(map, true);
	}

	private void submitMap(final PortfolioStructureMap map, final boolean logActivity) {
		if (!(map instanceof EPStructuredMap)) { return;// add an exception
		}

		final EPStructuredMap submittedMap = (EPStructuredMap) map;
		structureManager.submitMap(submittedMap);

		final EPTargetResource resource = submittedMap.getTargetResource();
		final OLATResourceable courseOres = resource.getOLATResourceable();
		final ICourse course = CourseFactory.loadCourse(courseOres);
		final AssessmentManager am = course.getCourseEnvironment().getAssessmentManager();
		final CourseNode courseNode = course.getRunStructure().getNode(resource.getSubPath());

		final List<Identity> owners = securityManager.getIdentitiesOfSecurityGroup(submittedMap.getOwnerGroup());
		for (final Identity owner : owners) {
			final IdentityEnvironment ienv = new IdentityEnvironment();
			ienv.setIdentity(owner);
			final UserCourseEnvironment uce = new UserCourseEnvironmentImpl(ienv, course.getCourseEnvironment());
			if (logActivity) {
				am.incrementNodeAttempts(courseNode, owner, uce);
			} else {
				am.incrementNodeAttemptsInBackground(courseNode, owner, uce);
			}

			assessmentNotificationsHandler.markPublisherNews(owner, course.getResourceableId());
			logAudit("Map " + map + " from " + owner.getName() + " has been submitted.");
		}
	}

	/**
	 * Close all maps after the deadline if there is a deadline. It can be a long running process if a lot of maps are involved.
	 */
	public void closeMapAfterDeadline() {
		final List<PortfolioStructureMap> mapsToClose = structureManager.getOpenStructuredMapAfterDeadline();
		final int count = 0;
		for (final PortfolioStructureMap mapToClose : mapsToClose) {
			submitMap(mapToClose, false);
			if (count % 5 == 0) {
				// this possibly takes longer than connection timeout, so do intermediatecommits.
				dbInstance.intermediateCommit();
			}
		}
	}

	/**
	 * get a valid name of style for a given PortfolioStructure if style is not enabled anymore, the default will be used.
	 * 
	 * @param struct
	 * @return the set style or the default from config if nothing is set.
	 */
	public String getValidStyleName(final PortfolioStructure struct) {
		// first style in list is the default, can be named default.
		final List<String> allStyles = portfolioModule.getAvailableMapStyles();
		if (allStyles == null || allStyles.size() == 0) { throw new AssertException("at least one style (that also exists in brasato.css must be configured for maps."); }
		final String styleName = ((EPStructureElement) struct).getStyle();
		if (StringHelper.containsNonWhitespace(styleName) && allStyles.contains(styleName)) { return styleName; }
		return allStyles.get(0);
	}

	/**
	 * The structure will be without any check on the DB copied. All the children structures MUST be loaded. This method is to use with the output of XStream at examples.
	 * 
	 * @param root
	 * @param identity
	 * @return The persisted structure
	 */
	public PortfolioStructureMap importPortfolioMapTemplate(final PortfolioStructure root, final Identity identity) {
		return structureManager.importPortfolioMapTemplate(root, identity);
	}

	// not yet available
	public void archivePortfolio() {}

	// not yet available
	public void exportPortfolio() {}

	// not yet available
	public void importPortfolio() {}

}
