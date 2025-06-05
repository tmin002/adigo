package kr.gachon.adigo.service;

import android.content.Context;
import android.util.Log;

import androidx.core.uwb.RangingParameters;
import androidx.core.uwb.RangingResult;
import androidx.core.uwb.UwbAddress;
import androidx.core.uwb.UwbClientSessionScope;
import androidx.core.uwb.UwbComplexChannel;
import androidx.core.uwb.UwbControleeSessionScope;
import androidx.core.uwb.UwbControllerSessionScope;
import androidx.core.uwb.UwbDevice;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.UwbClientSessionScopeRx;
import androidx.core.uwb.rxjava3.UwbManagerRx;

import com.google.common.primitives.Shorts;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.disposables.Disposable;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kr.gachon.adigo.service.FlowProvider;

public class uwbService {

    private static final String TAG = "UwbService";

    private final UwbManager uwbManager;
    private boolean isController = true;
    private final AtomicReference<UwbClientSessionScope> sessionScopeRef = new AtomicReference<>();
    private Disposable disposable;

    private final MutableStateFlow<Float> distance = FlowProvider.createMutableStateFlow(0f);
    private final MutableStateFlow<Float> angle = FlowProvider.createMutableStateFlow(0f);

    private final MutableStateFlow<String> localUwbAddressFlow = FlowProvider.createMutableStateFlow("N/A");
    private final MutableStateFlow<String> localUwbChannelFlow = FlowProvider.createMutableStateFlow("N/A");
    private final MutableStateFlow<String> localUwbPreambleIndexFlow = FlowProvider.createMutableStateFlow("N/A");


    public uwbService(Context context) {
        this.uwbManager = UwbManager.createInstance(context);
        Log.d(TAG, "UwbManager initialized.");
    }

    public void setRole(boolean controller) {
        this.isController = controller;
        Log.d(TAG, "Setting role: " + (controller ? "Controller" : "Controlee"));

        // Reset local channel/preamble for controlee when role changes, will be set on startRanging
        if (!controller) {
            localUwbChannelFlow.setValue("N/A");
            localUwbPreambleIndexFlow.setValue("N/A");
        }

        try {
            UwbClientSessionScope scope = controller
                    ? UwbManagerRx.controllerSessionScopeSingle(uwbManager).blockingGet()
                    : UwbManagerRx.controleeSessionScopeSingle(uwbManager).blockingGet();
            sessionScopeRef.set(scope);
            Log.d(TAG, "Session scope acquired successfully.");
            updateLocalUwbInfo(scope);
            Log.d(TAG, getLocalInfo());
        } catch (Exception e) {
            Log.e(TAG, "Failed to set session scope", e);
            localUwbAddressFlow.setValue("Error");
            localUwbChannelFlow.setValue("Error");
            localUwbPreambleIndexFlow.setValue("Error");
        }
    }

