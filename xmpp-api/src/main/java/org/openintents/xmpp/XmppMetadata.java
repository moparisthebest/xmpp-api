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

package org.openintents.xmpp;

import android.os.Parcel;
import android.os.Parcelable;

public class XmppMetadata implements Parcelable {
    /**
     * Since there might be a case where new versions of the client using the library getting
     * old versions of the protocol (and thus old versions of this class), we need a versioning
     * system for the parcels sent between the clients and the providers.
     */
    public static final int PARCELABLE_VERSION = 1;

    String filename;
    String mimeType;
    String charset;
    long modificationTime;
    long originalSize;

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getModificationTime() {
        return modificationTime;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public String getCharset() {
        return charset;
    }

    public XmppMetadata() {
    }

    public XmppMetadata(String filename, String mimeType, long modificationTime,
            long originalSize, String charset) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.modificationTime = modificationTime;
        this.originalSize = originalSize;
        this.charset = charset;
    }

    public XmppMetadata(String filename, String mimeType, long modificationTime,
                           long originalSize) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.modificationTime = modificationTime;
        this.originalSize = originalSize;
    }

    public XmppMetadata(XmppMetadata b) {
        this.filename = b.filename;
        this.mimeType = b.mimeType;
        this.modificationTime = b.modificationTime;
        this.originalSize = b.originalSize;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        /**
         * NOTE: When adding fields in the process of updating this API, make sure to bump
         * {@link #PARCELABLE_VERSION}.
         */
        dest.writeInt(PARCELABLE_VERSION);
        // Inject a placeholder that will store the parcel size from this point on
        // (not including the size itself).
        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();
        // version 1
        dest.writeString(filename);
        dest.writeString(mimeType);
        dest.writeLong(modificationTime);
        dest.writeLong(originalSize);
        // version 2
        dest.writeString(charset);
        // Go back and write the size
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<XmppMetadata> CREATOR = new Creator<XmppMetadata>() {
        public XmppMetadata createFromParcel(final Parcel source) {
            final int version = source.readInt(); // parcelableVersion
            final int parcelableSize = source.readInt();
            final int startPosition = source.dataPosition();

            XmppMetadata vr = new XmppMetadata();
            vr.filename = source.readString();
            vr.mimeType = source.readString();
            vr.modificationTime = source.readLong();
            vr.originalSize = source.readLong();
            vr.charset = source.readString();

            // skip over all fields added in future versions of this parcel
            source.setDataPosition(startPosition + parcelableSize);

            return vr;
        }

        public XmppMetadata[] newArray(final int size) {
            return new XmppMetadata[size];
        }
    };

    @Override
    public String toString() {
        String out = "\nfilename: " + filename;
        out += "\nmimeType: " + mimeType;
        out += "\nmodificationTime: " + modificationTime;
        out += "\noriginalSize: " + originalSize;
        out += "\ncharset: " + charset;
        return out;
    }

}
