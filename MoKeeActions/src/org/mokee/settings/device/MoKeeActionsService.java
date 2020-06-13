/*
 * Copyright (c) 2015 The CyanogenMod Project
 * Copyright (c) 2017 The LineageOS Project
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

package org.mokee.settings.device;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.util.List;
import java.util.LinkedList;

import org.mokee.settings.device.actions.CameraActivationSensor;
import org.mokee.settings.device.actions.ChopChopSensor;
import org.mokee.settings.device.actions.FlipToMute;
import org.mokee.settings.device.actions.LiftToSilence;
import org.mokee.settings.device.actions.ProximitySilencer;
import org.mokee.settings.device.actions.UpdatedStateNotifier;

import org.mokee.settings.device.doze.DozePulseAction;
import org.mokee.settings.device.doze.FlatUpSensor;
import org.mokee.settings.device.doze.ProximitySensor;
import org.mokee.settings.device.doze.ScreenReceiver;
import org.mokee.settings.device.doze.ScreenStateNotifier;
import org.mokee.settings.device.doze.StowSensor;

public class MoKeeActionsService extends IntentService implements ScreenStateNotifier,
        UpdatedStateNotifier {
    private static final String TAG = "MoKeeActions";

    private final PowerManager mPowerManager;
    private final PowerManager.WakeLock mWakeLock;

    private final List<ScreenStateNotifier> mScreenStateNotifiers = new LinkedList<>();
    private final List<UpdatedStateNotifier> mUpdatedStateNotifiers = new LinkedList<>();

    public MoKeeActionsService(Context context) {
        super("MoKeeActionService");

        Log.d(TAG, "Starting");

        MoKeeActionsSettings mokeeActionsSettings = new MoKeeActionsSettings(context, this);
        SensorHelper sensorHelper = new SensorHelper(context);
        new ScreenReceiver(context, this);

        DozePulseAction dozePulseAction = new DozePulseAction(context);
        mScreenStateNotifiers.add(dozePulseAction);

        // Actionable sensors get screen on/off notifications
        mScreenStateNotifiers.add(new FlatUpSensor(mokeeActionsSettings,
                    sensorHelper, dozePulseAction));
        mScreenStateNotifiers.add(new ProximitySensor(mokeeActionsSettings,
                    sensorHelper, dozePulseAction));
        mScreenStateNotifiers.add(new StowSensor(mokeeActionsSettings,
                    sensorHelper, dozePulseAction));

        // Other actions that are always enabled
        mUpdatedStateNotifiers.add(new CameraActivationSensor(mokeeActionsSettings,
                    sensorHelper));
        if (!Device.isSurnia()){
            mUpdatedStateNotifiers.add(new ChopChopSensor(mokeeActionsSettings,
                        sensorHelper));
        } else {
            Log.d(TAG, "No ChopChop");
        }

        mUpdatedStateNotifiers.add(new ProximitySilencer(mokeeActionsSettings,
                    context, sensorHelper));
        mUpdatedStateNotifiers.add(new FlipToMute(mokeeActionsSettings,
                    context, sensorHelper));
        mUpdatedStateNotifiers.add(new LiftToSilence(mokeeActionsSettings,
                    context, sensorHelper));

        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        String tag = context.getPackageName() + ":ServiceWakeLock";
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
        updateState();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public void screenTurnedOn() {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
        for (ScreenStateNotifier screenStateNotifier : mScreenStateNotifiers) {
            screenStateNotifier.screenTurnedOn();
        }
    }

    @Override
    public void screenTurnedOff() {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        for (ScreenStateNotifier screenStateNotifier : mScreenStateNotifiers) {
            screenStateNotifier.screenTurnedOff();
        }
    }

    public void updateState() {
        if (mPowerManager.isInteractive()) {
            screenTurnedOn();
        } else {
            screenTurnedOff();
        }
        for (UpdatedStateNotifier notifier : mUpdatedStateNotifiers) {
            notifier.updateState();
        }
    }
}
