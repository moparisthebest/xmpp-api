/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.openintents.xmpp.example;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.openintents.xmpp.XmppError;
import org.openintents.xmpp.AbstractXmppPluginCallback;
import org.openintents.xmpp.util.XmppPluginCallbackApi;
import org.openintents.xmpp.util.XmppServiceApi;
import org.openintents.xmpp.util.XmppServiceConnection;

import java.io.*;
import java.util.Date;
import java.util.Locale;

public class XmppApiActivity extends Activity {
    private EditText message;
    private EditText messageCallbackLocalPart, messageCallbackDomain;

    private XmppServiceConnection serviceConnection;

    private String accountJid;

    public static final int REQUEST_CODE_SEND_MESSAGE = 9910;
    public static final int REQUEST_CODE_REGISTER_CALLBACK = 9915;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xmpp_provider);

        message = (EditText) findViewById(R.id.message);
        Button sendMessage = (Button) findViewById(R.id.send_message);
        messageCallbackLocalPart = (EditText) findViewById(R.id.message_callback_localpart);
        messageCallbackDomain = (EditText) findViewById(R.id.message_callback_domain);
        Button registerMessageCallback = (Button) findViewById(R.id.register_message_callback);

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new Intent());
            }
        });
        registerMessageCallback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerMessageCallback(new Intent());
            }
        });

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String providerPackageName = settings.getString("xmpp_provider_list", "");
        accountJid = settings.getString("xmpp_key", "");
        if (TextUtils.isEmpty(providerPackageName)) {
            Toast.makeText(this, "No XMPP app selected!", Toast.LENGTH_LONG).show();
            finish();
        } else if (TextUtils.isEmpty(accountJid)) {
            Toast.makeText(this, "No account selected!", Toast.LENGTH_LONG).show();
            finish();
        } else {

            // set interesting default text
            message.setText("<message xmlns=\"jabber:client\" to=\"test@echo.burtrum.org\" type=\"normal\" id=\"9316996e-88da-4d6b-9bb6-6ff19c096a2c\" from=\""+accountJid+"\">\n" +
                    "  <echo xmlns=\"https://code.moparisthebest.com/moparisthebest/xmpp-echo-self\"/>\n" +
                    "    <forwarded xmlns=\"urn:xmpp:forward:0\">\n" +
                    "        <message xmlns=\"jabber:client\" from=\"test@echo.burtrum.org\" type=\"chat\" id=\"9316996e-88da-4d6b-9bb6-6ff19c096a2b\" to=\""+accountJid+"\">\n" +
                    "            <body>Now is "+new Date()+"</body>\n" +
                    "        </message>\n" +
                    "    </forwarded>\n" +
                    "</message>");

            // bind to service
            serviceConnection = new XmppServiceConnection(
                    XmppApiActivity.this.getApplicationContext(),
                    providerPackageName,
                    new XmppServiceConnection.OnBound() {
                        @Override
                        public void onBound(XmppServiceApi serviceApi) {
                            Log.d(XmppServiceApi.TAG, "onBound!");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(XmppServiceApi.TAG, "exception when binding!", e);
                        }
                    }
            );
            serviceConnection.bindToService();
        }
    }

    private void handleError(final XmppError error) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(XmppApiActivity.this,
                        "onError id:" + error.getErrorId() + "\n\n" + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e(Constants.TAG, "onError getErrorId:" + error.getErrorId());
                Log.e(Constants.TAG, "onError getMessage:" + error.getMessage());
            }
        });
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(XmppApiActivity.this,
                        message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class MyCallback implements XmppServiceApi.IXmppCallback {
        boolean returnToCiphertextField;
        ByteArrayOutputStream os;
        int requestCode;

        private MyCallback(boolean returnToCiphertextField, ByteArrayOutputStream os, int requestCode) {
            this.returnToCiphertextField = returnToCiphertextField;
            this.os = os;
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            switch (result.getIntExtra(XmppServiceApi.RESULT_CODE, XmppServiceApi.RESULT_CODE_ERROR)) {
                case XmppServiceApi.RESULT_CODE_SUCCESS: {
                    showToast("RESULT_CODE_SUCCESS");
                    break;
                }
                case XmppServiceApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    showToast("RESULT_CODE_USER_INTERACTION_REQUIRED");

                    PendingIntent pi = result.getParcelableExtra(XmppServiceApi.RESULT_INTENT);
                    try {
                        XmppApiActivity.this.startIntentSenderFromChild(
                                XmppApiActivity.this, pi.getIntentSender(),
                                requestCode, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(Constants.TAG, "SendIntentException", e);
                    }
                    break;
                }
                case XmppServiceApi.RESULT_CODE_ERROR: {
                    showToast("RESULT_CODE_ERROR");

                    XmppError error = result.getParcelableExtra(XmppServiceApi.RESULT_ERROR);
                    handleError(error);
                    break;
                }
            }
        }
    }

    private AbstractXmppPluginCallback pluginCallback = new AbstractXmppPluginCallback() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        @Override
        public Intent execute(final Intent data, final InputStream inputStream, final OutputStream outputStream) {
            if(XmppPluginCallbackApi.ACTION_NEW_MESSAGE.equals(data.getAction())) {
                showToast(String.format(Locale.US, "status: %d, from: '%s', to: '%s', body: '%s'",
                        data.getIntExtra(XmppPluginCallbackApi.EXTRA_MESSAGE_STATUS, -1),
                        data.getStringExtra(XmppPluginCallbackApi.EXTRA_MESSAGE_FROM),
                        data.getStringExtra(XmppPluginCallbackApi.EXTRA_MESSAGE_TO),
                        data.getStringExtra(XmppPluginCallbackApi.EXTRA_MESSAGE_BODY)));
                final Intent result = new Intent();
                result.putExtra(XmppPluginCallbackApi.RESULT_CODE, XmppPluginCallbackApi.RESULT_CODE_SUCCESS);
                return result;
            }
            final Intent result = new Intent();
            result.putExtra(XmppPluginCallbackApi.RESULT_CODE, XmppPluginCallbackApi.RESULT_CODE_ERROR);
            result.putExtra(XmppPluginCallbackApi.RESULT_ERROR,
                    new XmppError(XmppError.INCOMPATIBLE_API_VERSIONS, "action not implemented"));
            return result;
        }
    };

    public void sendMessage(Intent data) {
        data.setAction(XmppServiceApi.ACTION_SEND_RAW_XML);
        data.putExtra(XmppServiceApi.EXTRA_ACCOUNT_JID, accountJid);
        data.putExtra(XmppServiceApi.EXTRA_RAW_XML, message.getText().toString());

        serviceConnection.getApi().executeApiAsync(data, null, null, new MyCallback(false, null, REQUEST_CODE_SEND_MESSAGE));
    }

    public void registerMessageCallback(Intent data) {
        data.setAction(XmppServiceApi.ACTION_REGISTER_PLUGIN_CALLBACK);
        data.putExtra(XmppServiceApi.EXTRA_ACCOUNT_JID, accountJid);
        final String localPart = messageCallbackLocalPart.getText().toString().trim();
        final String domain = messageCallbackDomain.getText().toString().trim();
        if(!localPart.isEmpty())
            data.putExtra(XmppServiceApi.EXTRA_JID_LOCAL_PART, localPart);
        if(!domain.isEmpty())
            data.putExtra(XmppServiceApi.EXTRA_JID_DOMAIN, domain);

        serviceConnection.getApi().callbackApiAsync(data, pluginCallback, new MyCallback(true, null, REQUEST_CODE_REGISTER_CALLBACK));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(Constants.TAG, "onActivityResult resultCode: " + resultCode);

        // try again after user interaction
        if (resultCode == RESULT_OK) {
            /*
             * The data originally given to one of the methods above, is again
             * returned here to be used when calling the method again after user
             * interaction. The Intent now also contains results from the user
             * interaction, for example selected key ids.
             */
            switch (requestCode) {
                case REQUEST_CODE_SEND_MESSAGE: {
                    sendMessage(data);
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceConnection != null) {
            serviceConnection.unbindFromService();
        }
    }

}
