package com.xxl.sso.core.store.impl;

import com.xxl.sso.core.model.LoginInfo;
import com.xxl.sso.core.store.LoginStore;
import com.xxl.sso.core.token.TokenHelper;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.Response;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xuxueli 2018-04-02 20:03:11
 */
public class LocalLoginStore implements LoginStore {

    private final ConcurrentMap<String, LoginInfo> loginStore = new ConcurrentHashMap<>();

    /**
     * parse store key from token
     *
     * @param tokenLoginInfo
     * @return
     */
    private String parseStoreKey(LoginInfo tokenLoginInfo){
        if (tokenLoginInfo == null) {
            return null;
        }
        return tokenLoginInfo.getUserId();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public Response<String> set(String token, LoginInfo loginInfo, long tokenTimeout) {

        // parse storeKey
        String storeKey = parseStoreKey(TokenHelper.parseToken(token));
        if (StringTool.isBlank(storeKey)) {
            return Response.ofFail("token invalid.");
        }

        // valid loginInfo
        if (loginInfo == null
                || StringTool.isBlank(loginInfo.getUserId())
                || StringTool.isBlank(loginInfo.getUserName())) {
            return Response.ofFail("loginInfo invalid.");
        }

        // process expire time
        long expireTime = System.currentTimeMillis() + tokenTimeout;
        if (expireTime < System.currentTimeMillis()) {
            return Response.ofFail("expireTime invalid.");
        }
        loginInfo.setExpireTime(expireTime);

        // write
        loginStore.put(storeKey, loginInfo);
        return Response.ofSuccess(token);
    }

    @Override
    public LoginInfo get(String token) {
        // parse storeKey
        LoginInfo tokenLoginInfo = TokenHelper.parseToken(token);
        String storeKey = parseStoreKey(tokenLoginInfo);
        if (StringTool.isBlank(storeKey)) {
            return null;
        }
        String version = tokenLoginInfo.getVersion();

        // read
        LoginInfo loginInfo = loginStore.get(storeKey);

        // valid
        if (loginInfo != null) {
            // valid expire time
            if (loginInfo.getExpireTime() < System.currentTimeMillis()) {
                loginStore.remove(storeKey);
                return null;
            }
            // valid version if inconsistent
            if (loginInfo.getVersion()!=null && !loginInfo.getVersion().equals(version)){
                return null;    // Non-empty and inconsistent, intercept, intercept it
            }
        }

        return loginInfo;
    }

    @Override
    public Response<String> remove(String token) {
        // parse storeKey
        String storeKey = parseStoreKey(TokenHelper.parseToken(token));
        if (StringTool.isBlank(storeKey)) {
            return Response.ofFail("token is invalid");
        }

        // remove
        loginStore.remove(storeKey);
        return Response.ofSuccess();
    }

}