    private void updateLocalUwbInfo(UwbClientSessionScope scope) {
        try {
            if (scope == null) {
                localUwbAddressFlow.setValue("N/A (No Scope)");
                localUwbChannelFlow.setValue("N/A");
                localUwbPreambleIndexFlow.setValue("N/A");
                return;
            }
            localUwbAddressFlow.setValue(String.valueOf(Shorts.fromByteArray(scope.getLocalAddress().getAddress())));

            if (isController && scope instanceof UwbControllerSessionScope) {
                UwbControllerSessionScope cScope = (UwbControllerSessionScope) scope;
                localUwbChannelFlow.setValue(String.valueOf(cScope.getUwbComplexChannel().getChannel()));
                localUwbPreambleIndexFlow.setValue(String.valueOf(cScope.getUwbComplexChannel().getPreambleIndex()));
            } else if (!isController && scope instanceof UwbControleeSessionScope) {
                // For Controlee, channel and preamble are set during startRanging.
                // Here, we just confirm address. Channel/Preamble might be "N/A" until ranging starts.
                // If localUwbChannelFlow still "N/A", it means ranging hasn't started for controlee yet.
                if (localUwbChannelFlow.getValue().equals("N/A")) {
                    localUwbChannelFlow.setValue("Unset"); // Or keep N/A
                }
                if (localUwbPreambleIndexFlow.getValue().equals("N/A")) {
                    localUwbPreambleIndexFlow.setValue("Unset"); // Or keep N/A
                }
            } else {
                Log.w(TAG, "Scope type mismatch during info update.");
                localUwbChannelFlow.setValue("N/A");
                localUwbPreambleIndexFlow.setValue("N/A");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating local UWB info flows", e);
            localUwbAddressFlow.setValue("Error");
            localUwbChannelFlow.setValue("Error");
            localUwbPreambleIndexFlow.setValue("Error");
        }
    }


    public void setRoleAsync(boolean controller, Runnable onComplete) {
        new Thread(() -> {
            setRole(controller);
            if (onComplete != null) {
                onComplete.run();
            }
        }).start();
    }

    // New signature:
    // peerAddress: Address of the device to range with.
    // configParamChannel: If Controller, this is informational (expected peer channel).
    //                     If Controlee, this is the channel Controlee will listen on.
    // configParamPreamble: If Controller, this is informational (expected peer preamble).
    //                      If Controlee, this is the preamble Controlee will listen with.
    public void startRanging(int peerAddress, int configParamChannel, int configParamPreamble) {
        Log.d(TAG, "Attempting to start ranging. MyRole: " + (isController ? "Controller" : "Controlee") +
                ", PeerAddr: " + peerAddress +
                ", ConfigChannel: " + configParamChannel + ", ConfigPreamble: " + configParamPreamble);

        UwbClientSessionScope currentScope = sessionScopeRef.get();
        if (currentScope == null) {
            Log.w(TAG, "SessionScope is null. Call setRole() first or wait for it to complete.");
            return;
        }
        startRangingInternal(peerAddress, configParamChannel, configParamPreamble, currentScope);
    }

    private void startRangingInternal(int peerDeviceAddress, int configChannel, int configPreamble, UwbClientSessionScope clientSessionScope) {
        try {
            UwbAddress peerUwbAddress = new UwbAddress(Shorts.toByteArray((short) peerDeviceAddress));
            UwbComplexChannel complexChannelForSession;

            if (isController) {
                if (!(clientSessionScope instanceof UwbControllerSessionScope)) {
                    Log.e(TAG, "Session scope is not UwbControllerSessionScope for controller role.");
                    return;
                }
                // Controller uses its own pre-configured channel and preamble
                complexChannelForSession = ((UwbControllerSessionScope) clientSessionScope).getUwbComplexChannel();
                Log.d(TAG, "Controller using its own channel: " + complexChannelForSession.getChannel() +
                        ", Preamble: " + complexChannelForSession.getPreambleIndex() +
                        ". Expecting peer on Ch: " + configChannel + ", Preamble: " + configPreamble);
                // Update local info display to ensure it shows controller's actual channel/preamble
                localUwbChannelFlow.setValue(String.valueOf(complexChannelForSession.getChannel()));
                localUwbPreambleIndexFlow.setValue(String.valueOf(complexChannelForSession.getPreambleIndex()));

            } else { // Controlee
                if (!(clientSessionScope instanceof UwbControleeSessionScope)) {
                    Log.e(TAG, "Session scope is not UwbControleeSessionScope for controlee role.");
                    return;
                }
                // Controlee uses the provided configChannel and configPreamble to listen
                complexChannelForSession = new UwbComplexChannel(configChannel, configPreamble);
                Log.d(TAG, "Controlee configuring to listen on Channel: " + configChannel +
                        ", Preamble: " + configPreamble + ". Expecting peer: " + peerDeviceAddress);
                // Update local info flows for Controlee to reflect its listening parameters
                localUwbChannelFlow.setValue(String.valueOf(configChannel));
                localUwbPreambleIndexFlow.setValue(String.valueOf(configPreamble));
            }

            RangingParameters params = new RangingParameters(
                    RangingParameters.CONFIG_MULTICAST_DS_TWR, // Or other config
                    clientSessionScope.getLocalAddress().hashCode(), // Session ID
                    0,
                    new byte[8], // sessionKeyInfo (can be null if not used by profile)
                    null,        // subSessionKeyInfo (can be null)
                    complexChannelForSession, // The UWB channel local device will use
                    Collections.singletonList(new UwbDevice(peerUwbAddress)), // The peer device
                    RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
            );

            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
                Log.d(TAG, "Disposed old ranging session.");
            }

            disposable = UwbClientSessionScopeRx.rangingResultsObservable(clientSessionScope, params)
                    .subscribe(result -> {
                        if (result instanceof RangingResult.RangingResultPosition) {
                            RangingResult.RangingResultPosition pos = (RangingResult.RangingResultPosition) result;
                            if (pos.getPosition() != null && pos.getPosition().getDistance() != null) {
                                distance.setValue(pos.getPosition().getDistance().getValue());
                            }
                            if (pos.getPosition() != null && pos.getPosition().getAzimuth() != null) {
                                angle.setValue(pos.getPosition().getAzimuth().getValue());
                            }
                        } else if (result instanceof RangingResult.RangingResultPeerDisconnected) {
                            //Log.w(TAG, "Ranging peer disconnected: " + ((RangingResult.RangingResultPeerDisconnected) result).getPeerDevice().getAddress());
                        } else {
                            Log.w(TAG, "Non-position ranging result received: " + result.getClass().getSimpleName());
                        }
                    }, error -> {
                        Log.e(TAG, "Error during ranging", error);
                    });
            Log.d(TAG, "Ranging session started/restarted with local Ch: " + complexChannelForSession.getChannel() +
                    " Preamble: " + complexChannelForSession.getPreambleIndex());
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ranging", e);
        }
    }

