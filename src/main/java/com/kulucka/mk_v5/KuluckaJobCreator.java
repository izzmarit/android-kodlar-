package com.kulucka.mk_v5;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.kulucka.mk_v5.services.DataRefreshService;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class KuluckaJobCreator implements JobCreator {

    // static private'dan public'e değiştirildi
    public static final String TAG_AUTO_CONNECT_JOB = "auto_connect_job";

    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case TAG_AUTO_CONNECT_JOB:
                return new AutoConnectJob();
            default:
                return null;
        }
    }

    public static class AutoConnectJob extends Job {

        @NonNull
        @Override
        protected Result onRunJob(@NonNull Params params) {
            boolean autoConnectEnabled = SharedPrefsManager.getInstance().isAutoConnectEnabled();

            if (autoConnectEnabled) {
                // Arka plan servisini başlat
                getContext().startService(new Intent(getContext(), DataRefreshService.class));
                return Result.SUCCESS;
            } else {
                return Result.FAILURE;
            }
        }
    }
}