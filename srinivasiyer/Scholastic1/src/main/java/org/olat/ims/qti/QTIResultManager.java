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

package org.olat.ims.qti;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.user.UserDataDeletable;

/**
 * Description: Useful functions for download
 * 
 * @author Alexander Schneider
 */
public class QTIResultManager extends BasicManager implements UserDataDeletable {

	private static QTIResultManager instance;

	/**
	 * Constructor for QTIResultManager.
	 */
	private QTIResultManager(final UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
		instance = this;
	}

	/**
	 * @return QTIResultManager
	 */
	public static QTIResultManager getInstance() {
		return instance;
	}

	/**
	 * @param olatResource
	 * @param olatResourceDetail
	 * @param repositoryRef
	 * @return True if true, false otherwise.
	 */
	public boolean hasResultSets(final Long olatResource, final String olatResourceDetail, final Long repositoryRef) {
		return (getResultSets(olatResource, olatResourceDetail, repositoryRef, null).size() > 0);
	}

	/**
	 * Get the resulkt sets.
	 * 
	 * @param olatResource
	 * @param olatResourceDetail
	 * @param repositoryRef
	 * @param identity May be null
	 * @return List of resultsets
	 */
	public List getResultSets(final Long olatResource, final String olatResourceDetail, final Long repositoryRef, final Identity identity) {
		final Long olatRes = olatResource;
		final String olatResDet = olatResourceDetail;
		final Long repRef = repositoryRef;

		final DB db = DBFactory.getInstance();

		final StringBuilder slct = new StringBuilder();
		slct.append("select rset from ");
		slct.append("org.olat.ims.qti.QTIResultSet rset ");
		slct.append("where ");
		slct.append("rset.olatResource=? ");
		slct.append("and rset.olatResourceDetail=? ");
		slct.append("and rset.repositoryRef=? ");
		if (identity != null) {
			slct.append("and rset.identity.key=? ");
			return db.find(slct.toString(), new Object[] { olatRes, olatResDet, repRef, identity.getKey() }, new Type[] { Hibernate.LONG, Hibernate.STRING,
					Hibernate.LONG, Hibernate.LONG });
		} else {
			return db.find(slct.toString(), new Object[] { olatRes, olatResDet, repRef }, new Type[] { Hibernate.LONG, Hibernate.STRING, Hibernate.LONG });
		}
	}

	/**
	 * selects all resultsets of a IQCourseNode of a particular course
	 * 
	 * @param olatResource
	 * @param olatResourceDetail
	 * @param repositoryRef
	 * @return List of QTIResult objects
	 */
	public List selectResults(final Long olatResource, final String olatResourceDetail, final Long repositoryRef, final int type) {
		final Long olatRes = olatResource;
		final String olatResDet = olatResourceDetail;
		final Long repRef = repositoryRef;

		final DB db = DBFactory.getInstance();
		// join with user to sort by name
		final StringBuilder slct = new StringBuilder();
		slct.append("select res from ");
		slct.append("org.olat.ims.qti.QTIResultSet rset, ");
		slct.append("org.olat.ims.qti.QTIResult res, ");
		slct.append("org.olat.core.id.Identity identity, ");
		slct.append("org.olat.user.UserImpl usr ");
		slct.append("where ");
		slct.append("rset.key = res.resultSet ");
		slct.append("and rset.identity = identity.key ");
		slct.append("and identity.user = usr.key ");
		slct.append("and rset.olatResource=? ");
		slct.append("and rset.olatResourceDetail=? ");
		slct.append("and rset.repositoryRef=? ");
		// 1 -> iqtest, 2 -> iqself
		if (type == 1 || type == 2) {
			slct.append("order by usr.properties['").append(UserConstants.LASTNAME).append("'] , rset.assessmentID, res.itemIdent");
		} else {
			slct.append("order by rset.creationDate, rset.assessmentID, res.itemIdent");
		}

		List results = null;
		results = db.find(slct.toString(), new Object[] { olatRes, olatResDet, repRef }, new Type[] { Hibernate.LONG, Hibernate.STRING, Hibernate.LONG });

		return results;
	}

