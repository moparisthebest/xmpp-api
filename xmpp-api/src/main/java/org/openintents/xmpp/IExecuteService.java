package org.openintents.xmpp;

import android.content.Intent;
import android.os.ParcelFileDescriptor;

import java.io.InputStream;
import java.io.OutputStream;

public interface IExecuteService {
    /**
     * see org.openintents.xmpp.util.XmppApi for documentation
     */
    ParcelFileDescriptor createOutputPipe(int pipeId) throws android.os.RemoteException;

    /**
     * see org.openintents.xmpp.util.XmppApi for documentation
     */
    Intent execute(Intent data, ParcelFileDescriptor input, int pipeId) throws android.os.RemoteException;
}
