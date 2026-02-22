package com.aemtools.aem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse<T> {

    @JsonProperty("data")
    private T data;

    @JsonProperty("total")
    private long total;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("links")
    private List<Link> links;

    @JsonProperty("errors")
    private List<ApiError> errors;

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public List<Link> getLinks() { return links; }
    public void setLinks(List<Link> links) { this.links = links; }

    public List<ApiError> getErrors() { return errors; }
    public void setErrors(List<ApiError> errors) { this.errors = errors; }

    public boolean hasErrors() { return errors != null && !errors.isEmpty(); }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        @JsonProperty("href")
        private String href;

        @JsonProperty("rel")
        private String rel;

        @JsonProperty("type")
        private String type;

        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        public String getRel() { return rel; }
        public void setRel(String rel) { this.rel = rel; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiError {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("details")
        private Map<String, Object> details;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}
