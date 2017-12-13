package org.openintents.xmpp;

import android.content.Intent;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import org.openintents.xmpp.util.XmppServiceApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ExecuteService {

    public interface Execute {
        /**
         * Execute without worrying about closing streams
         * @param data intent, never null
         * @param inputStream may be null, automatically closed
         * @param outputStream may be null, automatically closed
         * @return result
         */
        Intent execute(Intent data, InputStream inputStream, OutputStream outputStream);
    }

    private final Execute execute;
    private final Map<Long, ParcelFileDescriptor> mOutputPipeMap = new HashMap<Long, ParcelFileDescriptor>();

    public ExecuteService(final Execute execute) {
        if(execute == null)
            throw new NullPointerException("execute must be non-null");
        this.execute = execute;
    }

    private long createKey(int id) {
        int callingPid = Binder.getCallingPid();
        return ((long) callingPid << 32) | ((long) id & 0xFFFFFFFL);
    }

    public ParcelFileDescriptor createOutputPipe(int outputPipeId) throws RemoteException {
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            mOutputPipeMap.put(createKey(outputPipeId), pipe[1]);
            return pipe[0];
        } catch (IOException e) {
            Log.e(XmppServiceApi.TAG, "IOException in ExecuteService", e);
            return null;
        }

    }

    public Intent execute(Intent data, ParcelFileDescriptor input, int outputPipeId) throws RemoteException {
        long key = createKey(outputPipeId);
        ParcelFileDescriptor output = mOutputPipeMap.get(key);
        mOutputPipeMap.remove(key);
        return executeInternal(data, input, output);
    }

    protected Intent executeInternal(final Intent data, final ParcelFileDescriptor input, final ParcelFileDescriptor output) {

        OutputStream outputStream =
                (output != null) ? new ParcelFileDescriptor.AutoCloseOutputStream(output) : null;
        InputStream inputStream =
                (input != null) ? new ParcelFileDescriptor.AutoCloseInputStream(input) : null;

        try {
            return execute.execute(data, inputStream, outputStream);
        } finally {
            // always close input and output file descriptors even in createErrorPendingIntent cases
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(XmppServiceApi.TAG, "IOException when closing input ParcelFileDescriptor", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(XmppServiceApi.TAG, "IOException when closing output ParcelFileDescriptor", e);
                }
            }
        }
    }
}
