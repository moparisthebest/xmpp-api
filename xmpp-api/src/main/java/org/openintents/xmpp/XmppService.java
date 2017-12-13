package org.openintents.xmpp;

import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public class XmppService implements IXmppService, IExecuteService {

    private final IXmppService delegate;

    private XmppService(final IXmppService delegate) {
        this.delegate = delegate;
    }

    public static XmppService wrap(final IXmppService delegate) {
        return delegate == null ? null : new XmppService(delegate);
    }

    public IXmppService getDelegate() {
        return delegate;
    }

    @Override
    public ParcelFileDescriptor createOutputPipe(final int pipeId) throws RemoteException {
        return delegate.createOutputPipe(pipeId);
    }

    @Override
    public Intent execute(final Intent data, final ParcelFileDescriptor input, final int pipeId) throws RemoteException {
        return delegate.execute(data, input, pipeId);
    }

    @Override
    public Intent callback(final Intent data, final IXmppPluginCallback callback) throws RemoteException {
        return delegate.callback(data, callback);
    }

    @Override
    public IBinder asBinder() {
        return delegate.asBinder();
    }
}
