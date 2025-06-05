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

    // Use FlowUtils to create MutableStateFlow instances
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
            if (isController && scope instanceof UwbControllerSessionScope) {
                UwbControllerSessionScope cScope = (UwbControllerSessionScope) scope;
                localUwbAddressFlow.setValue(String.valueOf(Shorts.fromByteArray(cScope.getLocalAddress().getAddress())));
                localUwbChannelFlow.setValue(String.valueOf(cScope.getUwbComplexChannel().getChannel()));
                localUwbPreambleIndexFlow.setValue(String.valueOf(cScope.getUwbComplexChannel().getPreambleIndex()));
            } else if (!isController && scope instanceof UwbControleeSessionScope) {
                UwbControleeSessionScope cScope = (UwbControleeSessionScope) scope;
                localUwbAddressFlow.setValue(String.valueOf(Shorts.fromByteArray(cScope.getLocalAddress().getAddress())));
                localUwbChannelFlow.setValue("9 (Fixed for Controlee)");
                localUwbPreambleIndexFlow.setValue("N/A (Controlee)");
            } else {
                Log.w(TAG, "Scope type mismatch or null scope during info update.");
                localUwbAddressFlow.setValue("N/A");
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

    public void startRanging(int address, Integer preambleIndex) {
        Log.d(TAG, "Starting ranging with address: " + address + ", preambleIndex: " + preambleIndex);

        UwbClientSessionScope currentScope = sessionScopeRef.get();
        if (currentScope == null) {
            Log.w(TAG, "SessionScope is null. Call setRole() first or wait for it to complete.");
            return;
        }
        startRangingInternal(address, preambleIndex, currentScope);
    }

    private void startRangingInternal(int address, Integer preambleIndex, UwbClientSessionScope clientSessionScope) {
        try {
            UwbAddress peerAddress = new UwbAddress(Shorts.toByteArray((short) address));
            UwbComplexChannel uwbChannel;

            if (isController) {
                if (!(clientSessionScope instanceof UwbControllerSessionScope)) {
                    Log.e(TAG, "Session scope is not UwbControllerSessionScope for controller role.");
                    return;
                }
                uwbChannel = ((UwbControllerSessionScope) clientSessionScope).getUwbComplexChannel();
                Log.d(TAG, "Using controller's channel: " + uwbChannel.getChannel() + ", Preamble: " + uwbChannel.getPreambleIndex());
            } else {
                int preamble = preambleIndex != null ? preambleIndex : 11;
                uwbChannel = new UwbComplexChannel(9, preamble);
                Log.d(TAG, "Controlee using channel 9 with preamble: " + preamble);
            }

            RangingParameters params = new RangingParameters(
                    RangingParameters.CONFIG_MULTICAST_DS_TWR,
                    clientSessionScope.getLocalAddress().hashCode(), // Session ID from local address hash
                    0,
                    new byte[8],
                    null,
                    uwbChannel,
                    Collections.singletonList(new UwbDevice(peerAddress)),
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
                                float d = pos.getPosition().getDistance().getValue();
                                distance.setValue(d);
                            }

                            if (pos.getPosition() != null && pos.getPosition().getAzimuth() != null) {
                                float a = pos.getPosition().getAzimuth().getValue();
                                angle.setValue(a);
                            }
                        } else if (result instanceof RangingResult.RangingResultPeerDisconnected) {
                            //Log.w(TAG, "Ranging peer disconnected: " + ((RangingResult.RangingResultPeerDisconnected) result).getPeerDevice().getAddress());
                        } else {
                            Log.w(TAG, "Non-position ranging result received: " + result.getClass().getSimpleName());
                        }
                    }, error -> {
                        Log.e(TAG, "Error during ranging", error);
                    });
            Log.d(TAG, "Ranging session started/restarted.");
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
            if (isController && currentScope instanceof UwbControllerSessionScope) {
                UwbControllerSessionScope scope = (UwbControllerSessionScope) currentScope;
                return "Controller\nAddress: " + Shorts.fromByteArray(scope.getLocalAddress().getAddress()) +
                        "\nChannel: " + scope.getUwbComplexChannel().getChannel() +
                        "\nPreamble: " + scope.getUwbComplexChannel().getPreambleIndex();
            } else if (!isController && currentScope instanceof UwbControleeSessionScope) {
                UwbControleeSessionScope scope = (UwbControleeSessionScope) currentScope;
                return "Controlee\nAddress: " + Shorts.fromByteArray(scope.getLocalAddress().getAddress()) +
                        "\nSupports Distance: " + scope.getRangingCapabilities().isDistanceSupported() +
                        "\nSupports Azimuth: " + scope.getRangingCapabilities().isAzimuthalAngleSupported() +
                        "\nSupports Elevation: " + scope.getRangingCapabilities().isElevationAngleSupported();
            } else {
                return "Unknown (Scope type mismatch or not UWB scope)";
            }
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