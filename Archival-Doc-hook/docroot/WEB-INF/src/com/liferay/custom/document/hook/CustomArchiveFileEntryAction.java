package com.liferay.custom.document.hook;

import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

public class CustomArchiveFileEntryAction extends BaseStrutsPortletAction  {

	/*public String execute(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		return "/portlet/document_library/archive_doc.jsp";
	}*/
	
	@Override
	public String render(
            StrutsPortletAction originalStrutsPortletAction,
            PortletConfig portletConfig, RenderRequest renderRequest,
            RenderResponse renderResponse)
        throws Exception {

        System.out.println("Custom Struts Action render method");

        return "/portlet/document_library/archive_doc.jsp";

    }

}
