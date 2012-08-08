package org.sakaiproject.nakamura.user;

import com.google.common.collect.ImmutableMap;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class OverriddenRequest extends HttpServletRequestWrapper {

	private Map<String,String[]> modifiedMap;
  private String mapEncoding;
  private String charEncoding;

	@SuppressWarnings("unchecked")
	public OverriddenRequest(HttpServletRequest request, Map<String,String[]> overrides) {
		super(request);
		modifiedMap = new HashMap<String, String[]>();
		modifiedMap.putAll((Map<String, String[]>)super.getParameterMap());
		modifiedMap.putAll(overrides);
    charEncoding = request.getCharacterEncoding();
    if (charEncoding == null) {
      charEncoding = "UTF-8";
    }
    mapEncoding = charEncoding;
	}

  public String getCharacterEncoding () {
    return charEncoding;
  }

  public void setCharacterEncoding (String encoding) {
    charEncoding = encoding;
  }

  protected void lazyEncodeParameterMap() {
    if (mapEncoding != null && !mapEncoding.equals(charEncoding)) {
      ImmutableMap.Builder<String, String[]> builder = new ImmutableMap.Builder<String, String[]> ();
      for (Map.Entry<String, String[]> entry : modifiedMap.entrySet()) {
        String key = entry.getKey();
        String[] valueArr = entry.getValue();

        if (valueArr != null) {
          String[] encodedArr = new String[valueArr.length];
          for (int i = 0; i < valueArr.length; i++) {
            try {
              encodedArr[i] = new String(valueArr[i].getBytes(mapEncoding), charEncoding);
            } catch (UnsupportedEncodingException e) {
              encodedArr[i] = valueArr[i];
            }
          }

          builder.put(key, encodedArr);
        }
      }

      modifiedMap = builder.build();
      mapEncoding = charEncoding;
    }
  }

	@Override
	public Map<String, String[]> getParameterMap() {
    lazyEncodeParameterMap();
		/**
		 * Deep down in the sling authentication stack the request.parameterMap is
		 * copied and used to get the parameters.
		 */
		return modifiedMap;
	}

	@Override
	public String getParameter(String parameter) {
    lazyEncodeParameterMap();

		String[] value = modifiedMap.get(parameter);
		if (value != null){
			return value[0];
		}
		return null;
	}
}