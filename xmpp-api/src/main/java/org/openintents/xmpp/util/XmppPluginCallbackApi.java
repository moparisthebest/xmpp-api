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


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import org.openintents.xmpp.IExecuteService;
import org.openintents.xmpp.IXmppPluginCallback;
import org.openintents.xmpp.XmppPluginCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.openintents.xmpp.util.XmppUtils.getExceptionError;

public class XmppPluginCallbackApi {

    public static final String TAG = "Xmpp API";

    /**
     * see CHANGELOG.md
     */
    public static final int API_VERSION = 1;

    /* Intent extras */
    public static final String EXTRA_API_VERSION = "api_version";

    /**
     * Notify callback of a new message, might be to your account, or from due to carbons or mam, or manual input
     *
     * required extras:
     * String      EXTRA_ACCOUNT_JID           (JID of sending account)
     * String      EXTRA_MESSAGE_FROM
     * String      EXTRA_MESSAGE_TO
     * String      EXTRA_MESSAGE_BODY
     * int         EXTRA_MESSAGE_STATUS
     */
    public static final String ACTION_NEW_MESSAGE = "org.openintents.xmpp.action.NEW_MESSAGE";

    /**
     * Notify callback of a IQ response to a ACTION_SEND_RAW_XML you sent with callback
     *
     * required extras:
     * String        EXTRA_ACCOUNT_JID           (JID of receiving account)
     * String        EXTRA_RAW_XML               (raw XML IQ response)
     */
    public static final String ACTION_IQ_RESPONSE = "org.openintents.xmpp.action.IQ_RESPONSE";

    // extras:
    public static final String EXTRA_MESSAGE_FROM = "message_from";
    public static final String EXTRA_MESSAGE_TO = "message_to";
    public static final String EXTRA_MESSAGE_BODY = "message_body";
    public static final String EXTRA_MESSAGE_STATUS = "message_status";

    /* Service Intent returns */
    public static final String RESULT_CODE = "result_code";

    // get actual error object from RESULT_ERROR
    public static final int RESULT_CODE_ERROR = 0;
    // success!
    public static final int RESULT_CODE_SUCCESS = 1;
    // get PendingIntent from RESULT_INTENT, start PendingIntent with startIntentSenderForResult,
    // and execute service method again in onActivityResult
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;

    public static final String RESULT_ERROR = "error";
    public static final String RESULT_INTENT = "intent";

    public interface IXmppCallback {
        void onReturn(final Intent result);
    }

    protected final IExecuteService executeService;
    protected final Context context;

    protected final String accountJid, localPart, domain;

    // this is thread safe, we only need 1
    private static final AtomicInteger pipeIdGen = new AtomicInteger();

    public XmppPluginCallbackApi(final Context context, final XmppPluginCallback service) {
        this(context, (IExecuteService)service);
    }

    public XmppPluginCallbackApi(final Context context, final IXmppPluginCallback service) {
        this(context, service, null, null, null);
    }

    public XmppPluginCallbackApi(final Context context, final IXmppPluginCallback service,
                                 final String accountJid, final String localPart, final String domain) {
        if(context == null || service == null)
            throw new NullPointerException("context and service cannot be null");
        if(accountJid == null)
            throw new NullPointerException("accountJid must be non-null and non-empty");
        if(localPart != null && domain == null)
            throw new NullPointerException("domain cannot be null if localPart is non-null");
        this.context = context;
        this.executeService = XmppPluginCallback.wrap(service);
        this.accountJid = accountJid;
        this.localPart = localPart;
        this.domain = domain;
    }

    protected XmppPluginCallbackApi(final Context context, final IExecuteService service) {
        if(context == null || service == null)
            throw new NullPointerException("context and service cannot be null");
        this.context = context;
        this.executeService = service;
        this.accountJid = this.localPart = this.domain = null;
    }

    public boolean matches(final String accountJid, final String localPart, final String domain) {
        return (this.accountJid == null || this.accountJid.equals(accountJid)) &&
                (this.localPart == null || this.localPart.equals(localPart)) &&
                (this.domain == null || this.domain.equals(domain));
    }

    /**
     * Rightly throws ClassCastException if this is actually an instance of XmppServiceApi
     * @return
     */
    public XmppPluginCallback getXmppPluginCallback() {
        return (XmppPluginCallback) executeService;
    }

    protected class XmppAsyncTask extends AsyncTask<Void, Integer, Intent> {
        final Intent data;
        final IXmppCallback callback;
        final InputStream is;
        final OutputStream os;

        XmppAsyncTask(final Intent data, final IXmppCallback callback) {
            this(data, callback, null, null);
        }

        XmppAsyncTask(final Intent data, final IXmppCallback callback, final InputStream is, final OutputStream os) {
            this.data = data;
            this.callback = callback;
            this.is = is;
            this.os = os;
        }

        @Override
        protected Intent doInBackground(Void... unused) {
            return executeApi(data, is, os);
        }

        protected void onPostExecute(Intent result) {
            callback.onReturn(result);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void executeApiAsyncPrivate(final XmppAsyncTask task) {
        // don't serialize async tasks!
        // http://commonsware.com/blog/2012/04/20/asynctask-threading-regression-confirmed.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        } else {
            task.execute((Void[]) null);
        }
    }

    public void executeApiAsync(Intent data, InputStream is, OutputStream os, IXmppCallback callback) {
        executeApiAsyncPrivate(new XmppAsyncTask(data, callback, is, os));
    }

    public Intent executeApi(Intent data, InputStream is, OutputStream os) {
        ParcelFileDescriptor input = null;
        try {
            if (is != null) {
                input = ParcelFileDescriptorUtil.pipeFrom(is);
            }

            return executeApi(data, input, os);
        } catch (Exception e) {
            return getErrorIntent(false, e);
        } finally {
            tryClose(input);
        }
    }

    /**
     * InputStream and OutputStreams are always closed after operating on them!
     */
    public Intent executeApi(Intent data, ParcelFileDescriptor input, OutputStream os) {
        ParcelFileDescriptor output = null;
        try {
            // always send version from client
            data.putExtra(EXTRA_API_VERSION, XmppPluginCallbackApi.API_VERSION);

            Intent result;

            Thread pumpThread = null;
            int outputPipeId = 0;

            if (os != null) {
                outputPipeId = pipeIdGen.incrementAndGet();
                output = executeService.createOutputPipe(outputPipeId);
                pumpThread = ParcelFileDescriptorUtil.pipeTo(os, output);
            }

            // blocks until result is ready
            result = executeService.execute(data, input, outputPipeId);

            // set class loader to current context to allow unparcelling
            // of XmppError and XmppSignatureResult
            // http://stackoverflow.com/a/3806769
            result.setExtrasClassLoader(context.getClassLoader());

            //wait for ALL data being pumped from remote side
            if (pumpThread != null) {
                pumpThread.join();
            }

            return result;
        } catch (Exception e) {
            return getErrorIntent(false, e);
        } finally {
            tryClose(output);
        }
    }

    private void tryClose(final ParcelFileDescriptor p) {
        // close() is required to halt the TransferThread
        if (p != null) {
            try {
                p.close();
            } catch (IOException e) {
                Log.e(XmppPluginCallbackApi.TAG, "IOException when closing ParcelFileDescriptor!", e);
            }
        }
    }

    protected Intent getErrorIntent(final boolean callback, final Exception e) {
        Log.e(XmppPluginCallbackApi.TAG, callback ? "Exception in callbackApi call" : "Exception in executeApi call", e);
        return getExceptionError(e);
    }
}
