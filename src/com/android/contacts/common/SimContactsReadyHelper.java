/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.contacts.common;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.IccProvider;

public class SimContactsReadyHelper {
    private final String ACTION_QUERY_SIM_CAPABILITY_INFO = "com.android.contacts.QUERY_SIM_CAPABILITY_INFO";
    private static final String TAG = SimContactsReadyHelper.class.getSimpleName();
    private static final boolean DBG = true;
    private static String mSimCapabilityInfo[] = {"",""};
    private Context mContext;
    private static final Object mLock = new Object();

    private static final String[] INFO_COLUMN_NAMES = new String[] {
        IccProvider.ICC_ADN_TOTAL, IccProvider.ICC_ADN_USED, IccProvider.ICC_ADN_LEN, IccProvider.ICC_ADN_DIGIT_LEN,
        IccProvider.ICC_EMAIL_TOTAL,IccProvider.ICC_EMAIL_USED, IccProvider.ICC_EMAIL_LEN,
        IccProvider.ICC_ANR_TOTAL,IccProvider.ICC_ANR_USED,IccProvider.ICC_ANR_LEN,
        IccProvider.ICC_GROUP_TOTAL,IccProvider.ICC_GROUP_USED,IccProvider.ICC_GROUP_LEN
    };

    private static final int SIM_CAPABILITY_INFO_TOTAL_COLUMN = 14;
    private static final int SIM_CONTACTS_LOADED = 0;
    private static final int SIM_CAPABILITY_INFO_ADN_TOTAL = 1;
    private static final int SIM_CAPABILITY_INFO_ADN_USED = 2;
    private static final int SIM_CAPABILITY_INFO_ADN_LEN = 3;
    private static final int SIM_CAPABILITY_INFO_ADN_DIGIT_LEN = 4;
    private static final int SIM_CAPABILITY_INFO_EMAIL_TOTAL = 5;
    private static final int SIM_CAPABILITY_INFO_EMAIL_USED = 6;
    private static final int SIM_CAPABILITY_INFO_EMAIL_LEN = 7;
    private static final int SIM_CAPABILITY_INFO_ANR_TOTAL = 8;
    private static final int SIM_CAPABILITY_INFO_ANR_USED = 9;
    private static final int SIM_CAPABILITY_INFO_ANR_LEN = 10;
    private static final int SIM_CAPABILITY_INFO_GRP_TOTAL = 11;
    private static final int SIM_CAPABILITY_INFO_GRP_USED = 12;
    private static final int SIM_CAPABILITY_INFO_GRP_LEN = 13;


    public SimContactsReadyHelper(Context context, boolean query) {
        mContext = context;
        //if(DBG) Log.d(TAG, "=>SimContactsReadyHelper(): sim1 = " + mSimCapabilityInfo[0] + ", sim2 = " + mSimCapabilityInfo[1]);

        if (query) {
            querySimContactsLoadedStatus();
        }
    }

    public void querySimContactsLoadedStatus() {
        if(DBG) Log.d(TAG, "=>querySimContactsLoadedStatus()");
        Intent intent = new Intent(ACTION_QUERY_SIM_CAPABILITY_INFO);
        if (null != mContext) {
            mContext.sendBroadcast(intent);
       }
    }
    public String getIccCardCapabilityInfo(ContentResolver resolver, SimCardID simId) {
        Uri uri;
        Cursor iccInfoCursor;
        String iccInfo;
        StringBuilder buf = new StringBuilder();

        if (getSimContactsLoaded(simId.toInt())) {
            buf.append(1);
        } else {
            buf.append(0);
        }

        if (SimCardID.ID_ONE == simId) {
            uri = Uri.parse("content://icc2/adn/info");
        } else {
            uri = Uri.parse("content://icc/adn/info");
        }

       iccInfoCursor = resolver.query(uri, INFO_COLUMN_NAMES, null, null, null);
        if (null != iccInfoCursor) {
            if (iccInfoCursor.moveToFirst()) {
                for (int i=0;i<13;i++) {
                    String infoStr = "";
                    int num = 0;
                    try {
                        infoStr = iccInfoCursor.getString(i);
                        num = Integer.valueOf(infoStr);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "getIccCardCapabilityInfo(): NumberFormatException. " + i + ": " + infoStr);
                        num = 0;
                    }

                    buf.append(",");
                    buf.append(num);
                }
            }

            if (!iccInfoCursor.isClosed()) {
                iccInfoCursor.close();
            }
            iccInfoCursor = null;
        }

