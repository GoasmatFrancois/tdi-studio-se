package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.auth.Secret;

class OauthCredentialRequest extends MsalRequest {
        ClientCredentialParameters parameters;
    OauthCredentialRequest(ClientCredentialParameters parameters, String username, String password,
                OauthClientApplication application, RequestContext requestContext) {
            super(application, createMsalGrant(parameters, username, password), requestContext);
            this.parameters = parameters;
        }

        private static OAuthAuthorizationGrant createMsalGrant(ClientCredentialParameters parameters,
                String username, String password) {
            return new OAuthAuthorizationGrant(new ResourceOwnerPasswordCredentialsGrant(username, new Secret(
                    password)), parameters.scopes(), parameters.claims());
        }

}
