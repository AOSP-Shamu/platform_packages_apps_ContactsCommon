/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.contacts.common.util;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;

import java.util.ArrayList;
import java.util.List;

import com.android.contacts.common.BrcmIccUtils;

/**
 * List-Adapter for Account selection
 */
public final class AccountsListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final List<AccountWithDataSet> mAccounts;
    private final AccountTypeManager mAccountTypes;
    private final Context mContext;

    /**
     * Filters that affect the list of accounts that is displayed by this adapter.
     */
    public enum AccountListFilter {
        ALL_ACCOUNTS,                   // All read-only and writable accounts
        ACCOUNTS_CONTACT_WRITABLE,      // Only where the account type is contact writable
        ACCOUNTS_GROUP_WRITABLE,         // Only accounts where the account type is group writable
        ACCOUNTS_CONTACT_WRITABLE_AND_LOCAL, // the account type is contact writable + local phonebook
        ACCOUNTS_CONTACT_WRITABLE_WITHOUT_SIM // the account type is contact writable but not SIM accounts
    }

    public AccountsListAdapter(Context context, AccountListFilter accountListFilter) {
        this(context, accountListFilter, null);
    }

    /**
     * @param currentAccount the Account currently selected by the user, which should come
     * first in the list. Can be null.
     */
    public AccountsListAdapter(Context context, AccountListFilter accountListFilter,
            AccountWithDataSet currentAccount) {
        mContext = context;
        mAccountTypes = AccountTypeManager.getInstance(context);
        mAccounts = getAccounts(accountListFilter);
        if (currentAccount != null
                && !mAccounts.isEmpty()
                && null != mAccounts.get(0)
                && !mAccounts.get(0).equals(currentAccount)
                && mAccounts.remove(currentAccount)) {
            mAccounts.add(0, currentAccount);
        }
        mInflater = LayoutInflater.from(context);
    }

    private List<AccountWithDataSet> getAccounts(AccountListFilter accountListFilter) {
        if (accountListFilter == AccountListFilter.ACCOUNTS_GROUP_WRITABLE) {
            return new ArrayList<AccountWithDataSet>(mAccountTypes.getGroupWritableAccounts());
        }

        if (accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE_WITHOUT_SIM) {
            List<AccountWithDataSet> allWritable = new ArrayList<AccountWithDataSet>(mAccountTypes.getAccounts(true));
            allWritable.remove(new AccountWithDataSet(
                    BrcmIccUtils.ACCOUNT_NAME_SIM1, BrcmIccUtils.ACCOUNT_TYPE_SIM, BrcmIccUtils.ACCOUNT_NAME_SIM1));
            allWritable.remove(new AccountWithDataSet(
                    BrcmIccUtils.ACCOUNT_NAME_SIM2, BrcmIccUtils.ACCOUNT_TYPE_SIM, BrcmIccUtils.ACCOUNT_NAME_SIM2));
            return allWritable;
        } else if (accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE_AND_LOCAL) {
            List<AccountWithDataSet> allWritable = new ArrayList<AccountWithDataSet>(mAccountTypes.getAccounts(true));
            allWritable.add(null);
            return allWritable;
        }

        return new ArrayList<AccountWithDataSet>(mAccountTypes.getAccounts(
                accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View resultView = convertView != null ? convertView
                : mInflater.inflate(R.layout.account_selector_list_item, parent, false);

        final TextView text1 = (TextView) resultView.findViewById(android.R.id.text1);
        final TextView text2 = (TextView) resultView.findViewById(android.R.id.text2);
        final ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);

        final AccountWithDataSet account = mAccounts.get(position);
        if (null == account) {
            text1.setText(R.string.local_phonebook_account_type_label);

            text2.setVisibility(View.GONE);
            icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_local_contact_picture));
            return resultView;
        }
        final AccountType accountType = mAccountTypes.getAccountType(account.type, account.dataSet);

        text1.setText(accountType.getDisplayLabel(mContext));

        // For email addresses, we don't want to truncate at end, which might cut off the domain
        // name.
        text2.setText(account.name);
        text2.setEllipsize(TruncateAt.MIDDLE);

        icon.setImageDrawable(accountType.getDisplayIcon(mContext));

        return resultView;
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public AccountWithDataSet getItem(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}

