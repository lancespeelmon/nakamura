package org.sakaiproject.nakamura.user;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;


@RunWith(MockitoJUnitRunner.class)
public class OverriddenRequestTest {

	@Mock
	HttpServletRequest request;

	@Test
	public void voidTestOverriddenRequest() {
		when(request.getParameterMap()).thenReturn(ImmutableMap.of("param1", "value1"));
		Map<String,String[]> overrides = new HashMap<String, String[]>();
		overrides.put("param1", new String[] { "newvalue1" });
		overrides.put("param2", new String[] { "value2" });
		OverriddenRequest or = new OverriddenRequest(request, overrides);
		assertEquals("newvalue1", or.getParameter("param1"));
		assertEquals("value2", or.getParameter("param2"));
		assertNull(or.getParameter("invalid"));
	}
}
