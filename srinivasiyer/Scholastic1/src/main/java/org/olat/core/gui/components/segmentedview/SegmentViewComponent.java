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
package org.olat.core.gui.components.segmentedview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;

public class SegmentViewComponent extends Component {

	private boolean allowNoSelection;
	private boolean allowMultipleSelection;

	private final Set<Component> selectedSegments = new HashSet<Component>();
	private final List<Component> segments = new ArrayList<Component>();

	private final static SegmentViewRenderer RENDERER = new SegmentViewRenderer();

	public SegmentViewComponent(String name) {
		super(name);
	}

	public boolean isAllowMultipleSelection() {
		return allowMultipleSelection;
	}

	public void setAllowMultipleSelection(boolean allowMultipleSelection) {
		this.allowMultipleSelection = allowMultipleSelection;
	}

	public boolean isAllowNoSelection() {
		return allowNoSelection;
	}

	public void setAllowNoSelection(boolean allowNoSelection) {
		this.allowNoSelection = allowNoSelection;
	}

	public boolean isEmpty() {
		return segments.isEmpty();
	}

	public List<Component> getSegments() {
		return segments;
	}

	public void setSegments(List<Link> links) {
		segments.clear();
		selectedSegments.clear();
		segments.addAll(links);
		setDirty(true);
	}

	public void addSegment(Link link) {
		segments.add(link);
		setDirty(true);
	}

	public void addSegment(Component link, boolean selected) {
		segments.add(link);
		if (selected) {
			selectedSegments.add(link);
		} else {
			selectedSegments.remove(link);
		}
		setDirty(true);
	}

	public void removeSegment(String name) {
		for (Iterator<Component> it = segments.iterator(); it.hasNext();) {
			Component segment = it.next();
			if (name.equals(segment.getComponentName())) {
				it.remove();
				selectedSegments.remove(segment);
				setDirty(true);
			}
		}
	}

	public void removeSegment(Component cmp) {
		for (Iterator<Component> it = segments.iterator(); it.hasNext();) {
			Component segment = it.next();
			if (cmp == segment) {
				it.remove();
				selectedSegments.remove(segment);
				setDirty(true);
			}
		}
	}

	public boolean isSelected(Component component) {
		return selectedSegments.contains(component);
	}

	public void select(Component component) {
		if (segments.contains(component)) {
			selectedSegments.clear();
			selectedSegments.add(component);
			setDirty(true);
		}
	}

	@Override
	protected void doDispatchRequest(UserRequest ureq) {
		Event e = null;
		String cmd = ureq.getParameter(VelocityContainer.COMMAND_ID);
		int count = 0;
		for (Component segment : segments) {
			if (cmd.equals(segment.getComponentName())) {
				boolean selected = selectedSegments.contains(segment);
				if (selected) {
					if (isAllowNoSelection() || selectedSegments.size() > 1) {
						e = new SegmentViewEvent(SegmentViewEvent.DESELECTION_EVENT, segment.getComponentName(), count);
						selectedSegments.remove(segment);
					}
				} else {
					if (!isAllowMultipleSelection()) {
						selectedSegments.clear();
					}
					e = new SegmentViewEvent(SegmentViewEvent.SELECTION_EVENT, segment.getComponentName(), count);
					selectedSegments.add(segment);
				}
				break;
			}
			count++;
		}

		if (e != null) {
			setDirty(true);
			fireEvent(ureq, e);
		}
	}

	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}
}
