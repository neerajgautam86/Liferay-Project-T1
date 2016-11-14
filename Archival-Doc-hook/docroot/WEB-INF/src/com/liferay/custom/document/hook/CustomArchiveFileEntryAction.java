package com.liferay.custom.document.hook;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

public class CustomArchiveFileEntryAction extends BaseStrutsPortletAction {
	
	private static Log LOGGER = LogFactoryUtil.getLog(ArchiveFileEntryAction.class);

	@Override
	public String render(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig, RenderRequest renderRequest,
			RenderResponse renderResponse) throws Exception {
		
		return "/portlet/document_library/archive_doc.jsp";

	}

}
