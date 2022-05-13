// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.ms.crm.odata.authentication;

import java.net.Proxy;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.naming.AuthenticationException;
import javax.naming.ServiceUnavailableException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.olingo.client.api.communication.request.ODataRequest;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.talend.ms.crm.odata.ClientConfiguration;
import org.talend.ms.crm.odata.ProxyProvider;
import org.talend.ms.crm.odata.httpclientfactory.IHttpclientFactoryObservable;
import org.talend.ms.crm.odata.httpclientfactory.OAuthHttpClientFactory;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.OauthClientApplication;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;

public class OAuthStrategyImpl implements IAuthStrategy {

    private ClientConfiguration conf;

    private IAuthenticationResult authResult;

    private IHttpclientFactoryObservable httpClientFactory;

    OAuthStrategyImpl(ClientConfiguration conf) {
        this.conf = conf;
    }

    @Override
    public void init() throws AuthenticationException {
        try {
            authResult = getAccessToken();
        } catch (ServiceUnavailableException e) {
            throw new AuthenticationException(e.getMessage());
        }
    }

    @Override
    public IHttpclientFactoryObservable getHttpClientFactory() throws AuthenticationException {
        try {
            authResult = getAccessToken();
        } catch (ServiceUnavailableException e) {
            throw new AuthenticationException(e.getMessage());
        }

        if (httpClientFactory == null) {
            httpClientFactory = new OAuthHttpClientFactory(this.conf);
        }

        return httpClientFactory;

    }

    @Override
    public void configureRequest(ODataRequest request) {
        request.addCustomHeader(HttpHeader.AUTHORIZATION, "Bearer " + authResult.accessToken());
    }

    @Override
    public void configureRequest(HttpRequestBase request) {
        request.addHeader(HttpHeader.AUTHORIZATION, "Bearer " + authResult.accessToken());
    }

    @Override
    public void refreshAuth() throws AuthenticationException {

        try {
            this.refreshToken();
        } catch (ServiceUnavailableException e) {
            throw new AuthenticationException(e.getMessage());
        }
    }

    /**
     * Refresh token when expired
     */
    public void refreshToken() throws ServiceUnavailableException {
        int retryTime = 0;
        // refresh the access token
        while (true) {
            try {
                this.authResult = getAccessToken();
                break;// refresh token successfully
            } catch (ServiceUnavailableException e) {
                if (retryTime < conf.getMaxRetryTimes()) {
                    retryTime++;
                    try {
                        Thread.sleep(conf.getIntervalTime());
                    } catch (InterruptedException e1) {
                        // ignore
                    }
                    continue;// retry to refresh token
                }
                throw e;// failed to refresh token after retry
            }

        }
    }

    private Future<IAuthenticationResult> acquireToken(PublicClientApplication context) throws Exception {
        Future<IAuthenticationResult> future;
            UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(
                    Collections.singleton(conf.getResource() + "/.default"), conf.getUserName(), conf.getPassword().toCharArray()).build();
            future = context.acquireToken(parameters);
            return future;

    }
    private Future<IAuthenticationResult> acquireToken(OauthClientApplication context) throws Exception {
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                Collections.singleton(conf.getResource() + "/.default")).build();
        return context.acquireToken(parameters);
    }

    private IAuthenticationResult getAccessToken() throws ServiceUnavailableException {
        if(conf.getAppRegisteredType() == ClientConfiguration.AppRegisteredType.NATIVE_APP){
            return getAccessTokenNative();
        } if(conf.getAppRegisteredType() == ClientConfiguration.AppRegisteredType.WEB_APP && conf.getWebAppPermission() == ClientConfiguration.WebAppPermission.DELEGATED){
            return getAccessTokenWebApp();
        } else {
            throw new RuntimeException("Can't retrieve token with this configuration : registered application type: "+conf.getAppRegisteredType()+", Web application permission: "+conf.getWebAppPermission());
        }
    }

    private IAuthenticationResult getAccessTokenNative() throws ServiceUnavailableException {
        PublicClientApplication context = null;
        IAuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            Proxy proxy = ProxyProvider.getProxy();
            PublicClientApplication.Builder contextBuilder = PublicClientApplication.builder(conf.getClientId()).authority("https://login.microsoftonline.com/organizations");
            if (proxy != null) {
                contextBuilder = contextBuilder.proxy(proxy);
            }
            context = contextBuilder.build();
            Future<IAuthenticationResult> future = this.acquireToken(context);
            result = future.get();
        } catch (Exception e) {
            throw new ServiceUnavailableException(e.getMessage());
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException("Authenticated failed! Please check your configuration!");
        }
        return result;
    }

    private IAuthenticationResult getAccessTokenWebApp() throws ServiceUnavailableException {
        OauthClientApplication context = null;
        IAuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            OauthClientApplication.Builder contextBuilder = OauthClientApplication.builder(conf.getClientId(),
                    ClientCredentialFactory.createFromSecret(conf.getClientSecret()), conf.getUserName(), conf.getPassword())
                    .authority(conf.getAuthoryEndpoint());
            Proxy proxy = ProxyProvider.getProxy();
            if (proxy != null) {
                contextBuilder.proxy(proxy);
            }
            context = contextBuilder.build();
            Future<IAuthenticationResult> future = this.acquireToken(context);
            result = future.get();
        } catch (Exception e) {
            throw new ServiceUnavailableException(e.getMessage());
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException("Authenticated failed! Please check your configuration!");
        }
        return result;
    }

}
