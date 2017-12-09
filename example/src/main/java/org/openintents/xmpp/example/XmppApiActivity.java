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

import org.openintents.xmpp.IXmppService;
import org.openintents.xmpp.XmppError;
import org.openintents.xmpp.util.XmppApi;
import org.openintents.xmpp.util.XmppServiceConnection;
import org.openintents.xmpp.util.XmppUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class XmppApiActivity extends Activity {
    private EditText mMessage;
    private EditText mCiphertext;
    private EditText mDetachedSignature;
    private EditText mEncryptUserIds;
    private EditText mGetKeyEdit;
    private EditText mGetKeyIdsEdit;

    private XmppServiceConnection mServiceConnection;

    private long mSignKeyId;

    public static final int REQUEST_CODE_CLEARTEXT_SIGN = 9910;
    public static final int REQUEST_CODE_ENCRYPT = 9911;
    public static final int REQUEST_CODE_SIGN_AND_ENCRYPT = 9912;
    public static final int REQUEST_CODE_DECRYPT_AND_VERIFY = 9913;
    public static final int REQUEST_CODE_GET_KEY = 9914;
    public static final int REQUEST_CODE_GET_KEY_IDS = 9915;
    public static final int REQUEST_CODE_DETACHED_SIGN = 9916;
    public static final int REQUEST_CODE_DECRYPT_AND_VERIFY_DETACHED = 9917;
    public static final int REQUEST_CODE_BACKUP = 9918;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xmpp_provider);

        mMessage = (EditText) findViewById(R.id.crypto_provider_demo_message);
        mCiphertext = (EditText) findViewById(R.id.crypto_provider_demo_ciphertext);
        mDetachedSignature = (EditText) findViewById(R.id.crypto_provider_demo_detached_signature);
        mEncryptUserIds = (EditText) findViewById(R.id.crypto_provider_demo_encrypt_user_id);
        Button cleartextSign = (Button) findViewById(R.id.crypto_provider_demo_cleartext_sign);
        Button detachedSign = (Button) findViewById(R.id.crypto_provider_demo_detached_sign);
        Button encrypt = (Button) findViewById(R.id.crypto_provider_demo_encrypt);
        Button signAndEncrypt = (Button) findViewById(R.id.crypto_provider_demo_sign_and_encrypt);
        Button decryptAndVerify = (Button) findViewById(R.id.crypto_provider_demo_decrypt_and_verify);
        Button verifyDetachedSignature = (Button) findViewById(R.id.crypto_provider_demo_verify_detached_signature);
        mGetKeyEdit = (EditText) findViewById(R.id.crypto_provider_demo_get_key_edit);
        mGetKeyIdsEdit = (EditText) findViewById(R.id.crypto_provider_demo_get_key_ids_edit);
        Button getKey = (Button) findViewById(R.id.crypto_provider_demo_get_key);
        Button getKeyIds = (Button) findViewById(R.id.crypto_provider_demo_get_key_ids);
        Button backup = (Button) findViewById(R.id.crypto_provider_demo_backup);

        cleartextSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleartextSign(new Intent());
            }
        });
        detachedSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detachedSign(new Intent());
            }
        });
        encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encrypt(new Intent());
            }
        });
        signAndEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signAndEncrypt(new Intent());
            }
        });
        decryptAndVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAndVerify(new Intent());
            }
        });
        verifyDetachedSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAndVerifyDetached(new Intent());
            }
        });
        getKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getKey(new Intent());
            }
        });
        getKeyIds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getKeyIds(new Intent());
            }
        });
        backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backup(new Intent());
            }
        });

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String providerPackageName = settings.getString("xmpp_provider_list", "");
        mSignKeyId = settings.getLong("xmpp_key", 0);
        if (TextUtils.isEmpty(providerPackageName)) {
            Toast.makeText(this, "No XMPP app selected!", Toast.LENGTH_LONG).show();
            finish();
        } else if (mSignKeyId == 0) {
            Toast.makeText(this, "No key selected!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            // bind to service
            mServiceConnection = new XmppServiceConnection(
                    XmppApiActivity.this.getApplicationContext(),
                    providerPackageName,
                    new XmppServiceConnection.OnBound() {
                        @Override
                        public void onBound(IXmppService service) {
                            Log.d(XmppApi.TAG, "onBound!");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(XmppApi.TAG, "exception when binding!", e);
                        }
                    }
            );
            mServiceConnection.bindToService();
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

    /**
     * Takes input from message or ciphertext EditText and turns it into a ByteArrayInputStream
     */
    private InputStream getInputstream(boolean ciphertext) {
        InputStream is = null;
        try {
            String inputStr;
            if (ciphertext) {
                inputStr = mCiphertext.getText().toString();
            } else {
                inputStr = mMessage.getText().toString();
            }
            is = new ByteArrayInputStream(inputStr.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
        }

        return is;
    }

    private class MyCallback implements XmppApi.IXmppCallback {
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
            switch (result.getIntExtra(XmppApi.RESULT_CODE, XmppApi.RESULT_CODE_ERROR)) {
                case XmppApi.RESULT_CODE_SUCCESS: {
                    showToast("RESULT_CODE_SUCCESS");

                    // encrypt/decrypt/sign/verify
                    if (os != null) {
                        try {
                            Log.d(XmppApi.TAG, "result: " + os.toByteArray().length
                                    + " str=" + os.toString("UTF-8"));

                            if (returnToCiphertextField) {
                                mCiphertext.setText(os.toString("UTF-8"));
                            } else {
                                mMessage.setText(os.toString("UTF-8"));
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
                        }
                    }

                    switch (requestCode) {
                        case REQUEST_CODE_DETACHED_SIGN: {
                            byte[] detachedSig
                                    = result.getByteArrayExtra(XmppApi.RESULT_DETACHED_SIGNATURE);
                            Log.d(XmppApi.TAG, "RESULT_DETACHED_SIGNATURE: " + detachedSig.length
                                    + " str=" + new String(detachedSig));
                            mDetachedSignature.setText(new String(detachedSig));

                            break;
                        }
                        default: {

                        }
                    }

                    break;
                }
                case XmppApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    showToast("RESULT_CODE_USER_INTERACTION_REQUIRED");

                    PendingIntent pi = result.getParcelableExtra(XmppApi.RESULT_INTENT);
                    try {
                        XmppApiActivity.this.startIntentSenderFromChild(
                                XmppApiActivity.this, pi.getIntentSender(),
                                requestCode, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(Constants.TAG, "SendIntentException", e);
                    }
                    break;
                }
                case XmppApi.RESULT_CODE_ERROR: {
                    showToast("RESULT_CODE_ERROR");

                    XmppError error = result.getParcelableExtra(XmppApi.RESULT_ERROR);
                    handleError(error);
                    break;
                }
            }
        }
    }

    public void cleartextSign(Intent data) {
        data.setAction(XmppApi.ACTION_CLEARTEXT_SIGN);
        data.putExtra(XmppApi.EXTRA_SIGN_KEY_ID, mSignKeyId);

        InputStream is = getInputstream(false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, os, new MyCallback(true, os, REQUEST_CODE_CLEARTEXT_SIGN));
    }

    public void detachedSign(Intent data) {
        data.setAction(XmppApi.ACTION_DETACHED_SIGN);
        data.putExtra(XmppApi.EXTRA_SIGN_KEY_ID, mSignKeyId);

        InputStream is = getInputstream(false);
        // no output stream needed, detached signature is returned as RESULT_DETACHED_SIGNATURE

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, null, new MyCallback(true, null, REQUEST_CODE_DETACHED_SIGN));
    }

    public void encrypt(Intent data) {
        data.setAction(XmppApi.ACTION_ENCRYPT);
        if (!TextUtils.isEmpty(mEncryptUserIds.getText().toString())) {
            data.putExtra(XmppApi.EXTRA_USER_IDS, mEncryptUserIds.getText().toString().split(","));
        }
        data.putExtra(XmppApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, os, new MyCallback(true, os, REQUEST_CODE_ENCRYPT));
    }

    public void signAndEncrypt(Intent data) {
        data.setAction(XmppApi.ACTION_SIGN_AND_ENCRYPT);
        data.putExtra(XmppApi.EXTRA_SIGN_KEY_ID, mSignKeyId);
        if (!TextUtils.isEmpty(mEncryptUserIds.getText().toString())) {
            data.putExtra(XmppApi.EXTRA_USER_IDS, mEncryptUserIds.getText().toString().split(","));
        }
        data.putExtra(XmppApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, os, new MyCallback(true, os, REQUEST_CODE_SIGN_AND_ENCRYPT));
    }

    public void decryptAndVerify(Intent data) {
        data.setAction(XmppApi.ACTION_DECRYPT_VERIFY);

        InputStream is = getInputstream(true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, os, new MyCallback(false, os, REQUEST_CODE_DECRYPT_AND_VERIFY));
    }

    public void decryptAndVerifyDetached(Intent data) {
        data.setAction(XmppApi.ACTION_DECRYPT_VERIFY);
        data.putExtra(XmppApi.EXTRA_DETACHED_SIGNATURE, mDetachedSignature.getText().toString().getBytes());

        // use from text from mMessage
        InputStream is = getInputstream(false);

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, null, new MyCallback(false, null, REQUEST_CODE_DECRYPT_AND_VERIFY_DETACHED));
    }

    public void getKey(Intent data) {
        data.setAction(XmppApi.ACTION_GET_KEY);
        data.putExtra(XmppApi.EXTRA_KEY_ID, Long.decode(mGetKeyEdit.getText().toString()));

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, null, null, new MyCallback(false, null, REQUEST_CODE_GET_KEY));
    }

    public void getKeyIds(Intent data) {
        data.setAction(XmppApi.ACTION_GET_KEY_IDS);
        data.putExtra(XmppApi.EXTRA_USER_IDS, mGetKeyIdsEdit.getText().toString().split(","));

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, null, null, new MyCallback(false, null, REQUEST_CODE_GET_KEY_IDS));
    }

    public void getAnyKeyIds(Intent data) {
        data.setAction(XmppApi.ACTION_GET_KEY_IDS);

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, null, null, new MyCallback(false, null, REQUEST_CODE_GET_KEY_IDS));
    }

    public void backup(Intent data) {
        data.setAction(XmppApi.ACTION_BACKUP);
        data.putExtra(XmppApi.EXTRA_KEY_IDS, new long[]{Long.decode(mGetKeyEdit.getText().toString())});
        data.putExtra(XmppApi.EXTRA_BACKUP_SECRET, true);
        data.putExtra(XmppApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        XmppApi api = new XmppApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, null, os, new MyCallback(true, os, REQUEST_CODE_BACKUP));
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
                case REQUEST_CODE_CLEARTEXT_SIGN: {
                    cleartextSign(data);
                    break;
                }
                case REQUEST_CODE_DETACHED_SIGN: {
                    detachedSign(data);
                    break;
                }
                case REQUEST_CODE_ENCRYPT: {
                    encrypt(data);
                    break;
                }
                case REQUEST_CODE_SIGN_AND_ENCRYPT: {
                    signAndEncrypt(data);
                    break;
                }
                case REQUEST_CODE_DECRYPT_AND_VERIFY: {
                    decryptAndVerify(data);
                    break;
                }
                case REQUEST_CODE_DECRYPT_AND_VERIFY_DETACHED: {
                    decryptAndVerifyDetached(data);
                    break;
                }
                case REQUEST_CODE_GET_KEY: {
                    getKey(data);
                    break;
                }
                case REQUEST_CODE_GET_KEY_IDS: {
                    getKeyIds(data);
                    break;
                }
                case REQUEST_CODE_BACKUP: {
                    backup(data);
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null) {
            mServiceConnection.unbindFromService();
        }
    }

}
