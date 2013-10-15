package org.olat.restapi.support.vo.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Description:<br>
 * task course node configuration
 * <P>
 * Initial Date: 27.07.2010 <br>
 * 
 * @author skoeber
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "taskConfigVO")
public class TaskConfigVO {

	/** @see org.olat.course.nodes.TACourseNode.CONF_TASK_ENABLED */
	private Boolean isAssignmentEnabled;
	/** @see org.olat.course.nodes.TACourseNode.CONF_TASK_TYPE */
	private String taskAssignmentType;
	/** @see org.olat.course.nodes.TACourseNode.CONF_TASK_TEXT */
	private String taskAssignmentText;
	/** @see org.olat.course.nodes.TACourseNode.CONF_TASK_PREVIEW */
	private Boolean isTaskPreviewEnabled;
	/** @see org.olat.course.nodes.TACourseNode.CONF_TASK_DESELECT */
	private Boolean isTaskDeselectEnabled;
	/** @see org.olat.course.nodes.TACourseNode.CONF_TASK_SAMPLING_WITH_REPLACEMENT */
	private Boolean onlyOneUserPerTask;

	/** @see org.olat.course.nodes.TACourseNode.CONF_DROPBOX_ENABLED */
	private Boolean isDropboxEnabled;
	/** @see org.olat.course.nodes.TACourseNode.CONF_DROPBOX_ENABLEMAIL */
	private Boolean isDropboxConfirmationMailEnabled;
	/** @see org.olat.course.nodes.TACourseNode.CONF_DROPBOX_CONFIRMATION */
	private String dropboxConfirmationText;

	/** @see org.olat.course.nodes.TACourseNode.CONF_RETURNBOX_ENABLED */
	private Boolean isReturnboxEnabled;

	/** @see org.olat.course.nodes.TACourseNode.CONF_SCORING_ENABLED */
	private Boolean isScoringEnabled;
	/** @see org.olat.course.nodes.MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD */
	private Boolean isScoringGranted;
	/** @see org.olat.course.nodes.MSCourseNode.CONFIG_KEY_SCORE_MIN */
	private Float minScore;
	/** @see org.olat.course.nodes.MSCourseNode.CONFIG_KEY_SCORE_MAX */
	private Float maxScore;
	/** @see org.olat.course.nodes.MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD */
	private Boolean isPassingGranted;
	/** @see org.olat.course.nodes.MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE */
	private Float passingScoreThreshold;
	/** @see org.olat.course.nodes.MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD */
	private Boolean hasCommentField;
	/** @see org.olat.course.nodes.MSCourseNode.CONFIG_KEY_INFOTEXT_USER */
	private String commentForUser;
	/** @see org.olat.course.nodes.MSCourseNode.CONFIG_KEY_INFOTEXT_COACH */
	private String commentForCoaches;

	/** @see org.olat.course.nodes.TACourseNode.CONF_SOLUTION_ENABLED */
	private Boolean isSolutionEnabled;

	/** @see org.olat.course.nodes.TACourseNode.ACCESS_TASK */
	private String conditionTask;
	/** @see org.olat.course.nodes.TACourseNode.ACCESS_DROPBOX */
	private String conditionDropbox;
	/** @see org.olat.course.nodes.TACourseNode.ACCESS_RETURNBOX */
	private String conditionReturnbox;
	/** @see org.olat.course.nodes.TACourseNode.ACCESS_SCORING */
	private String conditionScoring;
	/** @see org.olat.course.nodes.TACourseNode.ACCESS_SOLUTION */
	private String conditionSolution;

	public TaskConfigVO() {
		// make JAXB happy
	}

	public Boolean getIsAssignmentEnabled() {
		return isAssignmentEnabled;
	}

	public void setIsAssignmentEnabled(final Boolean isAssignmentEnabled) {
		this.isAssignmentEnabled = isAssignmentEnabled;
	}

	public String getTaskAssignmentType() {
		return taskAssignmentType;
	}

	public void setTaskAssignmentType(final String taskAssignmentType) {
		this.taskAssignmentType = taskAssignmentType;
	}

	public String getTaskAssignmentText() {
		return taskAssignmentText;
	}

	public void setTaskAssignmentText(final String taskAssignmentText) {
		this.taskAssignmentText = taskAssignmentText;
	}

	public Boolean getIsTaskPreviewEnabled() {
		return isTaskPreviewEnabled;
	}

	public void setIsTaskPreviewEnabled(final Boolean isTaskPreviewEnabled) {
		this.isTaskPreviewEnabled = isTaskPreviewEnabled;
	}

