package org.openintents.xmpp;

import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public class XmppPluginCallback implements IXmppPluginCallback, IExecuteService {

    private final IXmppPluginCallback delegate;

    private XmppPluginCallback(final IXmppPluginCallback delegate) {
        this.delegate = delegate;
    }
    
    public static XmppPluginCallback wrap(final IXmppPluginCallback delegate) {
        return delegate == null ? null : new XmppPluginCallback(delegate);
    }

    public IXmppPluginCallback getDelegate() {
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
    public IBinder asBinder() {
        return delegate.asBinder();
    }
}