	/**
	 * deletes all Results and ResultSets of a test, selftest or survey
	 * 
	 * @param olatRes
	 * @param olatResDet
	 * @param repRef
	 * @return deleted ResultSets
	 */
	public int deleteAllResults(final Long olatRes, final String olatResDet, final Long repRef) {
		final DB db = DBFactory.getInstance();

		final StringBuilder slct = new StringBuilder();
		slct.append("select rset from ");
		slct.append("org.olat.ims.qti.QTIResultSet rset ");
		slct.append("where ");
		slct.append("rset.olatResource=? ");
		slct.append("and rset.olatResourceDetail=? ");
		slct.append("and rset.repositoryRef=? ");

		List results = null;
		results = db.find(slct.toString(), new Object[] { olatRes, olatResDet, repRef }, new Type[] { Hibernate.LONG, Hibernate.STRING, Hibernate.LONG });

		final String delRes = "from res in class org.olat.ims.qti.QTIResult where res.resultSet.key = ?";
		final String delRset = "from rset in class org.olat.ims.qti.QTIResultSet where rset.key = ?";

		int deletedRset = 0;

		for (final Iterator iter = results.iterator(); iter.hasNext();) {
			final QTIResultSet rSet = (QTIResultSet) iter.next();
			final Long rSetKey = rSet.getKey();
			db.delete(delRes, rSetKey, Hibernate.LONG);
			db.delete(delRset, rSetKey, Hibernate.LONG);
			deletedRset++;
		}
		return deletedRset;
	}

	/**
	 * Deletes all Results and ResultSets for certain QTI-ResultSet.
	 * 
	 * @param qtiResultSet
	 */
	public void deleteResults(final QTIResultSet qtiResultSet) {
		deleteAllResults(qtiResultSet.getOlatResource(), qtiResultSet.getOlatResourceDetail(), qtiResultSet.getRepositoryRef());
	}

	/**
	 * translates the answerstring stored in table o_qtiresult
	 * 
	 * @param answerCode
	 * @return translation
	 */
	public static Map parseResponseStrAnswers(final String answerCode) {
		// calculate the correct answer, if eventually needed
		int modus = 0;
		int startIdentPosition = 0;
		int startCharacterPosition = 0;
		String tempIdent = null;
		final Map result = new HashMap();
		char c;

		for (int i = 0; i < answerCode.length(); i++) {
			c = answerCode.charAt(i);
			if (modus == 0) {
				if (c == '[') {
					final String sIdent = answerCode.substring(startIdentPosition, i);
					if (sIdent.length() > 0) {
						tempIdent = sIdent;
						modus = 1;
					}
				}
			} else if (modus == 1) {
				if (c == '[') {
					startCharacterPosition = i + 1;
					modus = 2;
				} else if (c == ']') {
					startIdentPosition = i + 1;
					tempIdent = null;
					modus = 0;
				}
			} else if (modus == 2) {
				if (c == ']') {
					if (answerCode.charAt(i - 1) != '\\') {
						final String s = answerCode.substring(startCharacterPosition, i);
						if (tempIdent != null) {
							result.put(tempIdent, s.replaceAll("\\\\\\]", "]"));
						}
						modus = 1;
					}
				}
			}
		}
		return result;
	}

	/**
	 * translates the answerstring stored in table o_qtiresult
	 * 
	 * @param answerCode
	 * @return translation
	 */
	public static List parseResponseLidAnswers(final String answerCode) {
		// calculate the correct answer, if eventually needed
		int modus = 0;
		int startCharacterPosition = 0;
		final List result = new ArrayList();
		char c;

		for (int i = 0; i < answerCode.length(); i++) {
			c = answerCode.charAt(i);
			if (modus == 0) {
				if (c == '[') {
					modus = 1;
				}
			} else if (modus == 1) {
				if (c == '[') {
					startCharacterPosition = i + 1;
					modus = 2;
				} else if (c == ']') {
					modus = 0;
				}
			} else if (modus == 2) {
				if (c == ']') {
					if (answerCode.charAt(i - 1) != '\\') {
						final String s = answerCode.substring(startCharacterPosition, i);
						result.add(s.replaceAll("\\\\\\]", "]"));
						modus = 1;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Find all ResultSets for certain identity.
	 * 
	 * @param identity
	 * @param assessmentID
	 * @return
	 */
	public List findQtiResultSets(final Identity identity) {
		return DBFactory.getInstance().find("from q in class org.olat.ims.qti.QTIResultSet where q.identity =?", identity.getKey(), Hibernate.LONG);
	}

	/**
	 * Delete all ResultSet for certain identity.
	 * 
	 * @param identity
	 */
	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		final List qtiResults = findQtiResultSets(identity);
		for (final Iterator iter = qtiResults.iterator(); iter.hasNext();) {
			deleteResultSet((QTIResultSet) iter.next());
		}
		Tracing.logDebug("Delete all QTI result data in db for identity=" + identity, this.getClass());
	}

	/**
	 * Delete all qti-results and qti-result-set entry for certain result-set.
	 * 
	 * @param rSet
	 */
	private void deleteResultSet(final QTIResultSet rSet) {
		final Long rSetKey = rSet.getKey();
		final DB db = DBFactory.getInstance();
		db.delete("from res in class org.olat.ims.qti.QTIResult where res.resultSet.key = ?", rSetKey, Hibernate.LONG);
		db.delete("from rset in class org.olat.ims.qti.QTIResultSet where rset.key = ?", rSetKey, Hibernate.LONG);
	}

}