	public Boolean getIsTaskDeselectEnabled() {
		return isTaskDeselectEnabled;
	}

	public void setIsTaskDeselectEnabled(final Boolean isTaskDeselectEnabled) {
		this.isTaskDeselectEnabled = isTaskDeselectEnabled;
	}

	public Boolean getOnlyOneUserPerTask() {
		return onlyOneUserPerTask;
	}

	public void setOnlyOneUserPerTask(final Boolean onlyOneUserPerTask) {
		this.onlyOneUserPerTask = onlyOneUserPerTask;
	}

	public Boolean getIsDropboxEnabled() {
		return isDropboxEnabled;
	}

	public void setIsDropboxEnabled(final Boolean isDropboxEnabled) {
		this.isDropboxEnabled = isDropboxEnabled;
	}

	public Boolean getIsDropboxConfirmationMailEnabled() {
		return isDropboxConfirmationMailEnabled;
	}

	public void setIsDropboxConfirmationMailEnabled(final Boolean isDropboxConfirmationMailEnabled) {
		this.isDropboxConfirmationMailEnabled = isDropboxConfirmationMailEnabled;
	}

	public String getDropboxConfirmationText() {
		return dropboxConfirmationText;
	}

	public void setDropboxConfirmationText(final String dropboxConfirmationText) {
		this.dropboxConfirmationText = dropboxConfirmationText;
	}

	public Boolean getIsReturnboxEnabled() {
		return isReturnboxEnabled;
	}

	public void setIsReturnboxEnabled(final Boolean isReturnboxEnabled) {
		this.isReturnboxEnabled = isReturnboxEnabled;
	}

	public Boolean getIsScoringEnabled() {
		return isScoringEnabled;
	}

	public void setIsScoringEnabled(final Boolean isScoringEnabled) {
		this.isScoringEnabled = isScoringEnabled;
	}

	public Boolean getIsScoringGranted() {
		return isScoringGranted;
	}

	public void setIsScoringGranted(final Boolean isScoringGranted) {
		this.isScoringGranted = isScoringGranted;
	}

	public Float getMinScore() {
		return minScore;
	}

	public void setMinScore(final Float minScore) {
		this.minScore = minScore;
	}

	public Float getMaxScore() {
		return maxScore;
	}

	public void setMaxScore(final Float maxScore) {
		this.maxScore = maxScore;
	}

	public Boolean getIsPassingGranted() {
		return isPassingGranted;
	}

	public void setIsPassingGranted(final Boolean isPassingGranted) {
		this.isPassingGranted = isPassingGranted;
	}

	public Float getPassingScoreThreshold() {
		return passingScoreThreshold;
	}

	public void setPassingScoreThreshold(final Float passingScoreThreshold) {
		this.passingScoreThreshold = passingScoreThreshold;
	}

	public Boolean getHasCommentField() {
		return hasCommentField;
	}

	public void setHasCommentField(final Boolean hasCommentField) {
		this.hasCommentField = hasCommentField;
	}

	public String getCommentForUser() {
		return commentForUser;
	}

	public void setCommentForUser(final String commentForUser) {
		this.commentForUser = commentForUser;
	}

	public String getCommentForCoaches() {
		return commentForCoaches;
	}

	public void setCommentForCoaches(final String commentForCoaches) {
		this.commentForCoaches = commentForCoaches;
	}

	public Boolean getIsSolutionEnabled() {
		return isSolutionEnabled;
	}

	public void setIsSolutionEnabled(final Boolean isSolutionEnabled) {
		this.isSolutionEnabled = isSolutionEnabled;
	}

	public String getConditionTask() {
		return conditionTask;
	}

	public void setConditionTask(final String conditionTask) {
		this.conditionTask = conditionTask;
	}

	public String getConditionDropbox() {
		return conditionDropbox;
	}

	public void setConditionDropbox(final String conditionDropbox) {
		this.conditionDropbox = conditionDropbox;
	}

	public String getConditionReturnbox() {
		return conditionReturnbox;
	}

	public void setConditionReturnbox(final String conditionReturnbox) {
		this.conditionReturnbox = conditionReturnbox;
	}

	public String getConditionScoring() {
		return conditionScoring;
	}

	public void setConditionScoring(final String conditionScoring) {
		this.conditionScoring = conditionScoring;
	}

	public String getConditionSolution() {
		return conditionSolution;
	}

	public void setConditionSolution(final String conditionSolution) {
		this.conditionSolution = conditionSolution;
	}

}
