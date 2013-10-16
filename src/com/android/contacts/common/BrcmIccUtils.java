/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2009-2011 Broadcom Corporation
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
 * limitations under the License
 */

package com.android.contacts.common;

import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.contacts.common.SimContactsReadyHelper;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import com.android.internal.telephony.IccProvider;

public class BrcmIccUtils {
    private static final String TAG = "BrcmIccUtils";
    private static final boolean DBG = true;
    public static final String ACCOUNT_TYPE_SIM = "com.android.contacts.sim";
    public static final String ACCOUNT_TYPE_LOCAL = "com.android.contacts.local";
    public static final String ACCOUNT_NAME_SIM1 = "SIM1";
    public static final String ACCOUNT_NAME_SIM2 = "SIM2";
    public static final String INTENT_EXTRA_RAWCONTACT_ID = "rawContactId";
    public static final String INTENT_EXTRA_SAVE_TO_PB = "saveToPhonebook";
    public static final String INTENT_EXTRA_SAVE_TO_SIM = "saveToSIM";
    public static final String INTENT_EXTRA_SIM_ID = "simId";
    public static final String EDIT_ADN_ACTIVITY_PACKAGE_NAME = "com.android.contacts";
    public static final String EDIT_ADN_ACTIVITY_CLASS_NAME = "com.android.contacts.EditAdnContactActivity";
    public static final String PHONEBOOK_CONTACTS_ONLY = "phonebook_contacts_only";
    public static final String EXPORT_To_SIM = "exportToSIM";
    public static final String SIM_ACTION = "simaction";
    public static final String INTENT_EXTRA_NAME = "tag";
    public static final String INTENT_EXTRA_NUMBER = "number";
    public static final String INTENT_EXTRA_EMAILS = "emails";
    public static final String INTENT_EXTRA_ANRS = "anrs";
    private static final Uri BLACK_NAMES_CONTENT_URI = Uri.parse("content://com.broadcom.blackname/blacknames");
    public static String getRawContactAccountTypeFromContactId(ContentResolver resolver, long contactId) {
        Cursor cursor = null;
        String accountType = null;
        try {
            cursor = resolver.query(RawContacts.CONTENT_URI,
                                    new String[] {RawContacts.ACCOUNT_TYPE},
                                    RawContacts.CONTACT_ID + "=" + contactId,
                                    null,
                                    null);
            if (cursor != null && cursor.moveToFirst()) {
                accountType = cursor.getString(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return accountType;
    }

//    public static String getRawContactAccountTypeFromRawContactId(ContentResolver resolver, long rawContactId) {
//        Cursor cursor = null;
//        String accountType = null;
//        try {
//            cursor = resolver.query(RawContacts.CONTENT_URI,
//                                    new String[] {RawContacts.ACCOUNT_TYPE},
//                                    RawContacts._ID + "=" + rawContactId,
//                                    null,
//                                    null);
//            if (cursor != null && cursor.moveToFirst()) {
//                accountType = cursor.getString(0);
//            }
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//        return accountType;
//    }

//    public static long getContactIdFromRawContactId(ContentResolver resolver, long rawContactId) {
//        Cursor cursor = null;
//        long contactId = -1;
//        try {
//            cursor = resolver.query(RawContacts.CONTENT_URI,
//                                    new String[] {RawContacts.CONTACT_ID},
//                                    RawContacts._ID + "=" + rawContactId,
//                                    null,
//                                    null);
//            if (cursor != null && cursor.moveToFirst()) {
//                contactId = cursor.getInt(0);
//            }
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//        return contactId;
//    }

    public static String getRawContactAccountName(ContentResolver resolver, long contactId) {
        Cursor cursor = null;
        String accountName = null;
        try {
            cursor = resolver.query(RawContacts.CONTENT_URI,
                                    new String[] {RawContacts.ACCOUNT_NAME},
                                    RawContacts.CONTACT_ID + "=" + contactId,
                                    null,
                                    null);
            if (cursor != null && cursor.moveToFirst()) {
                accountName = cursor.getString(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return accountName;
    }

    public static long getRawContactId(ContentResolver resolver, long contactId) {
        Cursor cursor = null;
        long rawContactId = -1;
        try {
            cursor = resolver.query(RawContacts.CONTENT_URI,
                                    new String[] {RawContacts._ID},
                                    RawContacts.CONTACT_ID + "=" + contactId,
                                    null,
                                    null);
            if (cursor != null && cursor.moveToFirst()) {
                rawContactId = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return rawContactId;
    }


    public static String[] getIccGroupInfoFromGroupId(ContentResolver resolver, long groupId) {
        String[] groupInfo = new String[4];
        for(int i=0;i<4;i++) {
            groupInfo[i] = "";
        }

        Cursor cursor = null;
        try {
            cursor = resolver.query(Groups.CONTENT_URI,
                                    new String[] {Groups.ACCOUNT_TYPE, Groups.ACCOUNT_NAME, Groups.TITLE, Groups.SOURCE_ID},
                                    Groups._ID + "=? ",
                                    new String[] {String.valueOf(groupId)},
                                    null);

            if (cursor != null && cursor.moveToFirst()) {
                for(int i=0;i<4;i++) {
                    String str = cursor.getString(i);
                    if (!TextUtils.isEmpty(str)) {
                        groupInfo[i] = str;
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (DBG) Log.d(TAG, "getIccGroupInfoFromGroupId(): groupId = " + groupId
                                                      + ", account type = " + groupInfo[0]
                                                      + ", account name = " + groupInfo[1]
                                                      + ", group title = " + groupInfo[2]
                                                      + ", icc group id = " + groupInfo[3]);
        return groupInfo;
    }

    public static String getIccGroupIdFromGroupId(ContentResolver resolver, long groupId) {
        String iccGroupId = "";
        Cursor cursor = null;
        try {
            cursor = resolver.query(Groups.CONTENT_URI,
                                    new String[] {Groups.SOURCE_ID},
                                    Groups._ID + "=? ",
                                    new String[] {String.valueOf(groupId)},
                                    null);

            if (cursor != null && cursor.moveToFirst()) {
                String strIccGroupId = cursor.getString(0);
                if (null != strIccGroupId) {
                    iccGroupId = strIccGroupId;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return iccGroupId;
    }


    // put EditAdnActivity.class, rawContactId and sim id into intent
    public static void prepareEditAdnIntent(ContentResolver resolver, long contactId, Intent intent) {
//        intent.setClassName(EDIT_ADN_ACTIVITY_PACKAGE_NAME, EDIT_ADN_ACTIVITY_CLASS_NAME);
//
//        long rawContactId;
//        rawContactId = getRawContactId(resolver, contactId);
//        intent.putExtra(INTENT_EXTRA_RAWCONTACT_ID, rawContactId);
//
//        String accountName = getRawContactAccountName(resolver, contactId);
//        if (accountName.equals(ACCOUNT_NAME_SIM2)) {
//            intent.putExtra(INTENT_EXTRA_SIM_ID, SimCardID.ID_ONE);
//        } else {
//            intent.putExtra(INTENT_EXTRA_SIM_ID, SimCardID.ID_ZERO);
//        }
    }

    // put EditAdnActivity.class, rawContactId and sim id into intent
//    public static void prepareInsertAdnIntent(SimCardID simId, Intent intent) {
//        intent.setClassName(EDIT_ADN_ACTIVITY_PACKAGE_NAME, EDIT_ADN_ACTIVITY_CLASS_NAME);
//
//        intent.putExtra(INTENT_EXTRA_RAWCONTACT_ID, -1);
//
//        if (SimCardID.ID_ONE == simId) {
//            intent.putExtra(INTENT_EXTRA_SIM_ID, SimCardID.ID_ONE);
//        } else {
//            intent.putExtra(INTENT_EXTRA_SIM_ID, SimCardID.ID_ZERO);
//        }
//    }

    public static ArrayList<Long> getGroupIdsFromRawContactId(ContentResolver resolver, long rawContactId) {
        ArrayList<Long> groupIds = new ArrayList<Long>();
        Cursor c = resolver.query(Data.CONTENT_URI,
                                  new String[] {GroupMembership.GROUP_ROW_ID},
                                  Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? " ,
                                  new String[] { String.valueOf(rawContactId), GroupMembership.CONTENT_ITEM_TYPE},
                                  null);

        if (c != null && c.moveToFirst()) {
            do {
                long group_row_id = c.getLong(0);
                Log.d(TAG,"getGroupIdsFromRawContactId(): group_row_id = " + group_row_id);
                groupIds.add(group_row_id);
            } while(c.moveToNext());
        }

        if (c != null)
            c.close();

        return groupIds;
    }

    public static long[] getContactIdsByAccount(ContentResolver resolver, String accountType,String accountName) {
        ArrayList<Long> contactList = new ArrayList<Long>();
        Cursor c;

        if (accountType!=null && accountName!=null) {
            c = resolver.query(RawContacts.CONTENT_URI,
                                      new String[] {RawContacts.CONTACT_ID},
                                      RawContacts.ACCOUNT_NAME + "=? AND " + RawContacts.ACCOUNT_TYPE + "=? AND " + RawContacts.DELETED + "=0",
                                      new String[] {accountName,accountType},
                                      null);
        }else {
            if (DBG) Log.d(TAG,"getContactIdsByAccount(): LocalAccountType APK not installed" );
            c = resolver.query(RawContacts.CONTENT_URI,
                                      new String[] {RawContacts.CONTACT_ID},
                                      RawContacts.ACCOUNT_NAME + " IS NULL AND " + RawContacts.ACCOUNT_TYPE + " IS NULL AND " + RawContacts.DELETED + "=0",
                                      null,
                                      null);
        }

        if (c != null) {
            if (DBG) Log.d(TAG,"getContactIdsByAccount(): accountName = " + accountName + " accountType = " +accountType);
            while(c.moveToNext()) {
                long ContactId = c.getLong(0);
                if (DBG) Log.d(TAG,"getContactIdsByAccount(): accountName = " + accountName + ", ContactId = " + ContactId);

                if(ContactId!=0) {
                    contactList.add(ContactId);
                }
            }
            c.close();
        }

        int totalContacts = contactList.size();
        if (0 < totalContacts) {
            long[] contactIds = new long[totalContacts];
            for (int i=0;i<totalContacts;i++) {
                contactIds[i] = contactList.get(i);
            }
            return contactIds;
        } else {
            return null;
        }
    }
    public static long[] getRawContactIdsFromGroupId(ContentResolver resolver, long groupId) {
        ArrayList<Long> rawContactList = new ArrayList<Long>();
        Cursor c = resolver.query(Data.CONTENT_URI,
                                  new String[] {Data.RAW_CONTACT_ID},
                                  Data.MIMETYPE + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
                                  new String[] {GroupMembership.CONTENT_ITEM_TYPE, String.valueOf(groupId)},
                                  null);
        if (c != null) {
            while(c.moveToNext()) {
                long rawContactId = c.getLong(0);
                if (DBG) Log.d(TAG,"getRawContactIdsFromGroupId(): groupId = " + groupId + ", rawContactId = " + rawContactId);
                rawContactList.add(rawContactId);
            }
            c.close();
        }

        int totalRawContacts = rawContactList.size();
        if (0 < totalRawContacts) {
            long[] rawContactIds = new long[totalRawContacts];
            for (int i=0;i<totalRawContacts;i++) {
                rawContactIds[i] = rawContactList.get(i);
            }
            return rawContactIds;
        } else {
            return null;
        }
    }

    public static boolean HaveFreeADNSpace(Context context,int simId) {
            SimContactsReadyHelper simReadyHelper;

            simReadyHelper = new SimContactsReadyHelper(context, false);

            if (simId==SimCardID.ID_ONE.toInt()) {
                simReadyHelper.setSim2CapabilityInfo(simReadyHelper.getIccCardCapabilityInfo(context.getContentResolver(), SimCardID.ID_ONE));
            } else {
                simReadyHelper.setSim1CapabilityInfo(simReadyHelper.getIccCardCapabilityInfo(context.getContentResolver(), SimCardID.ID_ZERO));
            }

            if(simReadyHelper!=null && simReadyHelper.getFreeADNCount(simId)<=0) {
                    Log.d(TAG, "HaveFreeADNSpace():no SIM space for ADN");
                    return false;
            }

            return true;
    }

    public static Cursor getPhoneTypesFromRawContactId(ContentResolver resolver,long rawContactId) {

            Cursor c;
            Uri baseUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
            Uri dataUri = Uri.withAppendedPath(baseUri, RawContacts.Data.CONTENT_DIRECTORY);
            int count=0;
            c = resolver.query(dataUri,
                               new String[] {Phone.TYPE},
                               Data.MIMETYPE + "=?",
                               new String[] {Phone.CONTENT_ITEM_TYPE},
                               null);

            return c;
    }

    public static boolean haveSecondaryPhone(ContentResolver resolver,long rawContactId) {
            Cursor c;
            int phoneNums;
            c=getPhoneTypesFromRawContactId(resolver,rawContactId);
            if(c !=null) {
                phoneNums=c.getCount();
                c.close();
                if(phoneNums>=2)
                     return true;
                else
                    return false;
           }
            Log.d(TAG, "haveSecondaryPhone(): c=null");
            return false;
    }

    public static boolean haveEmail(ContentResolver resolver,long rawContactId) {
            Cursor c;
            Uri baseUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
            Uri dataUri = Uri.withAppendedPath(baseUri, RawContacts.Data.CONTENT_DIRECTORY);
            int count=0;
            c = resolver.query(dataUri,
                           new String[] {Email.DATA},
                           Data.MIMETYPE + "=?",
                           new String[] {Email.CONTENT_ITEM_TYPE},
                           null);

            if (c != null) {
                count=c.getCount();
                c.close();
            }

            if(count>0)
                 return true;
            else
                return false;
    }

    public static String[] getIccContactInfo(ContentResolver resolver, long rawContactId, int[] dataId) {
        String[] iccContactInfo = new String[7];

        for(int i=0;i<7;i++) {
            iccContactInfo[i] = "";
        }

        iccContactInfo[0] = ACCOUNT_NAME_SIM1;
        iccContactInfo[6] = "-1";

        if (null != dataId) {
            for(int i=0;i<4;i++) {
                dataId[i] = -1;
            }
        }

        Cursor c;
        int id;

        Uri baseUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        Uri dataUri = Uri.withAppendedPath(baseUri, RawContacts.Data.CONTENT_DIRECTORY);

        c = resolver.query(dataUri,
                           new String[] {Phone.NUMBER,
                                         Phone.IS_PRIMARY,
                                         Phone.TYPE,
                                         Data._ID},
                           Data.MIMETYPE + "=?",
                           new String[] {Phone.CONTENT_ITEM_TYPE},
                           null);

        if (c != null && c.moveToFirst()) {
            String number;
            int primary;
            int type;
            do {
                number = c.getString(0);
                primary = c.getInt(1);
                type = c.getInt(2);
                id = c.getInt(3);
                if (!TextUtils.isEmpty(number)) {
                    if (type == Phone.TYPE_MOBILE) {
                        iccContactInfo[2] = number;
                        if (null != dataId) dataId[1] = id;
                    } else if (type == Phone.TYPE_HOME) {
                        iccContactInfo[4] = number;
                        if (null != dataId) dataId[3] = id;
                    }
                }
            } while(c.moveToNext());
        }

        if (c != null)
            c.close();

        c = resolver.query(dataUri,
                           new String[] {StructuredName.DISPLAY_NAME, Data._ID},
                           Data.MIMETYPE + "=?",
                           new String[] {StructuredName.CONTENT_ITEM_TYPE},
                           null);

        if (c != null && c.moveToFirst()) {
            String name;
            name = c.getString(0);
            id = c.getInt(1);
            if (!TextUtils.isEmpty(name)) {
                iccContactInfo[1] = name;
                if (null != dataId) dataId[0] = id;
            }
        }

        if (c != null)
            c.close();

        c = resolver.query(dataUri,
                           new String[] {Email.DATA, Data._ID},
                           Data.MIMETYPE + "=?",
                           new String[] {Email.CONTENT_ITEM_TYPE},
                           null);

        if (c != null && c.moveToFirst()) {
            String email;
            email = c.getString(0);
            id = c.getInt(1);
            if (!TextUtils.isEmpty(email)) {
                iccContactInfo[3] = email;
                if (null != dataId) dataId[2] = id;
            }
        }

        if (c != null)
            c.close();

        c = resolver.query(RawContacts.CONTENT_URI,
                           new String[] {RawContacts.ACCOUNT_TYPE,
                                         RawContacts.ACCOUNT_NAME,
                                         RawContacts.SOURCE_ID},
                           RawContacts._ID + "=?",
                           new String[]{String.valueOf(rawContactId)},
                           null);

        if (c != null && c.moveToFirst()) {
            String accountType;
            String sourceId;
            accountType = c.getString(0);
            if ((TextUtils.isEmpty(accountType)) || (!accountType.equals(ACCOUNT_TYPE_SIM))) {
                Log.e(TAG, "Account Type Error!");
            }
            iccContactInfo[0] = c.getString(1);
            sourceId = c.getString(2);
            if (!TextUtils.isEmpty(sourceId)) {
                iccContactInfo[6] = sourceId;
            }
        } else {
            Log.e(TAG, "getIccContactInfo(): Failed to read account name and record index");
        }

        if (c != null)
            c.close();

        ArrayList<Long> groupIds = getGroupIdsFromRawContactId(resolver, rawContactId);
        if (!groupIds.isEmpty()) {
            StringBuilder groupString = new StringBuilder();

            for (Long group_row_id : groupIds) {
                String iccGroupId = getIccGroupIdFromGroupId(resolver, group_row_id);
                if (!TextUtils.isEmpty(iccGroupId)) {
                    groupString.append(iccGroupId);
                    groupString.append(",");
                }
            }

            iccContactInfo[5] = groupString.toString();
        }

        if (DBG) Log.d(TAG,"getIccContactInfo(): rawContactId = " + rawContactId
                                            + ", account_name = " + iccContactInfo[0]
                                            + ", name = " + iccContactInfo[1]
                                            + ", number = " + iccContactInfo[2]
                                            + ", email = " + iccContactInfo[3]
                                            + ", anr = " + iccContactInfo[4]
                                            + ", group = " + iccContactInfo[5]
                                            + ", index = " + iccContactInfo[6]);

        return iccContactInfo;
    }

    public static void deleteIccContact(ContentResolver resolver, long contactId) {
        long rawContactId = getRawContactId(resolver, contactId);

        String[] iccContactInfo = getIccContactInfo(resolver, rawContactId, null);

        Uri uri;
        if (iccContactInfo[0].equals(ACCOUNT_NAME_SIM2)) {
            uri = Uri.parse("content://icc2/adn");
        } else {
            uri = Uri.parse("content://icc/adn");
        }

        StringBuilder buf = new StringBuilder();
        buf.append(IccProvider.ICC_TAG + "='");
        buf.append(iccContactInfo[1]);
        buf.append("' AND " + IccProvider.ICC_NUMBER + "='");
        buf.append(iccContactInfo[2]);
        buf.append("'");
        if (!TextUtils.isEmpty(iccContactInfo[3])) {
            buf.append(" AND " + IccProvider.ICC_EMAILS + "='");
            buf.append(iccContactInfo[3]);
            buf.append("'");
        }
        if (!TextUtils.isEmpty(iccContactInfo[4])) {
            buf.append(" AND " + IccProvider.ICC_ANRS + "='");
            buf.append(iccContactInfo[4]);
            buf.append("'");
        }

        if (!TextUtils.isEmpty(iccContactInfo[5])) {
            buf.append(" AND " + IccProvider.ICC_GROUPS+ "='");
            buf.append(iccContactInfo[5]);
            buf.append("'");
        }

        if (-1 != Integer.valueOf(iccContactInfo[6])) {
            buf.append(" AND " + IccProvider.ICC_INDEX + "='");
            buf.append(iccContactInfo[6]);
            buf.append("'");
        }

        resolver.delete(uri, buf.toString(), null);
    }

    public static boolean getContactInfoForExportToSim(Context context, long contactId, int index,int simId, String[] names, String[] numbers, String[] emails, String[] anrs) {
        if (DBG) Log.i(TAG, "getContactInfoForExportToSim(): index = " + index + ", contactId = " + contactId);

        if(null==context) {
            Log.e(TAG, "getContactInfoForExportToSim(): null == context");
            return false;
        }

        ContentResolver resolver;
        resolver=context.getContentResolver();
        if (null == resolver) {
            Log.e(TAG, "getContactInfoForExportToSim(): null == resolver");
            return false;
        }

        Cursor c;
        Uri baseUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri dataUri = Uri.withAppendedPath(baseUri, Contacts.Data.CONTENT_DIRECTORY);
        String name;

        SimContactsReadyHelper simReadyHelper;
        simReadyHelper = new SimContactsReadyHelper(null, false);
        name="";
        names[index] = "";
        c = resolver.query(dataUri,
                           new String[] {StructuredName.DISPLAY_NAME},
                           Data.MIMETYPE + "=?",
                           new String[] {StructuredName.CONTENT_ITEM_TYPE},
                           null);

        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();

                name = c.getString(0);

                if (!TextUtils.isEmpty(name)) {
                    int adnStrMaxLen=simReadyHelper.getAdnStrMaxLen(simId);
                    if(name.length() >adnStrMaxLen) {

                        names[index]=name.substring(0,adnStrMaxLen);
                        Log.w(TAG, names[index] + "getContactInfoForExportToSim(): name.length() >adnStrMaxLen");
                    }else {
                        names[index] = name;
                    }
                }
            }
            c.close();
        }

        if (TextUtils.isEmpty(names[index])) {
            Log.e(TAG, "getContactInfoForExportToSim(): names["+index+"] is empty. Skip...");
            return false;
        }

        numbers[index] = "";
        anrs[index] = "";
        c = resolver.query(dataUri,
                           new String[] {Phone.NUMBER,
                                         Phone.IS_PRIMARY},
                           Data.MIMETYPE + "=?",
                           new String[] {Phone.CONTENT_ITEM_TYPE},
                           null);

        if (c != null) {
            if (c.getCount() > 0) {
                String number;
                int primary;
                c.moveToFirst();
                do {
                    number = c.getString(0);
                    primary = c.getInt(1);

                    if (!TextUtils.isEmpty(number)) {
                        if (primary == 1) {
                            if (!TextUtils.isEmpty(numbers[index])) {
                                anrs[index] = numbers[index];
                            }
                            numbers[index] = number;
                        } else if (primary == 0) {
                            if ((TextUtils.isEmpty(numbers[index])) && (!TextUtils.isEmpty(anrs[index]))) {
                                numbers[index] = anrs[index];
                            }
                            anrs[index] = number;
                        }

                        if ((!TextUtils.isEmpty(numbers[index])) && (!TextUtils.isEmpty(anrs[index]))) {
                            break;
                        }
                    }
                } while(c.moveToNext());
            }
            c.close();
        }

        if ((TextUtils.isEmpty(numbers[index])) && (!TextUtils.isEmpty(anrs[index]))) {
            numbers[index] = anrs[index];
            anrs[index] = "";
        }
        numbers[index] = numbers[index].replaceAll("[\\(\\)\\- ]","");
        anrs[index] = anrs[index].replaceAll("[\\(\\)\\- ]","");

        int adnDigitMaxLen=simReadyHelper.getAdnDigitMaxLen(simId);
        if(numbers[index].length() >adnDigitMaxLen) {
           numbers[index]=numbers[index].substring(0,adnDigitMaxLen);
           Log.w(TAG, "getContactInfoForExportToSim():"+name+" numbers[index].length() >adnDigitMaxLen");
        }

        int anrDigitMaxLen=simReadyHelper.getAnrDigitMaxLen(simId);
        if(simReadyHelper.supportAnr(simId) && anrs[index].length() >anrDigitMaxLen) {
           anrs[index] =anrs[index].substring(0,anrDigitMaxLen);
           Log.w(TAG, "getContactInfoForExportToSim():"+ name +" anrs[index].length() >anrDigitMaxLen");
        }

        emails[index] = "";
        c = resolver.query(dataUri,
                           new String[] {Email.DATA},
                           Data.MIMETYPE + "=?",
                           new String[] {Email.CONTENT_ITEM_TYPE},
                           null);

        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                String email;
                email = c.getString(0);

                if (!TextUtils.isEmpty(email)) {
                    int emailStrMaxLen=simReadyHelper.getEmailStrMaxLen(simId);
                    if(simReadyHelper.supportEmail(simId) && email.length() >emailStrMaxLen) {
                       emails[index] =email.substring(0,emailStrMaxLen);
                       Log.w(TAG, "getContactInfoForExportToSim():"+name+" emails.length() > emailStrMaxLen");
                    }else {
                       emails[index] = email;
                    }
                }
            }
            c.close();
        }

        return true;
    }

    public static boolean addIccMembersToIccGroup(ContentResolver resolver, String simIdStr, long[] rawContactsToAdd, long iccGroupIndex) {
        if (null == rawContactsToAdd) {
            Log.e(TAG, "addIccMembersToIccGroup():null==rawContactsToAdd");
            return false;
        }

        if (0 > iccGroupIndex) {
            Log.e(TAG, "addIccMembersToIccGroup(): 0 > iccGroupIndex");
            return false;
        }

        if (DBG) Log.d(TAG, "addIccMembersToIccGroup(): simId = " + simIdStr + ", iccGroupIndex = " + iccGroupIndex);

        SimCardID simId;
        if (ACCOUNT_NAME_SIM2.equals(simIdStr)) {
            simId = SimCardID.ID_ONE;
        } else {
            simId = SimCardID.ID_ZERO;
        }

        boolean found;
        for (long rawContactId : rawContactsToAdd) {
            if (DBG) Log.d(TAG, "addIccMembersToIccGroup(): rawContactId = " + rawContactId);
            String[] contactInfo;
            contactInfo = getIccContactInfo(resolver, rawContactId, null);
            found = false;
            if (!TextUtils.isEmpty(contactInfo[5])) {
                String[] GroupArray = contactInfo[5].split(",");
                for (String group : GroupArray) {
                    if (!TextUtils.isEmpty(group)) {
                        if(iccGroupIndex == Long.valueOf(group)) {
                            found = true;
                        }
                    }
                }
            }

            if (!found) {
                StringBuilder groupString = new StringBuilder();
                groupString.append(contactInfo[5]);
                groupString.append(String.valueOf(iccGroupIndex));
                groupString.append(",");
                String newGroup = groupString.toString();

                if (!updateIccCardContact(resolver,
                                          simId,
                                          contactInfo[1],
                                          contactInfo[1],
                                          contactInfo[2],
                                          contactInfo[2],
                                          contactInfo[3],
                                          contactInfo[3],
                                          contactInfo[4],
                                          contactInfo[4],
                                          contactInfo[5],
                                          newGroup,
                                          Integer.valueOf(contactInfo[6]))) {
                    Log.e(TAG, "addIccMembersToIccGroup():updateIccCardContact fail");
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean removeIccMembersFromIccGroup(ContentResolver resolver, String simIdStr, long[] rawContactsToRemove,
            long iccGroupIndex) {
        String[] contactInfo;
        if (null == rawContactsToRemove) {
            return false;
        }

        if (0 > iccGroupIndex) {
            Log.e(TAG, "removeIccMembersFromIccGroup():0 > iccGroupIndex");
            return false;
        }

        if (DBG) Log.d(TAG, "removeIccMembersFromIccGroup(): simId = " + simIdStr + ", groupIndex = " + iccGroupIndex);

        SimCardID simId;
        if (ACCOUNT_NAME_SIM2.equals(simIdStr)) {
            simId = SimCardID.ID_ONE;
        } else {
            simId = SimCardID.ID_ZERO;
        }

        for (long rawContactId : rawContactsToRemove) {
            if (DBG) Log.d(TAG, "removeIccMembersFromIccGroup(): rawContactId = " + rawContactId);
            StringBuilder groupString = new StringBuilder();
            contactInfo = getIccContactInfo(resolver, rawContactId, null);
            if (!TextUtils.isEmpty(contactInfo[5])) {
                String[] GroupArray = contactInfo[5].split(",");
                for (String group : GroupArray) {
                    if (!TextUtils.isEmpty(group)) {
                        if(iccGroupIndex != Long.valueOf(group)){
                            groupString.append(String.valueOf(group));
                            groupString.append(",");
                        }
                    }
                }
            }

            if (!updateIccCardContact(resolver,
                                      simId,
                                      contactInfo[1],
                                      contactInfo[1],
                                      contactInfo[2],
                                      contactInfo[2],
                                      contactInfo[3],
                                      contactInfo[3],
                                      contactInfo[4],
                                      contactInfo[4],
                                      contactInfo[5],
                                      groupString.toString(),
                                      Integer.valueOf(contactInfo[6]))) {

                Log.e(TAG, "removeIccMembersFromIccGroup():updateIccCardContact fail");
                return false;
            }
        }
        return true;
    }

    public static Uri insertIccCardGroup(ContentResolver resolver, String simId, String groupName) {
        Uri uri;
        if (ACCOUNT_NAME_SIM2.equals(simId)) {
            uri = Uri.parse("content://icc2/adn/group");
        } else {
            uri = Uri.parse("content://icc/adn/group");
        }

        ContentValues initialValues = new ContentValues();
        if (!TextUtils.isEmpty(groupName)) {
            initialValues.put(IccProvider.ICC_GROUPS, groupName);
        }else{
            Log.e(TAG,"insertIccCardGroup: groupNmae is empty");
        }

        if (DBG) Log.d(TAG, "insertIccCardGroup(): simId = " + simId+ ", groupName = " + groupName);

        Uri newIccGroupUri = resolver.insert(uri, initialValues);
        if(null==newIccGroupUri) {
            Log.e(TAG, "insertIccCardGroup(): fail to insert Icc Contact");
            return null;
        } else {
            return newIccGroupUri;
        }
    }

    public static boolean insertIccCardContact(ContentResolver resolver, SimCardID simId, String newName, String newNumber, String newEmail, String newNumberAnr, String newGroup) {
        Uri uri;
        if (SimCardID.ID_ONE == simId) {
            uri = Uri.parse("content://icc2/adn");
        } else {
            uri = Uri.parse("content://icc/adn");
        }

        ContentValues initialValues = new ContentValues();
        initialValues.put(IccProvider.ICC_TAG, newName);
        initialValues.put(IccProvider.ICC_NUMBER, newNumber);
        if (!TextUtils.isEmpty(newEmail)) {
            initialValues.put(IccProvider.ICC_EMAILS, newEmail);
        }

        if (!TextUtils.isEmpty(newNumberAnr)) {
            initialValues.put(IccProvider.ICC_ANRS, newNumberAnr);
        }

        if (!TextUtils.isEmpty(newGroup)) {
            initialValues.put(IccProvider.ICC_GROUPS, newGroup);
        }

        if (DBG) Log.d(TAG, "insertIccCardContact(): simId = " + simId.toInt() + ", newName = " + newName + ", newNumber = " + newNumber + ", newEmail = " +
                newEmail + ", newNumberAnr = " + newNumberAnr+", newGroup = "+newGroup);

        Uri newIccContactUri = resolver.insert(uri, initialValues);
        if(null==newIccContactUri) {
            Log.e(TAG, "insertIccCardContact(): fail to insert Icc Contact");
            return false;
        } else {
            return true;
        }
    }

    public static boolean updateIccCardContact(ContentResolver resolver,
                                               SimCardID simId,
                                               String oldName,
                                               String newName,
                                               String oldNumber,
                                               String newNumber,
                                               String oldEmail,
                                               String newEmail,
                                               String oldNumberAnr,
                                               String newNumberAnr,
                                               String oldGroupName,
                                               String newGroupName,
                                               int recordIndex) {
        Uri uri;
        if (SimCardID.ID_ONE == simId) {
            uri = Uri.parse("content://icc2/adn");
        } else {
            uri = Uri.parse("content://icc/adn");
        }

        ContentValues updateValues = new ContentValues();
        updateValues.put(IccProvider.ICC_TAG, oldName);
        updateValues.put(IccProvider.ICC_NUMBER, oldNumber);
        updateValues.put(IccProvider.ICC_TAG_NEW, newName);
        updateValues.put(IccProvider.ICC_NUMBER_NEW, newNumber);

        if (!TextUtils.isEmpty(oldEmail)) {
            updateValues.put(IccProvider.ICC_EMAILS, oldEmail);
            if (DBG) Log.d(TAG, "old email=" + oldEmail);
        }

        if (!TextUtils.isEmpty(newEmail)) {
            updateValues.put(IccProvider.ICC_EMAILS_NEW, newEmail);
            if (DBG) Log.d(TAG, "new email=" + newEmail);
        }

        if (!TextUtils.isEmpty(oldNumberAnr)) {
            updateValues.put(IccProvider.ICC_ANRS, oldNumberAnr);
            if (DBG) Log.d(TAG, "old anr = " + oldNumberAnr);
        }

        if (!TextUtils.isEmpty(newNumberAnr)) {
            updateValues.put(IccProvider.ICC_ANRS_NEW, newNumberAnr);
            if (DBG) Log.d(TAG, "new anr = " + newNumberAnr);
        }

        if (!TextUtils.isEmpty(oldGroupName)) {
            updateValues.put(IccProvider.ICC_GROUPS, oldGroupName);
            if (DBG) Log.d(TAG, "old group=" + oldGroupName);
        }

        if (!TextUtils.isEmpty(newGroupName)) {
            updateValues.put(IccProvider.ICC_GROUPS_NEW, newGroupName);
            if (DBG) Log.d(TAG, "new group=" + newGroupName);
        }

        if(recordIndex != -1) {
            updateValues.put(IccProvider.ICC_INDEX, recordIndex);
        }

        if (DBG) Log.d(TAG, "updateIccCardContact(): oldName = " + oldName
                                                + ", newName = " + newName
                                                + ", oldNumber = " + oldNumber
                                                + ", newNumber = " + newNumber
                                                + ", oldEmail = " + oldEmail
                                                + ", newEmail = " + newEmail
                                                + ", oldNumberAnr = " + oldNumberAnr
                                                + ", newNumberAnr = " + newNumberAnr
                                                + ", oldGroupName = " + oldGroupName
                                                + ", newGroupName = " + newGroupName
                                                + ", recordIndex = " + recordIndex);

        int updateCount = resolver.update(uri, updateValues, null, null);
        if (0 >= updateCount) {
            Log.e(TAG, "updateIccCardContact(): fail to update Icc Contact");
            return false;
        } else {
            return true;
        }
    }

    public static boolean updateIccCardGroup(ContentResolver resolver,
                                               SimCardID simId,
                                               String oldGroupName,
                                               String newGroupName,
                                               long iccGroupIndex) {
        Uri uri;
        if (SimCardID.ID_ONE == simId) {
            uri = Uri.parse("content://icc2/adn/group");
        } else {
            uri = Uri.parse("content://icc/adn/group");
        }

        ContentValues updateValues = new ContentValues();

        if (!TextUtils.isEmpty(oldGroupName)) {
            updateValues.put(IccProvider.ICC_GROUPS, oldGroupName);
            if (DBG) Log.d(TAG, "old group=" + oldGroupName);
        }

        if (!TextUtils.isEmpty(newGroupName)) {
            updateValues.put(IccProvider.ICC_GROUPS_NEW, newGroupName);
            if (DBG) Log.d(TAG, "new group=" + newGroupName);
        }

        if(iccGroupIndex != -1) {
            updateValues.put(IccProvider.ICC_INDEX, iccGroupIndex);
        }

        if (DBG) Log.d(TAG, "updateIccCardGroup(): oldGroupName = " + oldGroupName +
                ", newGroupName = " + newGroupName);

        int updateCount = resolver.update(uri, updateValues, null, null);
        if (0 >= updateCount) {
            Log.e(TAG, "updateIccCardGroup(): fail to update Icc Group Name");
            return false;
        } else {
            return true;
        }
    }

    public static boolean removeIccCardGroup(ContentResolver resolver,
                                             SimCardID simId,
                                             long iccGroupIndex) {
        Uri uri;
        if (SimCardID.ID_ONE == simId) {
            uri = Uri.parse("content://icc2/adn/group");
        } else {
            uri = Uri.parse("content://icc/adn/group");
        }


        StringBuilder buf = new StringBuilder();
        if(iccGroupIndex != -1) {
            buf.append(IccProvider.ICC_INDEX + "='");
            buf.append(iccGroupIndex);
            buf.append("'");
        }

        if (DBG) Log.d(TAG, "removeIccCardGroup(): where: " + buf.toString());

        int deleteCount = resolver.delete(uri, buf.toString(), null);
        if (0 >= deleteCount) {
            Log.e(TAG, "removeIccCardGroup(): fail to update Icc Group Name. deleteCount = " + deleteCount);
            return false;
        } else {
            return true;
        }
    }
}
