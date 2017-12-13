package org.openintents.xmpp;

interface IXmppPluginCallback {

    /**
     * see org.openintents.xmpp.util.XmppApi for documentation
     */
    ParcelFileDescriptor createOutputPipe(in int pipeId);

    /**
     * see org.openintents.xmpp.util.XmppApi for documentation
     */
    Intent execute(in Intent data, in ParcelFileDescriptor input, int pipeId);
}