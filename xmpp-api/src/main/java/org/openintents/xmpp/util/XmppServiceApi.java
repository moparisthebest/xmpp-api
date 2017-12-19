/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.xmpp.util;


import android.content.Context;
import android.content.Intent;

import org.openintents.xmpp.*;

public class XmppServiceApi extends XmppPluginCallbackApi {

    public static final String SERVICE_INTENT = "org.openintents.xmpp.IXmppService";

    /**
     * General extras
     * --------------
     *
     * required extras:
     * int           EXTRA_API_VERSION           (always required)
     *
     * returned extras:
     * int           RESULT_CODE                 (RESULT_CODE_ERROR, RESULT_CODE_SUCCESS or RESULT_CODE_USER_INTERACTION_REQUIRED)
     * XmppError     RESULT_ERROR                (if RESULT_CODE == RESULT_CODE_ERROR)
     * PendingIntent RESULT_INTENT               (if RESULT_CODE == RESULT_CODE_USER_INTERACTION_REQUIRED)
     */

    /**
     * Get all versions of the API the provider supports
     *
     * returned extras:
     * int[]        EXTRA_SUPPORTED_VERSIONS
     */
    public static final String ACTION_GET_SUPPORTED_VERSIONS = "org.openintents.xmpp.action.GET_SUPPORTED_VERSIONS";

    /**
     * This action performs no operation, but can be used to check if the App has permission
     * to access the API in general, returning a user interaction PendingIntent otherwise.
     * This can be used to trigger the permission dialog explicitly.
     * 
     * This action uses no extras.
     */
    public static final String ACTION_CHECK_PERMISSION = "org.openintents.xmpp.action.CHECK_PERMISSION";

    /**
     * Send arbitrary raw XML using chosen account, only one element at a time (single stanza or nonza)
     *
     * if sending IQ, can request callback by sending IXmppPluginCallback to callback, ignored otherwise
     * 
     * required extras:
     * String        EXTRA_ACCOUNT_JID           (JID of sending account)
     * String        EXTRA_RAW_XML               (raw XML to send)
     */
    public static final String ACTION_SEND_RAW_XML = "org.openintents.xmpp.action.SEND_RAW_XML";

    /**
     * Register a plugin callback to receive messages matching a certain domain and/or local part
     *
     * required extras:
     * String        EXTRA_ACCOUNT_JID           (JID of associated account)
     *
     * optional extras:
     * String        EXTRA_JID_DOMAIN            (to match JID domain of conversation partner)
     * String        EXTRA_JID_LOCAL_PART        (to match JID localpart of conversation partner, cannot supply without domain)
     *
     * Must use callback method and send in callback
     */
    public static final String ACTION_REGISTER_PLUGIN_CALLBACK = "org.openintents.xmpp.action.REGISTER_PLUGIN_CALLBACK";

    /**
     * Unregister a previously registered plugin callback
     *
     * required extras:
     * String        EXTRA_ACCOUNT_JID           (JID of associated account)
     *
     * Must use callback method and send in callback
     */
    public static final String ACTION_UNREGISTER_PLUGIN_CALLBACK = "org.openintents.xmpp.action.UNREGISTER_PLUGIN_CALLBACK";

    /**
     * Select key id for signing
     * 
     * optional extras:
     * String      EXTRA_ACCOUNT_JID
     * 
     * returned extras:
     * String        EXTRA_ACCOUNT_JID
     */
    public static final String ACTION_GET_ACCOUNT_JID = "org.openintents.xmpp.action.GET_ACCOUNT_JID";

    // extras:
    public static final String EXTRA_SUPPORTED_VERSIONS = "supported_versions";
    public static final String EXTRA_ACCOUNT_JID = "account_jid";
    public static final String EXTRA_RAW_XML = "raw_xml";
    public static final String EXTRA_JID_DOMAIN = "jid_domain";
    public static final String EXTRA_JID_LOCAL_PART = "jid_local_part";

    private final XmppService xmppService;

    public XmppServiceApi(Context context, XmppService service) {
        super(context, service);
        this.xmppService = service;
    }

    public XmppServiceApi(Context context, IXmppService service) {
        super(context, XmppService.wrap(service));
        this.xmppService = (XmppService) super.executeService; // we know this is what we sent in above
    }

    public XmppService getXmppService() {
        return xmppService;
    }

    private class PluginXmppAsyncTask extends XmppAsyncTask {
        final IXmppPluginCallback pluginCallback;

        public PluginXmppAsyncTask(final Intent data, final IXmppCallback callback, final IXmppPluginCallback pluginCallback) {
            super(data, callback);
            this.pluginCallback = pluginCallback;
        }

        @Override
        protected Intent doInBackground(Void... unused) {
            return callbackApi(data, pluginCallback);
        }
    }

    public void callbackApiAsync(Intent data, IXmppPluginCallback pluginCallback, IXmppCallback callback) {
        executeApiAsyncPrivate(new PluginXmppAsyncTask(data, callback, pluginCallback));
    }

    public Intent callbackApi(Intent data, IXmppPluginCallback pluginCallback) {
        try {
            // always send version from client
            data.putExtra(EXTRA_API_VERSION, XmppServiceApi.API_VERSION);

            // blocks until result is ready
            final Intent result = xmppService.callback(data, pluginCallback);

            // set class loader to current context to allow unparcelling
            // of XmppError and XmppSignatureResult
            // http://stackoverflow.com/a/3806769
            result.setExtrasClassLoader(context.getClassLoader());

            return result;
        } catch (Exception e) {
            return getErrorIntent(true, e);
        }
    }

}
