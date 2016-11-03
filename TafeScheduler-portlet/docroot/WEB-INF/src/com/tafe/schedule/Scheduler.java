package com.tafe.schedule;

import com.liferay.mail.service.MailServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.mail.MailMessage;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroupRole;
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
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryMetadataLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryTypeLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.dynamicdatamapping.model.DDMStructure;
import com.liferay.portlet.dynamicdatamapping.storage.Field;
import com.liferay.portlet.dynamicdatamapping.storage.Fields;
import com.liferay.portlet.dynamicdatamapping.storage.StorageEngineUtil;
import com.liferay.util.ContentUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class Scheduler implements MessageListener {

	@Override
	public void receive(Message arg0) throws MessageListenerException {
		try {
			Long parentFolderId = DLFolderConstants.DEFAULT_PARENT_FOLDER_ID;
			Long companyId = PortalUtil.getDefaultCompanyId();

			// code to resolve permission checker issue in scheduler
			Role adminRole = RoleLocalServiceUtil.getRole(companyId, "Administrator");
			List<User> adminUsers = UserLocalServiceUtil.getRoleUsers(adminRole.getRoleId());

			PrincipalThreadLocal.setName(adminUsers.get(0).getUserId());
			PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(adminUsers.get(0), true);
			PermissionThreadLocal.setPermissionChecker(permissionChecker);

			//TODO: Change this to TAFE site name.
			Group group = GroupLocalServiceUtil.getGroup(companyId, "Guest");

			/*
			 * List<Folder> lFolder =
			 * DLAppServiceUtil.getFolders(group.getGroupId(), parentFolderId);
			 * List<DLFolder> folders =
			 * DLFolderLocalServiceUtil.getFolders(group.getGroupId(),
			 * parentFolderId);
			 */

			List<DLFolder> dlFolders = DLFolderLocalServiceUtil.getDLFolders(0, DLFolderLocalServiceUtil.getDLFoldersCount() - 1);
			List<DLFileEntry> fileEntryService = null;

			// DLFolder defaultFolder = DLFolderLocalServiceUtil.getFolder(group.getGroupId(), parentFolderId, "Home");
			List<DLFileEntry> fileEntryService1 = DLFileEntryLocalServiceUtil.getFileEntries(group.getGroupId(), parentFolderId, -1, -1,
					null);
			for (DLFileEntry fileEntryObj : fileEntryService1) {
				System.out.println(fileEntryObj.getTitle());
				System.out.println(fileEntryObj.getFileVersion());
				String expiryDateString = null;

				long fileEntryTypeId = fileEntryObj.getFileEntryTypeId();

				//check whether document contains any documentType or not.
				//This implies there is expiry date for given document and hence document can expires.
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
							if (expiryDateLabel.equals("Expiration Date")) {
								expiryDateString = field.getValue().toString();
								break innerForLoop;
							}
						}
					}

					if (null != expiryDateString) {
						SimpleDateFormat sdf = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
						SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
						//Date expiryDate = (Date) formatter.parse(expiryDateString);

						//check if expiry date is equal or less than 15 days from current date, then send mail.
						Date currentDate = new Date(System.currentTimeMillis());
						Date expiryDate = sdf.parse(expiryDateString);

						long diff = currentDate.getTime() - expiryDate.getTime();
						long diffDays = diff / (24 * 60 * 60 * 1000);
						if (diffDays <= 15) {
							String docOwnerEmail = UserLocalServiceUtil.getUser(fileEntryObj.getUserId()).getEmailAddress();
							List<User> siteUsers = UserLocalServiceUtil.getGroupUsers(group.getGroupId());

							String siteAdminEmail = null;

							//find the admin user
							usersLoop: for (User u : siteUsers) {
								List<UserGroupRole> userGroupRoles = UserGroupRoleLocalServiceUtil.getUserGroupRoles(u.getUserId());
								for (UserGroupRole ugRole : userGroupRoles) {
									if (ugRole.getRole().getName().equals("Site Administrator")) {
										siteAdminEmail = UserLocalServiceUtil.getUser(u.getUserId()).getEmailAddress();
										break usersLoop;
									}
								}
							}

							sendMailWithPlainText(docOwnerEmail, siteAdminEmail, companyId, fileEntryObj.getTitle(),
									dLFileEntryType.getName());
						}
					}
				}
			}

		} catch (SystemException e) {
			e.printStackTrace();
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendMailWithPlainText(String docOwnerEmail, String siteAdminEmail, Long companyId, String documentTitle, String documentType)
			throws PortalException, SystemException {

		InternetAddress fromAddress = null;
		InternetAddress toAddress = null;

		String doOwnerName = UserLocalServiceUtil.getUserByEmailAddress(companyId, docOwnerEmail).getFullName();
		String siteAdminName = UserLocalServiceUtil.getUserByEmailAddress(companyId, siteAdminEmail).getFullName();

		//Now change email template values.
		String body = ContentUtil.get("/content/email.tmpl", true);
		body = StringUtil.replace(body, new String[] { "[$TO_NAME$]", "[$PORTLET_NAME$]", "[$DOCUMENT_TYPE$]", "[$DOCUMENT_TITLE$]",
				"[$FROM_NAME$]", "[$FROM_ADDRESS$]" }, new String[] { doOwnerName, "Documents and Media", documentType, documentTitle,
				siteAdminName, siteAdminEmail });

		String subject = "Exipration Notification of [$DOCUMENT_TYPE$]: [$DOCUMENT_TITLE$]";
		subject = StringUtil.replace(subject, new String[] { "[$DOCUMENT_TYPE$]", "[$DOCUMENT_TITLE$]" }, new String[] { documentType,
				documentTitle });

		try {
			fromAddress = new InternetAddress(docOwnerEmail);
			toAddress = new InternetAddress(siteAdminEmail);
			MailMessage mailMessage = new MailMessage();
			mailMessage.setTo(toAddress);
			mailMessage.setFrom(fromAddress);
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