    public void stopRanging() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
            Log.d(TAG, "Ranging stopped.");
        } else {
            Log.d(TAG, "No active ranging to stop.");
        }
        distance.setValue(0f);
        angle.setValue(0f);
    }

    public String getLocalInfo() {
        UwbClientSessionScope currentScope = sessionScopeRef.get();
        if (currentScope == null) {
            return "Unknown (SessionScope not yet initialized)";
        }
        try {
            String role = isController ? "Controller" : "Controlee";
            String address = String.valueOf(Shorts.fromByteArray(currentScope.getLocalAddress().getAddress()));
            String channel = localUwbChannelFlow.getValue(); // Use the flow's current value
            String preamble = localUwbPreambleIndexFlow.getValue(); // Use the flow's current value

            if (isController && currentScope instanceof UwbControllerSessionScope) {
                // Ensure flows are updated if they weren't by startRanging yet for some reason
                UwbControllerSessionScope cScope = (UwbControllerSessionScope) currentScope;
                channel = String.valueOf(cScope.getUwbComplexChannel().getChannel());
                preamble = String.valueOf(cScope.getUwbComplexChannel().getPreambleIndex());
            }
            // For Controlee, channel/preamble are set by startRanging and reflected in flows.

            return role + "\nAddress: " + address +
                    "\nChannel: " + channel +
                    "\nPreamble: " + preamble;

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving local info", e);
            return "Unknown (Error)";
        }
    }

    public StateFlow<Float> getDistance() {
        return distance;
    }

    public StateFlow<Float> getAngle() {
        return angle;
    }

    public StateFlow<String> getLocalUwbAddressFlow() {
        return localUwbAddressFlow;
    }

    public StateFlow<String> getLocalUwbChannelFlow() {
        return localUwbChannelFlow;
    }

    public StateFlow<String> getLocalUwbPreambleIndexFlow() {
        return localUwbPreambleIndexFlow;
    }
}