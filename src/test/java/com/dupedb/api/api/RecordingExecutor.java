package com.dupedb.api.api;

import com.dupedb.api.internal.HttpExecutor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test double for HttpExecutor that records all method calls without making
 * real HTTP requests. Used by API client tests to verify correct path
 * construction and method delegation.
 */
class RecordingExecutor extends HttpExecutor {

    /**
     * A recorded method call with its path and body.
     *
     * @param method the HTTP method used (GET_CLASS, GET_TYPE, POST, PUT, PATCH,
     *               POST_FORM, DELETE, DELETE_RESPONSE, POST_MULTIPART)
     * @param path   the API path requested
     * @param body   the request body (null for GET/DELETE)
     */
    record Call(String method, String path, Object body) {}

    private final List<Call> calls = new ArrayList<>();

    RecordingExecutor() {
        super("https://test.dupedb.net", null);
    }

    /**
     * Returns all recorded calls in order.
     */
    List<Call> getCalls() {
        return calls;
    }

    @Override
    public <T> T get(String path, Class<T> type) {
        calls.add(new Call("GET_CLASS", path, null));
        return null;
    }

    @Override
    public <T> T get(String path, Type type) {
        calls.add(new Call("GET_TYPE", path, null));
        return null;
    }

    @Override
    public <T> T post(String path, Object body, Class<T> type) {
        calls.add(new Call("POST", path, body));
        return null;
    }

    @Override
    public <T> T put(String path, Object body, Class<T> type) {
        calls.add(new Call("PUT", path, body));
        return null;
    }

    @Override
    public <T> T patch(String path, Object body, Class<T> type) {
        calls.add(new Call("PATCH", path, body));
        return null;
    }

    @Override
    public <T> T postForm(String path, Map<String, String> formData, Class<T> type) {
        calls.add(new Call("POST_FORM", path, formData));
        return null;
    }

    @Override
    public void delete(String path) {
        calls.add(new Call("DELETE", path, null));
    }

    @Override
    public <T> T deleteWithResponse(String path, Class<T> type) {
        calls.add(new Call("DELETE_RESPONSE", path, null));
        return null;
    }

    @Override
    public <T> T postMultipart(String path, Map<String, Object> parts, Class<T> type) {
        calls.add(new Call("POST_MULTIPART", path, parts));
        return null;
    }
}
