<%--
/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */
--%>

<%@ include file="/html/portlet/document_library/init.jsp" %>

<%
String strutsAction = ParamUtil.getString(request, "struts_action");

Folder folder = (Folder)request.getAttribute("view.jsp-folder");

long folderId = GetterUtil.getLong((String)request.getAttribute("view.jsp-folderId"));

long repositoryId = GetterUtil.getLong((String)request.getAttribute("view.jsp-repositoryId"));

Group scopeGroup = themeDisplay.getScopeGroup();
%>

<aui:nav-bar>
	<%-- <aui:nav collapsible="<%= false %>" cssClass="nav-display-style-buttons pull-right" id="displayStyleButtons">
		<aui:nav-item>
			<span class="pull-left display-style-buttons-container" id="<portlet:namespace />displayStyleButtonsContainer">
				<c:if test='<%= !strutsAction.equals("/document_library/search") %>'>
					<liferay-util:include page="/html/portlet/document_library/display_style_buttons.jsp" />
				</c:if>
			</span>
		</aui:nav-item>
	</aui:nav> --%>

	<aui:nav id="toolbarContainer">
		<aui:nav-item cssClass="hide" dropdown="<%= true %>" id="actionsButtonContainer" label="actions">			
			<%
			String taglibURL = "javascript:Liferay.fire('" + renderResponse.getNamespace() + "restore_archive_doc'); void(0);";
			%>

			<aui:nav-item href="<%= taglibURL %>" iconCssClass="icon-move" label="restore-archival" />
			
			<%
			taglibURL = "javascript:" + renderResponse.getNamespace() + "deleteEntries();";
			%>

			<aui:nav-item href="<%= taglibURL %>" iconCssClass="icon-remove" id="deleteAction" label="delete" />
		</aui:nav-item>
	</aui:nav>
	
	<aui:nav>
		<aui:input type="checkbox" value="3m" href="#" class="interactive checked" label="3 months" name="3 months" onClick="refreshPage1();" />
		<aui:input type="checkbox" value="6m" href="#" class="interactive" label="6 months" name="6 months" />
		<aui:input type="checkbox" value="1y" href="#" class="interactive" label="1 year" name="1 year" />
	</aui:nav>

</aui:nav-bar>

<aui:script>
	function <portlet:namespace />deleteEntries() {
		if (confirm('<%= UnicodeLanguageUtil.get(pageContext, "are-you-sure-you-want-to-delete-the-selected-entries") %>')) {
			Liferay.fire(
				'<%= renderResponse.getNamespace() %>editEntry',
				{
					action: '<%= Constants.DELETE %>'
				}
			);
		}
	}
</aui:script>

<script>
	function refreshPage1(){
		/* alert("Wow"); */
		jQuery("#<portlet:namespace />entriesContainer").load(location.href + " #<portlet:namespace />entriesContainer");
	}
</script>