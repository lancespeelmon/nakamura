package org.sakaiproject.nakamura.user;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.user.UserFinder;
import org.sakaiproject.nakamura.formauth.FormLoginServlet;

import com.google.common.collect.ImmutableSet;


@RunWith(MockitoJUnitRunner.class)
public class CaseInsensitiveAuthFilterTest {

	@Mock
	HttpServletRequest request;
	
	@Mock
	HttpServletResponse response;
	
	@Mock
	FilterChain chain;

	@Mock
	UserFinder finder;
	
	CaseInsensitiveAuthFilter filter;
	
	@Before
	public void setUp(){
		filter = new CaseInsensitiveAuthFilter();
		filter.userFinder = finder;
	}
	
	@Test
	public void testOnlyPost() throws IOException, ServletException {
		when(request.getMethod()).thenReturn("GET");
		filter.doFilter(request, response, chain);
		verify(request).getMethod();
		verifyNoMoreInteractions(request);
	}
	
	@Test
	public void testOnlyUrls() throws IOException, ServletException {
		when(request.getMethod()).thenReturn("POST");
		filter.doFilter(request, response, chain);
		verify(request).getMethod();
		verify(request, times(2)).getRequestURI();
		verifyNoMoreInteractions(request);
	}

	@Test
	public void testLoginRequiresUserName() throws IOException, ServletException {
		when(request.getMethod()).thenReturn("POST");
		when(request.getRequestURI()).thenReturn("/system/sling/formlogin");
		filter.doFilter(request, response, chain);
		verify(request).getMethod();
		verify(request, times(2)).getRequestURI();
		verify(request).getParameter((FormLoginServlet.USERNAME));
		verifyNoMoreInteractions(request);
	}

	@Test
	public void testLoginRewritesUserName() throws Exception {
		when(request.getMethod()).thenReturn("POST");
		when(request.getRequestURI()).thenReturn("/system/sling/formlogin");
		when(request.getParameter((FormLoginServlet.USERNAME))).thenReturn("userid1");
		when(finder.findUsersByName("userid1")).thenReturn(ImmutableSet.of("USERID1"));
		
		filter.doFilter(request, response, chain);
		verify(request).getMethod();
		verify(request, times(2)).getRequestURI();
		verify(request).getParameter((FormLoginServlet.USERNAME));
		verify(request).getParameterMap();
		verify(chain).doFilter(any(OverriddenRequest.class), any(HttpServletResponse.class));
		verifyNoMoreInteractions(request);
	}
	
	@Test
	public void testStoreLowerRequiresUserName() throws IOException, ServletException {
		when(request.getMethod()).thenReturn("POST");
		when(request.getRequestURI()).thenReturn("/system/userManager/user.create.html");
		filter.doFilter(request, response, chain);
		verify(request).getMethod();
		verify(request, times(2)).getRequestURI();
		verify(request).getParameter((":name"));
		verifyNoMoreInteractions(request);
	}

	@Test
	public void testStoreLowerOverridesRequest() throws Exception {
		when(request.getMethod()).thenReturn("POST");
		when(request.getRequestURI()).thenReturn("/system/userManager/user.create.html");
		when(request.getParameter((":name"))).thenReturn("USERID1");
		
		filter.doFilter(request, response, chain);
		verify(request).getMethod();
		verify(request, times(2)).getRequestURI();
		verify(request).getParameter(":name");
		verify(request).getParameterMap();
		verify(chain).doFilter(any(OverriddenRequest.class), any(HttpServletResponse.class));
		verifyNoMoreInteractions(request);
	}
}
