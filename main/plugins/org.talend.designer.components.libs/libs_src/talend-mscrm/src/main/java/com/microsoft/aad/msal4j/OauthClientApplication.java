package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.LoggerFactory;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

public class OauthClientApplication extends AbstractClientApplicationBase implements IConfidentialClientApplication {

    private ClientAuthentication clientAuthentication;
    private CustomJWTAuthentication customJWTAuthentication;
    private boolean clientCertAuthentication = false;
    private ClientCertificate clientCertificate;
    private boolean sendX5c;
    private final String username;
    private final String password;

    private OauthClientApplication(OauthClientApplication.Builder builder) {
        super(builder);
        validateNotNull("username", builder.username);
        validateNotNull("password", builder.password);
        sendX5c = builder.sendX5c;
        log = LoggerFactory.getLogger(ConfidentialClientApplication.class);
        initClientAuthentication(builder.clientCredential);
        this.username = builder.username;
        this.password = builder.password;
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(ClientCredentialParameters parameters) {
        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_FOR_CLIENT, parameters);
        OauthCredentialRequest clientCredentialRequest = new OauthCredentialRequest(parameters,
                username, password,this, context);

        return this.executeRequest(clientCredentialRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(OnBehalfOfParameters parameters) {
        throw new IllegalStateException("Use ConfidentialClientApplication instead");
    }

    private void initClientAuthentication(IClientCredential clientCredential) {
        validateNotNull("clientCredential", clientCredential);
        if (clientCredential instanceof ClientSecret) {
            clientAuthentication = new ClientSecretPost(new ClientID(clientId()), new Secret(((ClientSecret) clientCredential).clientSecret()));
        } else if (clientCredential instanceof ClientCertificate) {
            this.clientCertAuthentication = true;
            this.clientCertificate = (ClientCertificate) clientCredential;
            clientAuthentication = buildValidClientCertificateAuthority();
        } else if (clientCredential instanceof ClientAssertion) {
            clientAuthentication = createClientAuthFromClientAssertion((ClientAssertion) clientCredential);
        } else {
            throw new IllegalArgumentException("Unsupported client credential");
        }
    }

    @Override
    protected ClientAuthentication clientAuthentication() {
        if (clientCertAuthentication) {
            final Date currentDateTime = new Date(System.currentTimeMillis());
            final Date expirationTime = ((PrivateKeyJWT) clientAuthentication).getJWTAuthenticationClaimsSet().getExpirationTime();
            if (expirationTime.before(currentDateTime)) {
                //The asserted private jwt with the client certificate can expire so rebuild it when the
                clientAuthentication = buildValidClientCertificateAuthority();
            }
        }
        return clientAuthentication;
    }

    private ClientAuthentication buildValidClientCertificateAuthority() {
        ClientAssertion clientAssertion = JwtHelper.buildJwt(clientId(), clientCertificate, this.authenticationAuthority.selfSignedJwtAudience(), sendX5c);
        return createClientAuthFromClientAssertion(clientAssertion);
    }

    private ClientAuthentication createClientAuthFromClientAssertion(final ClientAssertion clientAssertion) {
        final Map<String, List<String>> map = new HashMap<>();
        try {
            map.put("client_assertion_type", Collections.singletonList(ClientAssertion.assertionType));
            map.put("client_assertion", Collections.singletonList(clientAssertion.assertion()));
            return PrivateKeyJWT.parse(map);
        } catch (final ParseException e) {
            //This library is not supposed to validate Issuer and subject values.
            //The next lines of code ensures that exception is not thrown.
            if (e.getMessage().contains("Issuer and subject in client JWT assertion must designate the same client identifier")) {
                return new CustomJWTAuthentication(ClientAuthenticationMethod.PRIVATE_KEY_JWT, clientAssertion, new ClientID(clientId()));
            }
            throw new MsalClientException(e);
        }
    }

    /**
     * Creates instance of Builder of ConfidentialClientApplication

     *

     * @param clientId         Client ID (Application ID) of the application as registered

     *                         in the application registration portal (portal.azure.com)

     * @param clientCredential The client credential to use for token acquisition.

     * @return instance of Builder of ConfidentialClientApplication
     */
    public static OauthClientApplication.Builder builder(String clientId, IClientCredential clientCredential, String username, String password) {
        return new OauthClientApplication.Builder(clientId, clientCredential, username, password);
    }


    public static class Builder extends AbstractClientApplicationBase.Builder<OauthClientApplication.Builder> {

        private IClientCredential clientCredential;
        private final String username;
        private final String password;
        private boolean sendX5c = true;

        private Builder(String clientId, IClientCredential clientCredential, String username, String password) {
            super(clientId);
            this.clientCredential = clientCredential;
            this.username = username;
            this.password = password;
        }

        /**
         * Specifies if the x5c claim (public key of the certificate) should be sent to the STS.
         * Default value is true
         *
         * @param val true if the x5c should be sent. Otherwise false
         * @return instance of the Builder on which method was called
         */
        public OauthClientApplication.Builder sendX5c(boolean val) {
            this.sendX5c = val;
            return self();
        }

        @Override
        public OauthClientApplication build() {
            return new OauthClientApplication(this);
        }

        @Override
        protected OauthClientApplication.Builder self() {
            return this;
        }
    }

    @java.lang.SuppressWarnings("all")
    public boolean sendX5c() {
        return this.sendX5c;
    }
}
