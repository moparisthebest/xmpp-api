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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.openintents.xmpp.IXmppService;

public class XmppServiceConnection {

    // callback interface
    public interface OnBound {
        public void onBound(XmppServiceApi serviceApi);

        public void onError(Exception e);
    }

    private Context context;

    private XmppServiceApi serviceApi;
    private String mProviderPackageName;

    private OnBound mOnBoundListener;

    /**
     * Create new connection
     *
     * @param context
     * @param providerPackageName specify package name of XMPP provider,
     *                            e.g., "eu.siacs.conversations"
     */
    public XmppServiceConnection(Context context, String providerPackageName) {
        this.context = context;
        this.mProviderPackageName = providerPackageName;
    }

    /**
     * Create new connection with callback
     *
     * @param context
     * @param providerPackageName specify package name of XMPP provider,
     *                            e.g., "eu.siacs.conversations"
     * @param onBoundListener     callback, executed when connection to service has been established
     */
    public XmppServiceConnection(Context context, String providerPackageName,
                                    OnBound onBoundListener) {
        this(context, providerPackageName);
        this.mOnBoundListener = onBoundListener;
    }

    public XmppServiceApi getApi() {
        return serviceApi;
    }

    public boolean isBound() {
        return (serviceApi != null);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceApi = new XmppServiceApi(context, IXmppService.Stub.asInterface(service));
            if (mOnBoundListener != null) {
                mOnBoundListener.onBound(serviceApi);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            serviceApi = null;
        }
    };

    /**
     * If not already bound, bind to service!
     *
     * @return
     */
    public void bindToService() {
        // if not already bound...
        if (serviceApi == null) {
            try {
                Intent serviceIntent = new Intent(XmppServiceApi.SERVICE_INTENT);
                // NOTE: setPackage is very important to restrict the intent to this provider only!
                serviceIntent.setPackage(mProviderPackageName);
                boolean connect = context.getApplicationContext().bindService(serviceIntent, serviceConnection,
                        Context.BIND_AUTO_CREATE);
                if (!connect) {
                    throw new Exception("bindService() returned false!");
                }
            } catch (Exception e) {
                if (mOnBoundListener != null) {
                    mOnBoundListener.onError(e);
                }
            }
        } else {
            // already bound, but also inform client about it with callback
            if (mOnBoundListener != null) {
                mOnBoundListener.onBound(serviceApi);
            }
        }
    }

    public void unbindFromService() {
        context.getApplicationContext().unbindService(serviceConnection);
    }

}
