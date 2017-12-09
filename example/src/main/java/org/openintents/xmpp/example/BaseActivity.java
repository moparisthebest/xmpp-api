/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import org.openintents.xmpp.util.XmppAppPreference;
import org.openintents.xmpp.util.XmppAccountPreference;

public class BaseActivity extends PreferenceActivity {
    XmppAccountPreference mKey;
    XmppAppPreference mProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load preferences from xml
        addPreferencesFromResource(R.xml.base_preference);

        // find preferences
        Preference xmppApi = findPreference("xmpp_provider_demo");
        mProvider = (XmppAppPreference) findPreference("xmpp_provider_list");
        mKey = (XmppAccountPreference) findPreference("xmpp_key");

        xmppApi.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(BaseActivity.this, XmppApiActivity.class));

                return false;
            }
        });

        mKey.setXmppProvider(mProvider.getValue());
        mProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mKey.setXmppProvider((String) newValue);
                return true;
            }
        });
        mKey.setDefaultUserId("Alice <alice@example.com>");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mKey.handleOnActivityResult(requestCode, resultCode, data)) {
            // handled by XmppKeyPreference
            return;
        }
        // other request codes...
    }
}
