package com.liferay.custom.document.hook;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFileEntryMetadata;
import com.liferay.portlet.documentlibrary.model.DLFileEntryType;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;
import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryMetadataLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryTypeLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileVersionLocalServiceUtil;
import com.liferay.portlet.dynamicdatamapping.model.DDMStructure;
import com.liferay.portlet.dynamicdatamapping.storage.Field;
import com.liferay.portlet.dynamicdatamapping.storage.Fields;
import com.liferay.portlet.dynamicdatamapping.storage.StorageEngineUtil;

import java.util.Iterator;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

public class ArchiveFileEntryAction extends BaseStrutsPortletAction {

	private static Log LOGGER = LogFactoryUtil.getLog(ArchiveFileEntryAction.class);

	@Override
	public String render(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig, RenderRequest renderRequest,
			RenderResponse renderResponse) throws Exception {

		return originalStrutsPortletAction.render(null, portletConfig, renderRequest, renderResponse);		
	}

	@Override
	public void processAction(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse) throws Exception {

		LOGGER.info("Archive Custom Action class is called for processAction.");

		long[] fileEntryIds = StringUtil.split(ParamUtil.getString(actionRequest, "fileEntryIds"), 0L);
		String cmd = ParamUtil.getString(actionRequest, "cmd");
		if (cmd.equals("restore_archive")) {

			for (int i = 0; i < fileEntryIds.length; i++) {
				long fileEntryId = fileEntryIds[i];
				DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getFileEntry(fileEntryId);

				//all file versions 
				List<DLFileVersion> dlFileVersions = dlFileEntry.getFileVersions(-1);

				//latest fileVersion
				String latestDLFileVerion = dlFileEntry.getVersion();

				for (DLFileVersion updateFileVersion : dlFileVersions) {
					if (updateFileVersion.getVersion().equals(latestDLFileVerion)) {
						LOGGER.info(dlFileEntry.getFileVersion().getStatus());
						updateFileVersion.setStatus(WorkflowConstants.STATUS_APPROVED);
						DLFileVersionLocalServiceUtil.updateDLFileVersion(updateFileVersion);
						LOGGER.info(dlFileEntry.getFileVersion().getStatus());
					}
				}

				//update expiry date field.
				DLFileEntryType dLFileEntryType = DLFileEntryTypeLocalServiceUtil.getDLFileEntryType(dlFileEntry.getFileEntryTypeId());

				List<DDMStructure> structures = dLFileEntryType.getDDMStructures();
				innerForLoop: for (DDMStructure struct : structures) {
					DLFileEntryMetadata dlFileEntryMetadata = DLFileEntryMetadataLocalServiceUtil.getFileEntryMetadata(
							struct.getStructureId(), dlFileEntry.getFileVersion().getFileVersionId());
					Fields fields = StorageEngineUtil.getFields(dlFileEntryMetadata.getDDMStorageId());
					Iterator<Field> iField = fields.iterator();
					while (iField.hasNext()) {
						Field field = iField.next();

						String expiryDateLabel = field.getDDMStructure().getFieldLabel(field.getName(), field.getDefaultLocale());
						if (expiryDateLabel.equals(PropsUtil.get("expiry.custom.field.name"))) {
							LOGGER.info(field.getValue().toString());
							field.setValue("");
							break innerForLoop;
						}
					}
				}
			}

		} 
		/*else if (cmd.equals("delete")) {

			for (int i = 0; i < fileEntryIds.length; i++) {
				long deleteFileEntryId = fileEntryIds[i];
				DLAppServiceUtil.deleteFileEntry(deleteFileEntryId);
			}
		}*/

		originalStrutsPortletAction.processAction(originalStrutsPortletAction, portletConfig, actionRequest, actionResponse);
	}
}
