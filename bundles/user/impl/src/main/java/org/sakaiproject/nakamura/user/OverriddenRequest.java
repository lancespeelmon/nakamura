package org.sakaiproject.nakamura.user;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class OverriddenRequest extends HttpServletRequestWrapper {

	private Map<String,String[]> modifiedMap;

	@SuppressWarnings("unchecked")
	public OverriddenRequest(HttpServletRequest request, Map<String,String[]> overrides) {
		super(request);
		modifiedMap = new HashMap<String, String[]>();
		modifiedMap.putAll((Map<String, String[]>)super.getParameterMap());
		modifiedMap.putAll(overrides);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		/**
		 * Deep down in the sling authentication stack the request.parameterMap is
		 * copied and used to get the parameters.
		 */
		return modifiedMap;
	}

	@Override
	public String getParameter(String parameter) {
		String[] value = modifiedMap.get(parameter);
		if (value != null){
			return value[0];
		}
		return null;
	}
}