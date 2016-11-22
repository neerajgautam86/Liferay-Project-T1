package com.liferay.custom.document.hook;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.kernel.util.ParamUtil;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

public class ArchiveViewAction extends BaseStrutsPortletAction {

	private static Log LOGGER = LogFactoryUtil.getLog(ArchiveViewAction.class);

	@Override
	public String render(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig, RenderRequest renderRequest,
			RenderResponse renderResponse) throws Exception {

		return originalStrutsPortletAction.render(null, portletConfig, renderRequest, renderResponse);
	}

	@Override
	public void serveResource(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig,
			ResourceRequest resourceRequest, ResourceResponse resourceResponse) throws Exception {

		boolean viewArchivalEntries = ParamUtil.getBoolean(resourceRequest, "viewArchivalEntries");

		if (viewArchivalEntries) {

			PortletContext portletContext = portletConfig.getPortletContext();

			PortletRequestDispatcher portletRequestDispatcher = portletContext
					.getRequestDispatcher("/html/portlet/document_library/archive_view.jsp");

			portletRequestDispatcher.include(resourceRequest, resourceResponse);
		}

		originalStrutsPortletAction.serveResource(originalStrutsPortletAction, portletConfig, resourceRequest, resourceResponse);
	}
}
