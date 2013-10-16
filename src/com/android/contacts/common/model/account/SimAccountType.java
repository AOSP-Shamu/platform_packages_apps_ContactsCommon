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
import com.android.contacts.common.model.account.AccountType.EditField;

import com.android.contacts.common.model.dataitem.DataKind;
import com.google.android.collect.Lists;

import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;

public class SimAccountType extends BaseAccountType {
    private static final String TAG = "SimAccountType";

    public static final String ACCOUNT_TYPE = "com.android.contacts.sim";

    private boolean mSupportGroup = false;

    public SimAccountType(Context context, String resPackageName, String dataSet) {
        this.accountType = ACCOUNT_TYPE;
        this.dataSet = dataSet;
        this.titleRes = R.string.account_phone;
        this.iconRes = R.mipmap.ic_launcher_contacts;

        this.resourcePackageName = resPackageName;
        this.syncAdapterPackageName = resPackageName;
        this.mSupportGroup = false;

        try {
            addDataKindStructuredName(context);
            addDataKindDisplayName(context);
//            addDataKindPhone(context);
//            addDataKindEmail(context);
//            addDataKindGroupMembership(context);

            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    @Override
    protected DataKind addDataKindStructuredName(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(StructuredName.CONTENT_ITEM_TYPE,
                R.string.nameLabelsGroup, -1, true));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);
        kind.typeOverallMax = 1;

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME, R.string.full_name, FLAGS_PERSON_NAME));

        return kind;
    }

    protected DataKind addDataKindDisplayName(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME,
                R.string.nameLabelsGroup, -1, true));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);
        kind.typeOverallMax = 1;

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME, R.string.full_name, FLAGS_PERSON_NAME));

        return kind;
    }

    public DataKind addDataKindPhone(Context context, boolean supportSecondaryPhone) throws DefinitionException {
        DataKind kind = addKind(new DataKind(Phone.CONTENT_ITEM_TYPE, R.string.phoneLabelsGroup,
                10, true));
        kind.iconAltRes = R.drawable.ic_text_holo_light;
        kind.iconAltDescriptionRes = R.string.sms;
        kind.actionHeader = new PhoneActionInflater();
        kind.actionAltHeader = new PhoneActionAltInflater();
        kind.actionBody = new SimpleInflater(Phone.NUMBER);
        kind.typeColumn = Phone.TYPE;
        if (supportSecondaryPhone) {
            kind.typeOverallMax = 2;
        } else {
            kind.typeOverallMax = 1;
        }

        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildPhoneType(Phone.TYPE_MOBILE).setSpecificMax(1));
        if (supportSecondaryPhone) {
            kind.typeList.add(buildPhoneType(Phone.TYPE_HOME).setSpecificMax(1));
        }

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));

        return kind;
    }

    public DataKind addDataKindEmail(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(Email.CONTENT_ITEM_TYPE, R.string.emailLabelsGroup,
                15, true));
        kind.actionHeader = new EmailActionInflater();
        kind.actionBody = new SimpleInflater(Email.DATA);
        kind.typeColumn = Email.TYPE;
        kind.typeOverallMax = 1;
        kind.typeList = Lists.newArrayList();
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Email.DATA, R.string.emailLabelsGroup, FLAGS_EMAIL));

        return kind;
    }

    public DataKind addDataKindGroupMembership(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(GroupMembership.CONTENT_ITEM_TYPE,
                R.string.groupsLabel, 999, true));

        mSupportGroup = true;

        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(GroupMembership.GROUP_ROW_ID, -1, -1));

        return kind;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return mSupportGroup;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }
}
