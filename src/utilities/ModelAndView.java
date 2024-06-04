package utilities;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    String url ;
    Map<String,Object> data = new HashMap<>();

    public void setUrl(String url) {
        this.url = url;
    }
    public void addObject(String url , Object data) {
        this.data.put(url, data);
    }

    public String getUrl() {
        return url;
    }
    public Map<String, Object> getData() {
        return data;
    }

}
