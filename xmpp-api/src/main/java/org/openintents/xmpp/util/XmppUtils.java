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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import org.openintents.xmpp.XmppError;

import static org.openintents.xmpp.util.XmppPluginCallbackApi.*;

public class XmppUtils {

    public static boolean isAvailable(Context context) {
        Intent intent = new Intent(XmppServiceApi.SERVICE_INTENT);
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentServices(intent, 0);
        return !resInfo.isEmpty();
    }

    public static Intent getSuccess() {
        final Intent result = new Intent();
        result.putExtra(RESULT_CODE, RESULT_CODE_SUCCESS);
        return result;
    }

    public static Intent getError(final int errorId, final String message) {
        final Intent result = new Intent();
        final XmppError error = new XmppError(errorId, message);
        result.putExtra(RESULT_ERROR, error);
        result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
        return result;
    }

    public static Intent getExceptionError(final Exception e) {
        final Intent result = new Intent();
        result.putExtra(RESULT_ERROR, new XmppError(XmppError.GENERIC_ERROR, e));
        result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
        return result;
    }

}
