package org.openintents.xmpp;

import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public abstract class AbstractXmppService extends IXmppService.Stub implements IExecuteService, ExecuteService.Execute {

    private final ExecuteService executeService = new ExecuteService(this);

    @Override
    public ParcelFileDescriptor createOutputPipe(final int pipeId) throws RemoteException {
        return executeService.createOutputPipe(pipeId);
    }

    @Override
    public Intent execute(final Intent data, final ParcelFileDescriptor input, final int pipeId) throws RemoteException {
        return executeService.execute(data, input, pipeId);
    }
}
