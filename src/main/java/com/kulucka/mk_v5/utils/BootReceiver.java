package com.kulucka.mk_v5.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.evernote.android.job.JobRequest;
import com.kulucka.mk_v5.KuluckaJobCreator;

import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Cihaz yeniden başladığında, bağlantı işini planla
            if (SharedPrefsManager.getInstance().isAutoConnectEnabled()) {
                scheduleAutoConnectJob();
            }
        }
    }

    private void scheduleAutoConnectJob() {
        new JobRequest.Builder(KuluckaJobCreator.TAG_AUTO_CONNECT_JOB)
                .setExecutionWindow(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(30))
                .setBackoffCriteria(TimeUnit.MINUTES.toMillis(5), JobRequest.BackoffPolicy.EXPONENTIAL)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .build()
                .schedule();
    }
}