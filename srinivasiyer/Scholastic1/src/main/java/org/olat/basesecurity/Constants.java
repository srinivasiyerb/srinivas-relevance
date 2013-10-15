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

package org.olat.basesecurity;

import org.olat.core.id.OLATResourceable;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public class Constants {

	/**
	 * <code>GROUP_OLATUSERS</code> predfined groups length restricted to 16 chars!
	 */
	public static final String GROUP_OLATUSERS = "users";
	/**
	 * <code>GROUP_ADMIN</code> predefined groups length restricted to 16 chars!
	 */
	public static final String GROUP_ADMIN = "admins";
	/**
	 * <code>GROUP_USERMANAGERS</code> predefined groups length restricted to 16 chars!
	 */
	public static final String GROUP_USERMANAGERS = "usermanagers";
	/**
	 * <code>GROUP_AUTHORS</code> predefined groups length restricted to 16 chars!
	 */
	public static final String GROUP_AUTHORS = "authors";
	/**
	 * <code>GROUP_INST_ORES_MANAGER</code> predefined groups length restricted to 16 chars!
	 */
	public static final String GROUP_INST_ORES_MANAGER = "instoresmanager";
	/**
	 * <code>GROUP_GROUPMANAGERS</code> predefined groups length restricted to 16 chars!
	 */
	public static final String GROUP_GROUPMANAGERS = "groupmanagers";
	/**
	 * <code>GROUP_ANONYMOUS</code> predefined groups length restricted to 16 chars!
	 */
	public static final String GROUP_ANONYMOUS = "anonymous";

	/**
	 * access a thing; means read, write, update, and delete <code>PERMISSION_ACCESS</code> predefined permissions length restricted to 16 chars!
	 */
	public static final String PERMISSION_ACCESS = "access";

	/**
	 * access a thing; means read, write, update, and delete <code>PERMISSION_READ</code> predefined permissions length restricted to 16 chars!
	 */
	public static final String PERMISSION_READ = "read";
	/**
	 * access a thing; means read, write, update, and delete <code>PERMISSION_WRITE</code> predefined permissions length restricted to 16 chars!
	 */
	public static final String PERMISSION_WRITE = "write";
	/**
	 * access a thing; means read, write, update, and delete <code>PERMISSION_UPDATE</code> predefined permissions length restricted to 16 chars!
	 */
	public static final String PERMISSION_UPDATE = "update";
	/**
	 * access a thing; means read, write, update, and delete <code>PERMISSION_DELETE</code> predefined permissions length restricted to 16 chars!
	 */
	public static final String PERMISSION_DELETE = "delete";

	/**
	 * group context permissions <code>PERMISSION_PARTI</code>
	 */
	public static final String PERMISSION_PARTI = "participant";
	/**
	 * <code>PERMISSION_COACH</code>
	 */
	public static final String PERMISSION_COACH = "coach";

	/**
	 * having a role; like being author <code>PERMISSION_HASROLE</code>
	 */
	public static final String PERMISSION_HASROLE = "hasRole";

	/**
	 * admin of e.g. the whole olat, or: a course, or: a buddy group <code>PERMISSION_ADMIN</code>
	 */
	public static final String PERMISSION_ADMIN = "admin";

	/**
	 * length restricted to 50 chars! <br>
	 * TYPE resource for the whole olat system (e.g. used with permission PERMISSION_LOGINDENIED) <br>
	 * <code>ORESOURCE_OLAT</code>
	 */
	public static final OLATResourceable ORESOURCE_OLAT = OresHelper.lookupType(BaseSecurityModule.class, "WHOLE-OLAT");

	/**
	 * resourceable TYPE for olat administrators <code>ORESOURCE_ADMIN</code>
	 */
	public static final OLATResourceable ORESOURCE_ADMIN = OresHelper.lookupType(BaseSecurityModule.class, "RAdmins");

	/**
	 * resourceable TYPE for authors <code>ORESOURCE_AUTHOR</code>
	 */
	public static final OLATResourceable ORESOURCE_AUTHOR = OresHelper.lookupType(BaseSecurityModule.class, "RAuthor");

	/**
	 * resourceable TYPE for groupmanagers <code>ORESOURCE_GROUPMANAGER</code>
	 */
	public static final OLATResourceable ORESOURCE_GROUPMANAGER = OresHelper.lookupType(BaseSecurityModule.class, "RGroupmanager");

	/**
	 * resourceable TYPE for usermanagers <code>ORESOURCE_USERMANAGER</code>
	 */
	public static final OLATResourceable ORESOURCE_USERMANAGER = OresHelper.lookupType(BaseSecurityModule.class, "RUsermanager");

	/**
	 * resourceable TYPE for institutionalresourcemanager <code>ORESOURCE_INSTORESMANAGER</code>
	 */
	public static final OLATResourceable ORESOURCE_INSTORESMANAGER = OresHelper.lookupType(BaseSecurityModule.class, "RResmanager");

	/**
	 * resourceable TYPE for all security groups <code>ORESOURCE_SECURITYGROUPS</code>
	 */
	public static final OLATResourceable ORESOURCE_SECURITYGROUPS = OresHelper.lookupType(BaseSecurityModule.class, "SecGroup");

	/**
	 * resourceable TYPE for all courses <code>ORESOURCE_COURSES</code>
	 */
	public static final OLATResourceable ORESOURCE_COURSES = OresHelper.lookupType(CourseModule.class);

	/**
	 * resourceable TYPE for olat users (everybody but guests) <code>ORESOURCE_USERS</code>
	 */
	public static final OLATResourceable ORESOURCE_USERS = OresHelper.lookupType(BaseSecurityModule.class, "RUsers");

	/**
	 * resourceable TYPE for olat guests (restricted functionality) <code>ORESOURCE_GUESTONLY</code>
	 */
	public static final OLATResourceable ORESOURCE_GUESTONLY = OresHelper.lookupType(BaseSecurityModule.class, "RGuestOnly");

}