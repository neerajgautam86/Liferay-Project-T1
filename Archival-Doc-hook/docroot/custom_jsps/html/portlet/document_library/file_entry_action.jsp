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

<%@ page import="com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil" %>

<%
ResultRow row = (ResultRow)request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);

FileEntry fileEntry = null;
DLFileShortcut fileShortcut = null;

boolean showWhenSingleIcon = false;

if (portletId.equals(PortletKeys.DOCUMENT_LIBRARY)) {
	showWhenSingleIcon = true;
}

if (row != null) {
	Object result = row.getObject();

	if (result instanceof AssetEntry) {
		AssetEntry assetEntry = (AssetEntry)result;

		if (assetEntry.getClassName().equals(DLFileEntryConstants.getClassName())) {
			fileEntry = DLAppLocalServiceUtil.getFileEntry(assetEntry.getClassPK());
		}
		else {
			fileShortcut = DLAppLocalServiceUtil.getFileShortcut(assetEntry.getClassPK());
		}
	}
	else if (result instanceof FileEntry) {
		fileEntry = (FileEntry)result;
	}
	else if (result instanceof TrashEntry) {
		TrashEntry trashEntry = (TrashEntry)result;

		String className = trashEntry.getClassName();

		if (className.equals(DLFileEntryConstants.getClassName())) {
			fileEntry = DLAppLocalServiceUtil.getFileEntry(trashEntry.getClassPK());
		}
		else {
			fileShortcut = DLAppLocalServiceUtil.getFileShortcut(trashEntry.getClassPK());
		}
	}
	else {
		fileShortcut = (DLFileShortcut)result;
	}
}
else {
	if (portletName.equals(PortletKeys.DOCUMENT_LIBRARY_DISPLAY)) {
		if (request.getAttribute("view_file_entry.jsp-fileEntry") != null) {
			fileEntry = (FileEntry)request.getAttribute("view_file_entry.jsp-fileEntry");

			if (request.getAttribute("view_file_entry.jsp-fileShortcut") != null) {
				fileShortcut = (DLFileShortcut)request.getAttribute("view_file_entry.jsp-fileShortcut");
			}
		}
		else {
			fileShortcut = (DLFileShortcut)request.getAttribute("view_file_shortcut.jsp-fileShortcut");
		}
	}
	else {
		if (request.getAttribute("view_entries.jsp-fileEntry") != null) {
			fileEntry = (FileEntry)request.getAttribute("view_entries.jsp-fileEntry");

			if (request.getAttribute("view_entries.jsp-fileShortcut") != null) {
				fileShortcut = (DLFileShortcut)request.getAttribute("view_entries.jsp-fileShortcut");
			}
		}
		else {
			fileShortcut = (DLFileShortcut)request.getAttribute("view_file_shortcut.jsp-fileShortcut");
		}
	}
}

long folderId = 0;

if (fileShortcut != null) {
	folderId = fileShortcut.getFolderId();

	fileEntry = DLAppLocalServiceUtil.getFileEntry(fileShortcut.getToFileEntryId());
}
else if (fileEntry != null) {
	folderId = fileEntry.getFolderId();
}

boolean checkedOut = fileEntry.isCheckedOut();
boolean hasLock = fileEntry.hasLock();
boolean restore = false;

PortletURL viewFolderURL = liferayPortletResponse.createRenderURL();

viewFolderURL.setParameter("struts_action", "/document_library/view");
viewFolderURL.setParameter("folderId", String.valueOf(folderId));

if (fileShortcut != null) {
	fileEntry = DLAppLocalServiceUtil.getFileEntry(fileShortcut.getToFileEntryId());
}

DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry(fileEntry.getFileEntryId());

//all file versions 
List<DLFileVersion> dlFileVersions = dlFileEntry.getFileVersions(-1);

//latest fileVersion
String latestDLFileVerion = fileEntry.getVersion();

boolean isExpiredDocument = false;

for (DLFileVersion updateFileVersion : dlFileVersions) {
	if (updateFileVersion.getVersion().equals(latestDLFileVerion) && updateFileVersion.getStatus() == WorkflowConstants.STATUS_EXPIRED) {
		isExpiredDocument = true;
	}
}

%>

<liferay-util:buffer var="iconMenu">
	<liferay-ui:icon-menu direction='<%= showMinimalActionButtons ? "down" : "left" %>' extended="<%= showMinimalActionButtons ? false : true %>" icon="<%= showMinimalActionButtons ? StringPool.BLANK : null %>" message='<%= showMinimalActionButtons ? StringPool.BLANK : "actions" %>' showExpanded="<%= false %>" showWhenSingleIcon="<%= showWhenSingleIcon %>" triggerCssClass="btn">
		
		<c:choose>
			<c:when test="<%= !isExpiredDocument %>" >
				<%@ include file="/html/portlet/document_library/action/download.jspf" %>
				<%@ include file="/html/portlet/document_library/action/open_document.jspf" %>
				<%@ include file="/html/portlet/document_library/action/view_original.jspf" %>
				<%@ include file="/html/portlet/document_library/action/edit.jspf" %>
				<%@ include file="/html/portlet/document_library/action/move.jspf" %>
				<%@ include file="/html/portlet/document_library/action/lock.jspf" %>
				<%@ include file="/html/portlet/document_library/action/permissions.jspf" %>
				<%@ include file="/html/portlet/document_library/action/delete.jspf" %>
			</c:when>
			<c:otherwise>
				<%@ include file="/html/portlet/document_library/action/edit.jspf" %>
				<%@ include file="/html/portlet/document_library/action/restore_archival.jspf" %>
				<%@ include file="/html/portlet/document_library/action/delete.jspf" %>
			</c:otherwise>
		</c:choose>
	</liferay-ui:icon-menu>
</liferay-util:buffer>

<c:choose>
	<c:when test="<%= portletName.equals(PortletKeys.DOCUMENT_LIBRARY_DISPLAY) && !showMinimalActionButtons %>">

		<%= iconMenu %>

	</c:when>
	<c:otherwise>
		<span class="entry-action overlay">

			<%= iconMenu %>

		</span>
	</c:otherwise>
</c:choose>