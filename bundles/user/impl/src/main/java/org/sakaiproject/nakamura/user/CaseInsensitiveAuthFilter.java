package org.sakaiproject.nakamura.user;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.user.UserFinder;
import org.sakaiproject.nakamura.formauth.FormLoginServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

@Component(metatype = true)
public class CaseInsensitiveAuthFilter implements Filter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CaseInsensitiveAuthFilter.class);

	/**
	 * Priority of this filter, higher number means sooner
	 */
	@Property(intValue = 8)
	protected static final String FILTER_PRIORITY_CONF = "filter.priority";

	@Property(boolValue = false)
	protected static final String EMAIL_AUTH_CONF = "authenticate.with.email";
	protected Boolean authenticateWithEmail = false;

	@Reference
	protected ExtHttpService extHttpService;

	@Reference
	protected UserFinder userFinder;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hrequest = (HttpServletRequest) request;

		if ("POST".equals(hrequest.getMethod())) {
			// Case Insensitive lookup during login
			if ("/system/sling/formlogin".equals(hrequest.getRequestURI())) {
				String userId = (String)hrequest.getParameter((FormLoginServlet.USERNAME));
				if (userId != null) {
					try {
						Set<String> results = getValidUserId(userId);
						if (results.size() == 1){
							// Wrap the request so it looks like the user entered the proper case
							hrequest = new OverriddenRequest(hrequest,
									ImmutableMap.of(FormLoginServlet.USERNAME,
											new String[] { results.iterator().next(), }));
						}
					} catch (Exception e) {
						LOGGER.error("An error occurred while looking up " + userId, e);
					}
				}
			}

			// Add the lowercased username as the nameLower property
			if ("/system/userManager/user.create.html".equals(hrequest.getRequestURI())) {
				String name = (String)hrequest.getParameter(":name");
				if (name != null) {
					hrequest = new OverriddenRequest(hrequest,
							ImmutableMap.of("nameLower", new String[] { name.toLowerCase(), }));
				}
			}
		}
		chain.doFilter(hrequest, response);
	}

	protected Set<String> getValidUserId(String userId) throws Exception {
		Set<String> results = userFinder.findUsersByName(userId);
		// try to find the user by case insensitive email address search
		if (results.isEmpty() && authenticateWithEmail){
			results = userFinder.findUsersByEmail(userId);
		}
		return results;
	}

	@Activate
	protected void activate(Map<?,?> props)
			throws ServletException {
		authenticateWithEmail = PropertiesUtil.toBoolean(props.get(EMAIL_AUTH_CONF), false);
		int filterPriority = PropertiesUtil.toInteger(props.get(FILTER_PRIORITY_CONF), 8);
		extHttpService.registerFilter(this, ".*", null, filterPriority, null);
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		extHttpService.unregisterFilter(this);
	}

	@Override
	public void destroy() { }

	@Override
	public void init(FilterConfig filterConfig) throws ServletException { }
}