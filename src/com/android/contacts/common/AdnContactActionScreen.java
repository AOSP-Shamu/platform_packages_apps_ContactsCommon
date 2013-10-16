/************************************************************************************
 *
 *  Copyright (C) 2009-2010 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ************************************************************************************/

package com.android.contacts.common;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.telephony.IccProvider;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract;
import android.content.ContentProviderOperation;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Photo;

import java.util.ArrayList;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import com.android.internal.telephony.RILConstants.SimCardID;

public class AdnContactActionScreen extends Activity {
    private static final String LOG_TAG = "AdnContactActionScreen";
    private static final boolean DBG = true;

    private String[] mTags=null;
    private String[] mNumbers=null;
    private String[] mEmails=null;
    private String[] mAnrs=null;
    //private String[] mAnrst=null;
    private int[] mIndexs=null;

    protected QueryHandler mQueryHandler;
    private int mAction=0; //action type, del or add
    private Handler mHandler = new Handler();

    public  static final int SIM_OPC_NONE=0;
    public  static final int SIM_OPC_ADD=1;

    public    static final String SIM_ACTION="simaction";
    private static boolean mExporting=false;
    private int mIdxOn=0;    //current operation of idex for bulk operation
    private int mIdxSum=0;    //total idx
    private Uri uri = Uri.parse("content://icc/adn");
    static final String INTENT_EXTRA_NAME = "tag";
    static final String INTENT_EXTRA_NUMBER = "number";
    static final String INTENT_EXTRA_NAME_UP = "newtag";
    static final String INTENT_EXTRA_NUMBER_UP = "newnumber";
    static final String INTENT_EXTRA_INDEX = "index";//for original record index in usim
///usim anr and email
    static final String INTENT_EXTRA_ANRS = "anrs";
    //static final String INTENT_EXTRA_ANRST = "anrst";//for example, "M,H,O"
    static final String INTENT_EXTRA_EMAILS = "emails"; //fo example, "xxx@broadcom.com,yyy@broadcom.com"
    static final String INTENT_EXTRA_ANRS_UPS = "newanrs";//for example, "02162805558,13917665555"
    //static final String INTENT_EXTRA_ANRST_UPS = "newanrst";
    static final String INTENT_EXTRA_EMAIL_UPS = "newemails";

    private ProgressDialog mProgressDialog;
    private SimContactsReadyHelper mSimReadyHelper=null;
    private boolean mSupportEmail = false;
    private boolean mSupportAnr = false;
    private int mFreeEmailEntries = 0;
    private int mFreeAnrEntries = 0;

    String mMsgFormat;
    String mMsgOk="";
    String mMsgFailed="";


    private SimCardID mSimCardId = SimCardID.ID_ZERO;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        resolveIntent();

