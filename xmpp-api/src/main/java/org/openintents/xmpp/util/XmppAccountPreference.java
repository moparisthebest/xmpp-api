/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import org.openintents.xmpp.IXmppService;
import org.openintents.xmpp.XmppError;
import org.openintents.xmpp.R;

public class XmppAccountPreference extends Preference {
    private long mAccountId;
    private String mXmppProvider;
    private XmppServiceConnection mServiceConnection;
    private String mDefaultUserId;

    public static final int REQUEST_CODE_ACCOUNT_PREFERENCE = 9999;

    private static final int NO_ACCOUNT = 0;

    public XmppAccountPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        return (mAccountId == NO_ACCOUNT) ? getContext().getString(R.string.xmpp_no_account_selected)
                : getContext().getString(R.string.xmpp_account_selected);
    }

    private void updateEnabled() {
        if (TextUtils.isEmpty(mXmppProvider)) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }

    public void setXmppProvider(String packageName) {
        mXmppProvider = packageName;
        updateEnabled();
    }

    public void setDefaultUserId(String userId) {
        mDefaultUserId = userId;
    }

    @Override
    protected void onClick() {
        // bind to service
        mServiceConnection = new XmppServiceConnection(
                getContext().getApplicationContext(),
                mXmppProvider,
                new XmppServiceConnection.OnBound() {
                    @Override
                    public void onBound(IXmppService service) {

                        getAccountId(new Intent());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(XmppApi.TAG, "exception on binding!", e);
                    }
                }
        );
        mServiceConnection.bindToService();
    }

    private void getAccountId(Intent data) {
        data.setAction(XmppApi.ACTION_GET_ACCOUNT_ID);
        data.putExtra(XmppApi.EXTRA_USER_ID, mDefaultUserId);

        XmppApi api = new XmppApi(getContext(), mServiceConnection.getService());
        api.executeApiAsync(data, null, null, new MyCallback(REQUEST_CODE_ACCOUNT_PREFERENCE));
    }

    private class MyCallback implements XmppApi.IXmppCallback {
        int requestCode;

        private MyCallback(int requestCode) {
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            switch (result.getIntExtra(XmppApi.RESULT_CODE, XmppApi.RESULT_CODE_ERROR)) {
                case XmppApi.RESULT_CODE_SUCCESS: {

                    long keyId = result.getLongExtra(XmppApi.EXTRA_SIGN_KEY_ID, NO_ACCOUNT);
                    save(keyId);

                    break;
                }
                case XmppApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {

                    PendingIntent pi = result.getParcelableExtra(XmppApi.RESULT_INTENT);
                    try {
                        Activity act = (Activity) getContext();
                        act.startIntentSenderFromChild(
                                act, pi.getIntentSender(),
                                requestCode, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(XmppApi.TAG, "SendIntentException", e);
                    }
                    break;
                }
                case XmppApi.RESULT_CODE_ERROR: {
                    XmppError error = result.getParcelableExtra(XmppApi.RESULT_ERROR);
                    Log.e(XmppApi.TAG, "RESULT_CODE_ERROR: " + error.getMessage());

                    break;
                }
            }
        }
    }

    private void save(long newValue) {
        // Give the client a chance to ignore this change if they deem it
        // invalid
        if (!callChangeListener(newValue)) {
            // They don't want the value to be set
            return;
        }

        setAndPersist(newValue);
    }

    /**
     * Public API
     */
    public void setValue(long accountId) {
        setAndPersist(accountId);
    }

    /**
     * Public API
     */
    public long getValue() {
        return mAccountId;
    }

    private void setAndPersist(long newValue) {
        mAccountId = newValue;

        // Save to persistent storage (this method will make sure this
        // preference should be persistent, along with other useful checks)
        persistLong(mAccountId);

        // Data has changed, notify so UI can be refreshed!
        notifyChanged();

        // also update summary
        setSummary(getSummary());
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // This preference type's value type is Long, so we read the default
        // value from the attributes as an Integer.
        return (long) a.getInteger(index, NO_ACCOUNT);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            // Restore state
            mAccountId = getPersistedLong(mAccountId);
        } else {
            // Set state
            long value = (Long) defaultValue;
            setAndPersist(value);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */

        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.accountId = mAccountId;
        myState.xmppProvider = mXmppProvider;
        myState.defaultUserId = mDefaultUserId;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mAccountId = myState.accountId;
        mXmppProvider = myState.xmppProvider;
        mDefaultUserId = myState.defaultUserId;
        notifyChanged();
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p/>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        long accountId;
        String xmppProvider;
        String defaultUserId;

        public SavedState(Parcel source) {
            super(source);

            accountId = source.readInt();
            xmppProvider = source.readString();
            defaultUserId = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeLong(accountId);
            dest.writeString(xmppProvider);
            dest.writeString(defaultUserId);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_PREFERENCE && resultCode == Activity.RESULT_OK) {
            getAccountId(data);
            return true;
        } else {
            return false;
        }
    }

}