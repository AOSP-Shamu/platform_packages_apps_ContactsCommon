/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.common.model.account;

import com.android.contacts.common.R;

import android.content.Context;
import android.util.Log;


import java.util.List;
public class LocalAccountType extends BaseAccountType {
    private static final String TAG = "LocalAccountType";

    public static final String LOCAL_CONTACTS_ACCOUNT_NAME = "Phonebook"; // do not modify this for locale
    public static final String ACCOUNT_TYPE = "com.android.contacts.local";

    public LocalAccountType(Context context, String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.dataSet = null;
        this.titleRes = R.string.account_phone;
        this.iconRes = R.mipmap.ic_launcher_contacts;

        this.resourcePackageName = resPackageName;
        this.syncAdapterPackageName = resPackageName;

        try {
            addDataKindStructuredName(context);
            addDataKindDisplayName(context);
            addDataKindPhoneticName(context);
            addDataKindNickname(context);
            addDataKindPhone(context);
            addDataKindEmail(context);
            addDataKindStructuredPostal(context);
            addDataKindIm(context);
            addDataKindOrganization(context);
            addDataKindPhoto(context);
            addDataKindNote(context);
            addDataKindWebsite(context);
            addDataKindSipAddress(context);
            addDataKindGroupMembership(context);

            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return true;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }
}