        if(mIdxSum!=0) {
            mExporting=true;
            mQueryHandler = new QueryHandler(getContentResolver());

            setContentView(R.layout.nothing_screen);


            String title="";
            if (mAction==SIM_OPC_ADD) {
                addContact();
                title=getString(R.string.adding_adn_contact);
                mMsgFormat=getString(R.string.updating_adn_contact_msg);
                if (SimCardID.ID_ONE == mSimCardId) {
                    mMsgOk=getString(R.string.adn_contact_added_sim2);
                    mMsgFailed=getString(R.string.adn_contact_add_failed_sim2);
                } else {
                    mMsgOk=getString(R.string.adn_contact_added_sim1);
                    mMsgFailed=getString(R.string.adn_contact_add_failed_sim1);
                }
            }
            showProgressDialog(null,title);
        }
    }

    private void showProgressDialog(DialogInterface.OnCancelListener listener,String titleAct) {
        String title = titleAct;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(title);
        //String message = String.format(mMsgFormat,mIdxOn+1,mIdxSum);
        //mProgressDialog.setMessage(message);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setOnCancelListener(listener);
        mProgressDialog.show();
        mProgressDialog.setMax(mIdxSum);
        mProgressDialog.setProgress(0);
    }

    private void resolveIntent() {
        Intent intent = getIntent();
        int tp=intent.getIntExtra(SIM_ACTION,SIM_OPC_NONE);

        if(tp!=SIM_OPC_ADD) {
            if (DBG) log("-------none action, so finish ");
            finish();
            return;
        }
        mAction=tp;
        if (!intent.hasExtra(BrcmIccUtils.INTENT_EXTRA_SIM_ID)) {
            Log.e(LOG_TAG, "resolveIntent(): not EXTRA_SIM_ID extra");
        }
        mSimCardId = (SimCardID) (intent.getExtra(BrcmIccUtils.INTENT_EXTRA_SIM_ID, SimCardID.ID_ZERO));
        if (SimCardID.ID_ONE == mSimCardId) {
            uri = Uri.parse("content://icc2/adn");
        } else {
            uri = Uri.parse("content://icc/adn");
        }

        mSimReadyHelper = new SimContactsReadyHelper(null, false);

        int simId=mSimCardId.toInt();

        if (simId==SimCardID.ID_ONE.toInt()) {
            mSimReadyHelper.setSim2CapabilityInfo(mSimReadyHelper.getIccCardCapabilityInfo(getContentResolver(), SimCardID.ID_ONE));
        } else {
            mSimReadyHelper.setSim1CapabilityInfo(mSimReadyHelper.getIccCardCapabilityInfo(getContentResolver(), SimCardID.ID_ZERO));
        }

        mSupportEmail = mSimReadyHelper.supportEmail(simId);
        if(mSupportEmail){
           mFreeEmailEntries=mSimReadyHelper.getFreeEmailCount(simId);
        }else {
           mFreeEmailEntries=0;
        }

        mSupportAnr = mSimReadyHelper.supportAnr(simId);
        if(mSupportAnr){
           mFreeAnrEntries=mSimReadyHelper.getFreeANRCount(simId);
        }else {
           mFreeAnrEntries=0;
        }

        mTags =  intent.getStringArrayExtra(INTENT_EXTRA_NAME);
        mNumbers =  intent.getStringArrayExtra(INTENT_EXTRA_NUMBER);
        mEmails =    intent.getStringArrayExtra(INTENT_EXTRA_EMAILS);
        mAnrs =  intent.getStringArrayExtra(INTENT_EXTRA_ANRS);
        //mAnrst =  intent.getStringArrayExtra(INTENT_EXTRA_ANRST);
        //mAnrst =  intent.getStringArrayExtra(INTENT_EXTRA_ANRST);
        mIndexs =  intent.getIntArrayExtra(INTENT_EXTRA_INDEX);

        if (mTags==null&&mNumbers==null) {
            if (DBG) log("-------both name and number are empty, so finish ");
            finish();
        } else {
            if(mTags.length>mSimReadyHelper.getFreeADNCount(simId)){
                Log.e(LOG_TAG, "resolveIntent(): no enough SIM spaces to export");
                if(mTags.length==1) {
                    Toast.makeText(this, R.string.adnNoSimSpace,
                                    Toast.LENGTH_LONG).show();
                }else if (mTags.length > 1) {
                    Toast.makeText(this, R.string.adnNoSimSpaceForExportAll,
                                    Toast.LENGTH_LONG).show();
                }
                finish();
            }else {
                mIdxSum=mTags.length;
                mIdxOn=0;
            }
        }
    }

    private void addContact() {

        ContentValues initialValues=new ContentValues();
        initialValues.put(IccProvider.ICC_TAG,mTags[mIdxOn]);
        initialValues.put(IccProvider.ICC_NUMBER,mNumbers[mIdxOn]);

        if (mFreeEmailEntries>0) {
            if(mEmails!=null) {
                initialValues.put(IccProvider.ICC_EMAILS,mEmails[mIdxOn]);
            }
            mFreeEmailEntries--;
        }

        if (mFreeAnrEntries>0) {
            if(mAnrs!=null) {
                initialValues.put(IccProvider.ICC_ANRS,mAnrs[mIdxOn]);
             }
            //if(mAnrst!=null)
            //initialValues.put(IccProvider.ICC_ANRST,mAnrst[mIdxOn]);
            mFreeAnrEntries--;
        }

        Log.v(LOG_TAG,"addContact "+mIdxOn+":"+mTags[mIdxOn]+" "+mNumbers[mIdxOn]+" "+mIdxOn+"/"+mIdxSum+ " mFreeEmailEntries:"+mFreeEmailEntries+ " mFreeAnrEntries:" + mFreeAnrEntries);
        mQueryHandler.startInsert(0, null, uri,initialValues);
    }

    // Replace the status field with a toast to make things appear similar
    // to the rest of the settings.  Removed the useless status field.
    private void showStatus(CharSequence statusMsg) {
        if (statusMsg != null) {
            Toast.makeText(this, statusMsg, Toast.LENGTH_SHORT)
            .show();
        }
    }

    private void handleResult(boolean success) {
        int delay=2000;
        if (success) {
            showStatus(mMsgOk);
        } else {
            String msgDetail=mMsgFailed+"("+mTags[mIdxOn]+"/"+mNumbers[mIdxOn]+")";
            delay=3000;
            showStatus(msgDetail);
        }

        mHandler.postDelayed(new Runnable() {
            public void run() {
                mExporting=false;
                finish();
            }
        }, delay);

    }

    public static boolean isExportingToSIM() {
        return mExporting;
    }

    private boolean addContactsDatabase() {
        if (DBG) log("addContactsDatabase");

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        int rawContactInsertIndex = ops.size();

        int aggregationMode = RawContacts.AGGREGATION_MODE_DISABLED;
        String accountType = BrcmIccUtils.ACCOUNT_TYPE_SIM;
        String accountName = BrcmIccUtils.ACCOUNT_NAME_SIM1;

        if (SimCardID.ID_ONE == mSimCardId) {
            accountName = BrcmIccUtils.ACCOUNT_NAME_SIM2;
        }

        // insert record
        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.AGGREGATION_MODE, aggregationMode)
                .withValue(RawContacts.DELETED, 0)
                .withValue(RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(RawContacts.ACCOUNT_NAME, accountName)
                .withValue(RawContacts.DATA_SET, accountName)
//            .withValue(RawContacts.SOURCE_ID, c.getString(4))
                .withValue(RawContacts.VERSION, 1)
                .withValue(RawContacts.DIRTY, 0)
                .withValue(RawContacts.SYNC1, null)
                .withValue(RawContacts.SYNC2, null)
                .withValue(RawContacts.SYNC3, null)
                .withValue(RawContacts.SYNC4, null)
                .withValues(new ContentValues())
                .build());

        // insert photo
        byte[] photoIcon = null;
        Bitmap photo;
        if (SimCardID.ID_ONE == mSimCardId) {
            photo = BitmapFactory.decodeResource(getResources(),R.drawable.ic_sim2_picture);
        } else {
            photo = BitmapFactory.decodeResource(getResources(),R.drawable.ic_sim1_picture);
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 75, stream);
        photoIcon = stream.toByteArray();

        if (null != photoIcon) {
            log("addContactsDatabase(): [" + rawContactInsertIndex + "].photo");

            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Photo.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                    .withValue(Photo.PHOTO, photoIcon)
                    .withValue(Data.IS_SUPER_PRIMARY, 1)
                    .build());
        }


        // insert name
        if (!TextUtils.isEmpty(mTags[mIdxOn])) {
            log("addContactsDatabase(): [" + rawContactInsertIndex + "].name=" + mTags[mIdxOn]);

            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(StructuredName.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, mTags[mIdxOn])
                    .build());
        }

        // insert number
        if (!TextUtils.isEmpty(mNumbers[mIdxOn])) {
            log("addContactsDatabase(): [" + rawContactInsertIndex + "].number=" + mNumbers[mIdxOn]);

            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Phone.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .withValue(Phone.NUMBER, mNumbers[mIdxOn])
                    .withValue(Data.IS_PRIMARY, 1)
                    .build());
        }

        if (mFreeEmailEntries>0) {
            // insert allEmails
            if (!TextUtils.isEmpty(mEmails[mIdxOn])) {
                log("addContactsDatabase(): [" + rawContactInsertIndex + "].email=" + mEmails[mIdxOn]);

                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Email.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                        .withValue(Email.TYPE, Email.TYPE_MOBILE)
                        .withValue(Email.DATA, mEmails[mIdxOn])
                        .build());
            }
        }

        if (mFreeAnrEntries>0) {
            // insert ANR
            if (!TextUtils.isEmpty(mAnrs[mIdxOn])) {
                log("addContactsDatabase(): [" + rawContactInsertIndex + "].anr=" + mAnrs[mIdxOn]);

                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Phone.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.TYPE, Phone.TYPE_HOME)
                        .withValue(Phone.NUMBER, mAnrs[mIdxOn])
                        .withValue(Data.IS_PRIMARY, 0)
                        .build());
            }
        }

        boolean exceptionOccurs = false;
        if (0 < ops.size()) {
            try {
                getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (OperationApplicationException e) {
                exceptionOccurs = true;
                log("applyBatch(): OperationApplicationException: " + e.toString() + ". message = " + e.getMessage());
            } catch (RemoteException e) {
                exceptionOccurs = true;
                log("applyBatch(): RemoteException: " + e.toString() + ". message = " + e.getMessage());
            }
        }

        return !exceptionOccurs;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mProgressDialog!=null) {
            mProgressDialog.dismiss();
            mProgressDialog=null;
        }
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
        }

        protected void onInsertComplete(int token, Object cookie,Uri uri) {
            if (DBG) log("onInsertComplete "+Integer.toString(mIdxOn)+"/"+Integer.toString(mIdxSum));
            if(uri==null) {
                //failed, show result
                if (mProgressDialog!=null) mProgressDialog.dismiss();
                handleResult(false);
            } else {
                if(mAction==SIM_OPC_ADD) {

                    addContactsDatabase();
                    mIdxOn++;
                    if(mIdxOn<mIdxSum) {
                        //String message = String.format(mMsgFormat,mIdxOn+1,mIdxSum);
                        //mProgressDialog.setMessage(message);
                        if (mProgressDialog!=null) mProgressDialog.incrementProgressBy(1);
                        addContact();
                        //show progress bar
                    } else {
                        //show result
                        if (mProgressDialog!=null) mProgressDialog.dismiss();
                        handleResult(true);
                    }
                }
            }
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
