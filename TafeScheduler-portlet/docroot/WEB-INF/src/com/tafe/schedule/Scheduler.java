package com.tafe.schedule;

import com.liferay.mail.service.MailServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.mail.MailMessage;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFileEntryMetadata;
import com.liferay.portlet.documentlibrary.model.DLFileEntryType;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryMetadataLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryTypeLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileVersionLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.dynamicdatamapping.model.DDMStructure;
import com.liferay.portlet.dynamicdatamapping.storage.Field;
import com.liferay.portlet.dynamicdatamapping.storage.Fields;
import com.liferay.portlet.dynamicdatamapping.storage.StorageEngineUtil;
import com.liferay.util.ContentUtil;
import com.liferay.util.portlet.PortletProps;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class Scheduler implements MessageListener {

	private static Log LOGGER = LogFactoryUtil.getLog(Scheduler.class);

	@Override
	public void receive(Message arg0) throws MessageListenerException {

		try {
			Long parentFolderId = DLFolderConstants.DEFAULT_PARENT_FOLDER_ID;
			Long companyId = PortalUtil.getDefaultCompanyId();

			// code to resolve permission checker issue in scheduler
			Role adminRole = RoleLocalServiceUtil.getRole(companyId, PortletProps.get("role.name"));
			List<User> adminUsers = UserLocalServiceUtil.getRoleUsers(adminRole.getRoleId());

			PrincipalThreadLocal.setName(adminUsers.get(0).getUserId());
			PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(adminUsers.get(0), true);
			PermissionThreadLocal.setPermissionChecker(permissionChecker);

			// get all live groups and for each site check for document and media library.
			List<Group> liveGroups = GroupLocalServiceUtil.getCompanyGroups(companyId, 0,
					GroupLocalServiceUtil.getCompanyGroupsCount(companyId));
			for (Group group : liveGroups) {
				Long groupId = group.getGroupId();

				List<DLFolder> dlFolders = DLFolderLocalServiceUtil.getFolders(group.getGroupId(), parentFolderId);

				// Check all the documents under all the folders.
				if (!dlFolders.isEmpty()) {
					for (DLFolder folder : dlFolders) {
						List<DLFileEntry> dlFileEntries = DLFileEntryLocalServiceUtil.getFileEntries(groupId, folder.getFolderId(), -1, -1,
								null);
						if (!dlFileEntries.isEmpty()) {
							getDLFileEntries(companyId, groupId, dlFileEntries);
						}
					}
				}

				//Now scan all documents under by default folder of DM portlet i.e. Home
				List<DLFileEntry> dlFileEntries = DLFileEntryLocalServiceUtil.getFileEntries(groupId, parentFolderId, -1, -1, null);
				if (!dlFileEntries.isEmpty()) {
					getDLFileEntries(companyId, groupId, dlFileEntries);
				}
			}

		} catch (SystemException e) {
			LOGGER.error("System Exception is thrown while running scheduler.");
		} catch (PortalException e) {
			LOGGER.error("Portal Exception is thrown while running scheduler.");
		} catch (ParseException e) {
			LOGGER.error("Parsing Exception is thrown while running scheduler.");
		} catch (Exception e) {
			LOGGER.error("Exception is thrown while running scheduler.");
		}
	}

	/**
	 * Method to iterate through all documents in a given folder. Check the
	 * calling method for the same.
	 * 
	 * @param companyId
	 * @param groupId
	 * @param dlFileEntries
	 * @throws PortalException
	 * @throws SystemException
	 * @throws ParseException
	 */
	private void getDLFileEntries(Long companyId, Long groupId, List<DLFileEntry> dlFileEntries) throws PortalException, SystemException,
			ParseException {

		for (DLFileEntry fileEntryObj : dlFileEntries) {
			String expiryDateString = null;
			LOGGER.info("Doc file Name=" + fileEntryObj.getTitle());
			long fileEntryTypeId = fileEntryObj.getFileEntryTypeId();

			// check whether document contains any documentType or not.
			// This implies there is expiry date for given document and
			// hence document can expires.
			if (fileEntryTypeId != 0) {
				DLFileEntryType dLFileEntryType = DLFileEntryTypeLocalServiceUtil.getDLFileEntryType(fileEntryTypeId);

				List<DDMStructure> structures = dLFileEntryType.getDDMStructures();
				innerForLoop: for (DDMStructure struct : structures) {
					DLFileEntryMetadata dlFileEntryMetadata = DLFileEntryMetadataLocalServiceUtil.getFileEntryMetadata(
							struct.getStructureId(), fileEntryObj.getFileVersion().getFileVersionId());
					Fields fields = StorageEngineUtil.getFields(dlFileEntryMetadata.getDDMStorageId());
					Iterator<Field> iField = fields.iterator();
					while (iField.hasNext()) {
						Field field = iField.next();

						String expiryDateLabel = field.getDDMStructure().getFieldLabel(field.getName(), field.getDefaultLocale());
						if (expiryDateLabel.equals(PortletProps.get("expiry.custom.field.name"))) {
							expiryDateString = field.getValue().toString();
							break innerForLoop;
						}
					}
				}

				if (Validator.isNotNull(expiryDateString)) {
					SimpleDateFormat sdf = new SimpleDateFormat(PortletProps.get("format.date"), Locale.ENGLISH);

					// check if expiry date is equal or less than 15 days from current date, then send mail.
					Date currentDate = new Date(System.currentTimeMillis());
					Date expiryDate = sdf.parse(expiryDateString);

					long diff = expiryDate.getTime() - currentDate.getTime();
					long diffDays = diff / (24 * 60 * 60 * 1000);
					LOGGER.info("Diff in no of days=" + diffDays);

					//update the status of the document to archive
					if (expiryDate.before(currentDate) && fileEntryObj.getFileVersion().getStatus() != WorkflowConstants.STATUS_EXPIRED) {

						//all file versions 
						List<DLFileVersion> dlFileVersions = fileEntryObj.getFileVersions(-1);

						//latest fileVersion
						String latestDLFileVerion = fileEntryObj.getVersion();

						for (DLFileVersion updateFileVersion : dlFileVersions) {
							if (updateFileVersion.getVersion().equals(latestDLFileVerion)) {
								updateFileVersion.setStatus(WorkflowConstants.STATUS_EXPIRED);
								DLFileVersionLocalServiceUtil.updateDLFileVersion(updateFileVersion);
								LOGGER.info(fileEntryObj.getFileVersion().getStatus());
							}
						}
					}

					//send email only on 15th day irrespective of scheduler being running.
					if (expiryDate.after(currentDate) && diffDays > 0
							&& diffDays == Integer.parseInt(PortletProps.get("expiry.no.of.days"))) {

						String docOwnerEmail = UserLocalServiceUtil.getUser(fileEntryObj.getUserId()).getEmailAddress();
						List<User> siteUsers = UserLocalServiceUtil.getGroupUsers(groupId);

						String siteAdminEmail = null;

						// find the site admin user
						usersLoop: for (User u : siteUsers) {
							if (UserGroupRoleLocalServiceUtil.hasUserGroupRole(u.getUserId(), groupId, RoleConstants.SITE_ADMINISTRATOR)) {
								siteAdminEmail = UserLocalServiceUtil.getUser(u.getUserId()).getEmailAddress();
								break usersLoop;
							}
						}

						if (Validator.isNotNull(siteAdminEmail)) {
							LOGGER.info("Mail sent for doc with title=" + fileEntryObj.getTitle());
							sendMailWithPlainText(docOwnerEmail, siteAdminEmail, companyId, fileEntryObj.getTitle(),
									dLFileEntryType.getName());
						}
					}
				}
			}
		}

	}

	public void sendMailWithPlainText(String docOwnerEmail, String siteAdminEmail, Long companyId, String documentTitle, String documentType)
			throws PortalException, SystemException {

		InternetAddress fromAddress = null;
		InternetAddress toAddress = null;

		String docOwnerName = UserLocalServiceUtil.getUserByEmailAddress(companyId, docOwnerEmail).getFullName();
		String siteAdminName = UserLocalServiceUtil.getUserByEmailAddress(companyId, siteAdminEmail).getFullName();

		// Now change email template values.
		String body = ContentUtil.get("/content/email.tmpl", true);
		body = StringUtil.replace(body, new String[] { "[$TO_NAME$]", "[$PORTLET_NAME$]", "[$DOCUMENT_TYPE$]", "[$DOCUMENT_TITLE$]",
				"[$FROM_NAME$]", "[$FROM_ADDRESS$]" }, new String[] { docOwnerName, "Documents and Media", documentType, documentTitle,
				siteAdminName, siteAdminEmail });

		String subject = "Exipration Notification of [$DOCUMENT_TYPE$]: [$DOCUMENT_TITLE$]";
		subject = StringUtil.replace(subject, new String[] { "[$DOCUMENT_TYPE$]", "[$DOCUMENT_TITLE$]" }, new String[] { documentType,
				documentTitle });

		try {
			fromAddress = new InternetAddress(siteAdminEmail);
			toAddress = new InternetAddress(docOwnerEmail);
			MailMessage mailMessage = new MailMessage();
			mailMessage.setTo(toAddress);
			mailMessage.setFrom(fromAddress);
			mailMessage.setCC(fromAddress);
			mailMessage.setSubject(subject);
			mailMessage.setBody(body);
			mailMessage.setHTMLFormat(true);
			MailServiceUtil.sendEmail(mailMessage);
			System.out.println("Send mail with Plain Text");
		} catch (AddressException e) {
			e.printStackTrace();
		}
	}

}
