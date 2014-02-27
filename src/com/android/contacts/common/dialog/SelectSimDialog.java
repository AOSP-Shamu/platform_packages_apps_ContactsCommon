package com.android.contacts.common.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.telephony.SubInfoRecord;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.contacts.common.R;
import com.android.contacts.common.interactions.ImportExportDialogFragment;
import com.android.contacts.common.util.AccountSelectionUtil;
import com.android.contacts.common.vcard.ExportVCardActivity;
import com.android.contacts.common.vcard.VCardCommonArguments;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.widget.SubscriptionView;
import com.android.internal.telephony.PhoneConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * An dialog invoked to select Sim
 */
public class SelectSimDialog extends DialogFragment {
    public static final String TAG = "SelectSimDialog";
    private static final IntentFilter SIM_STATE_FILTER = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
    private SubscriptionAdapter mAdapter = null;

    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState)) {
                if (getDialog() != null && getDialog().isShowing()) {
                    int simId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
                    Log.d(TAG, "sim has been plugin out,simId:" + simId);
                    for (int i = 0; i < mAdapter.getCount(); i++) {
                        if (mAdapter.getItem(i).mSimId == simId) {
                            dismiss();
                            return;
                        }
                    }
                }
            }
        }
    };

    public static void show(FragmentManager fragmentManager) {
        SelectSimDialog dialog = new SelectSimDialog();
        dialog.show(fragmentManager, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAdapter = new SubscriptionAdapter(getActivity(), SubscriptionManager.getActivatedSubInfoList(getActivity()));

        final DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final long subId = mAdapter.getItem(which).mSubId;
                AccountSelectionUtil.doImportFromSim(getActivity(), null, subId);
                dialog.dismiss();
            }
        };

        return new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_import)
                .setSingleChoiceItems(mAdapter, -1, clickListener).create();
    }

    private class SubscriptionAdapter extends BaseAdapter {
        private Context mContext = null;
        private List<SubInfoRecord> mDataSource = null;

        public SubscriptionAdapter(Context context, List<SubInfoRecord> dataSource) {
            mContext = context;
            mDataSource = dataSource != null ? dataSource : new ArrayList<SubInfoRecord>();
        }

        @Override
        public int getCount() {
            return mDataSource.size();
        }

        @Override
        public SubInfoRecord getItem(int position) {
            return mDataSource.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView : LayoutInflater.from(mContext).inflate(
                    R.layout.select_sim_layout, null);
            SubscriptionView simItem = (SubscriptionView) view.findViewById(R.id.simItem);
            simItem.setThemeType(SubscriptionView.LIGHT_THEME);
            simItem.setSubInfo(getItem(position));
            return view;
        }

        public void changeDataSource(List<SubInfoRecord> dataSource) {
            mDataSource = dataSource != null ? dataSource : new ArrayList<SubInfoRecord>();
            notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mSimStateReceiver, SIM_STATE_FILTER);
        // refresh dialog when resume
        if (mAdapter != null) {
            List<SubInfoRecord> list = SubscriptionManager.getActivatedSubInfoList(getActivity());
            if (list == null || list.size() <= 0) {
                if (getDialog() != null && getDialog().isShowing()) {
                    dismiss();
                }
            } else {
                mAdapter.changeDataSource(SubscriptionManager.getActivatedSubInfoList(getActivity()));
            }
        }
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mSimStateReceiver);
        super.onPause();
    }
}