        iccInfo = buf.toString();

        if (DBG) Log.d(TAG,"getIccCardCapabilityInfo(): simId = " + simId + ", iccInfo = " + iccInfo);

        return iccInfo;
    }


    public boolean getSimContactsLoaded(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return false;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CONTACTS_LOADED]))) {
                Log.e(TAG, "getSimContactsLoaded(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return false;
            }

            int loaded = 0;
            try {
                loaded = Integer.valueOf(lines[SIM_CONTACTS_LOADED]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "getSimContactsLoaded(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                loaded = 0;
            }

            return (0 != loaded);
        }
    }

    public void setSimCapabilityInfo(String sim1CapabilityInfo, String sim2CapabilityInfo) {
        synchronized (mLock) {
                //if(DBG) Log.d(TAG, "=>setSimContactsLoaded("+sim1CapabilityInfo+", "+sim2CapabilityInfo+")");
                mSimCapabilityInfo[0] = sim1CapabilityInfo;
                mSimCapabilityInfo[1] = sim2CapabilityInfo;
        }
    }

    public void setSim1CapabilityInfo(String sim1CapabilityInfo) {
            mSimCapabilityInfo[0] = sim1CapabilityInfo;
    }

    public void setSim2CapabilityInfo(String sim2CapabilityInfo) {
            mSimCapabilityInfo[1] = sim2CapabilityInfo;
    }

    public boolean isSimCapabilityInfoReady(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return false;
            } else {
                String[] lines = mSimCapabilityInfo[simId].split(",");
                if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                    || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_TOTAL]))
                    || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_USED]))
                    || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_LEN]))
                    || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_DIGIT_LEN]))) {
                    return false;
                } else {
                    return true;
                }
            }
        }
    }

    public int getFreeADNCount(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return 0;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_USED]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_LEN]))) {
                Log.e(TAG, "getFreeADNCount(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }

            try {
                final int total = Integer.valueOf(lines[SIM_CAPABILITY_INFO_ADN_TOTAL]);
                final int used = Integer.valueOf(lines[SIM_CAPABILITY_INFO_ADN_USED]);
                final int free =total-used;
                if(DBG) Log.d(TAG, "=>getFreeADNCount() free= "+ free);
                return free;
            } catch (NumberFormatException e) {
                Log.e(TAG, "getFreeADNCount(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }
        }
    }

    public int getAdnStrMaxLen(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return 0;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_LEN]))) {
                Log.e(TAG, "getAdnStrMaxLen(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }

            try {
                final int AdnMaxStrLen = Integer.valueOf(lines[SIM_CAPABILITY_INFO_ADN_LEN]);

                if(DBG) Log.d(TAG, "=>getAdnStrMaxLen() AdnMaxStrLen= "+ AdnMaxStrLen);
                return AdnMaxStrLen;
            } catch (NumberFormatException e) {
                Log.e(TAG, "getAdnStrMaxLen(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }
        }
    }

    public int getAdnDigitMaxLen(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return 0;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ADN_DIGIT_LEN]))) {
                Log.e(TAG, "getAdnDigitMaxLen(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }

            try {
                final int AdnDigitMaxLen = Integer.valueOf(lines[SIM_CAPABILITY_INFO_ADN_DIGIT_LEN]);

                if(DBG) Log.d(TAG, "=>getAdnDigitMaxLen() AdnDigitMaxLen= "+ AdnDigitMaxLen);
                return AdnDigitMaxLen;
            } catch (NumberFormatException e) {
                Log.e(TAG, "getAdnDigitMaxLen(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }
        }
    }

    public int getFreeGASCount(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return 0;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_GRP_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_GRP_USED]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_GRP_LEN]))) {
                Log.e(TAG, "getFreeGASCount(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }

            try {
                final int total = Integer.valueOf(lines[SIM_CAPABILITY_INFO_GRP_TOTAL]);
                final int used = Integer.valueOf(lines[SIM_CAPABILITY_INFO_GRP_USED]);
                final int free =total-used;
                if(DBG) Log.d(TAG, "=>getFreeGASCount() free= "+ free);
                return free;
            } catch (NumberFormatException e) {
                Log.e(TAG, "getFreeGASCount(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }
        }
    }

    public int getFreeEmailCount(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return 0;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_EMAIL_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_EMAIL_USED]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_EMAIL_LEN]))) {
                Log.e(TAG, "getFreeEmailCount(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }

            try {
                final int total = Integer.valueOf(lines[SIM_CAPABILITY_INFO_EMAIL_TOTAL]);
                final int used = Integer.valueOf(lines[SIM_CAPABILITY_INFO_EMAIL_USED]);
                final int free =total-used;
                if(DBG) Log.d(TAG, "=>getFreeEmailCount() free= "+ free);
                return free;
            } catch (NumberFormatException e) {
                Log.e(TAG, "getFreeEmailCount(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }
        }
    }

    public int getEmailStrMaxLen(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return 0;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_EMAIL_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_EMAIL_LEN]))) {
                Log.e(TAG, "getEmailStrMaxLen(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }

            try {
                final int emailStrMaxLen = Integer.valueOf(lines[SIM_CAPABILITY_INFO_EMAIL_LEN]);

                if(DBG) Log.d(TAG, "=>getEmailStrMaxLen() emailStrMaxLen= "+ emailStrMaxLen);
                return emailStrMaxLen;
            } catch (NumberFormatException e) {
                Log.e(TAG, "getEmailStrMaxLen(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }
        }
    }

    public boolean supportEmail(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return false;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_EMAIL_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_EMAIL_USED]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_EMAIL_LEN]))) {
                Log.e(TAG, "supportsEmail(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return false;
            }

            try {
                final int total = Integer.valueOf(lines[SIM_CAPABILITY_INFO_EMAIL_TOTAL]);
                return (total>0);
            } catch (NumberFormatException e) {
                Log.e(TAG, "supportsEmail(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return false;
            }
        }
    }

    public int getFreeANRCount(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return 0;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ANR_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ANR_USED]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ANR_LEN]))) {
                Log.e(TAG, "getFreeANRCount(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }

            try {
                final int total = Integer.valueOf(lines[SIM_CAPABILITY_INFO_ANR_TOTAL]);
                final int used = Integer.valueOf(lines[SIM_CAPABILITY_INFO_ANR_USED]);
                final int free =total-used;
                if(DBG) Log.d(TAG, "=>getFreeANRCount() free= "+ free);
                return free;
            } catch (NumberFormatException e) {
                Log.e(TAG, "getFreeANRCount(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }
        }
    }

    public int getAnrDigitMaxLen(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return 0;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ANR_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ANR_LEN]))) {
                Log.e(TAG, "getAnrDigitMaxLen(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }

            try {
                final int anrDigitMaxLen = Integer.valueOf(lines[SIM_CAPABILITY_INFO_ANR_LEN]);

                if(DBG) Log.d(TAG, "=>getAnrDigitMaxLen() anrDigitMaxLen= "+ anrDigitMaxLen);
                return anrDigitMaxLen;
            } catch (NumberFormatException e) {
                Log.e(TAG, "getAnrDigitMaxLen(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return 0;
            }
        }
    }

    public boolean supportAnr(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return false;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ANR_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ANR_USED]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_ANR_LEN]))) {
                Log.e(TAG, "supportAnr(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return false;
            }

            try {
                final int total = Integer.valueOf(lines[SIM_CAPABILITY_INFO_ANR_TOTAL]);
                return (total>0);
            } catch (NumberFormatException e) {
                Log.e(TAG, "supportAnr(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return false;
            }
        }
    }

    public boolean supportGroup(int simId) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(mSimCapabilityInfo[simId])) {
                return false;
            }

            String[] lines = mSimCapabilityInfo[simId].split(",");
            if ((SIM_CAPABILITY_INFO_TOTAL_COLUMN != lines.length)
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_GRP_TOTAL]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_GRP_USED]))
                || (TextUtils.isEmpty(lines[SIM_CAPABILITY_INFO_GRP_LEN]))) {
                Log.e(TAG, "supportGroup(): Error parsing: info["+simId+"] = " + mSimCapabilityInfo[simId]);
                return false;
            }

            try {
                final int total = Integer.valueOf(lines[SIM_CAPABILITY_INFO_GRP_TOTAL]);
                return (total>0);
            } catch (NumberFormatException e) {
                Log.e(TAG, "supportGroup(): NumberFormatException: mSimCapabilityInfo["+simId+"] = " + mSimCapabilityInfo[simId]);
                return false;
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("0: ");
        sb.append(mSimCapabilityInfo[0]);
        sb.append(", 1: ");
        sb.append(mSimCapabilityInfo[1]);
        return sb.toString();
    }
}
