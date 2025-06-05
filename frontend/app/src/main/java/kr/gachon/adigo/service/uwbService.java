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

public class uwbService {

    private static final String TAG = "UwbService";

    private final UwbManager uwbManager;
    private boolean isController = true;
    private final AtomicReference<UwbClientSessionScope> sessionScopeRef = new AtomicReference<>();
    private Disposable disposable;

    private final MutableStateFlow<Float> distance = FlowProvider.createFloatStateFlow(0f);
    private final MutableStateFlow<Float> angle = FlowProvider.createFloatStateFlow(0f);

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
            Log.d(TAG, getLocalInfo());
        } catch (Exception e) {
            Log.e(TAG, "Failed to set session scope", e);
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

        if (sessionScopeRef.get() == null) {
            Log.w(TAG, "SessionScope is null. Call setRole() first.");
            return;
        }

        try {
            UwbAddress peerAddress = new UwbAddress(Shorts.toByteArray((short) address));
            UwbComplexChannel uwbChannel;

            if (isController) {
                uwbChannel = ((UwbControllerSessionScope) sessionScopeRef.get()).getUwbComplexChannel();
                Log.d(TAG, "Using controller's channel: " + uwbChannel.getChannel());
            } else {
                int preamble = preambleIndex != null ? preambleIndex : 5;
                uwbChannel = new UwbComplexChannel(9, preamble);
                Log.d(TAG, "Controlee using custom channel with preamble: " + preamble);
            }

            RangingParameters params = new RangingParameters(
                    RangingParameters.CONFIG_MULTICAST_DS_TWR,
                    12345,
                    0,
                    new byte[8],
                    null,
                    uwbChannel,
                    Collections.singletonList(new UwbDevice(peerAddress)),
                    RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
            );

            if (disposable != null) {
                disposable.dispose();
                Log.d(TAG, "Disposed old ranging session.");
            }

            disposable = UwbClientSessionScopeRx.rangingResultsObservable(sessionScopeRef.get(), params)
                    .subscribe(result -> {
                        if (result instanceof RangingResult.RangingResultPosition) {
                            RangingResult.RangingResultPosition pos = (RangingResult.RangingResultPosition) result;
                            Log.d(TAG, "RangingResult received.");

                            if (pos.getPosition().getDistance() != null) {
                                float d = pos.getPosition().getDistance().getValue();
                                distance.setValue(d);
                                Log.d(TAG, "Distance: " + d);
                            }

                            if (pos.getPosition().getAzimuth() != null) {
                                float a = pos.getPosition().getAzimuth().getValue();
                                angle.setValue(a);
                                Log.d(TAG, "Azimuth: " + a);
                            }
                        } else {
                            Log.w(TAG, "Non-position ranging result received.");
                        }
                    }, error -> {
                        Log.e(TAG, "Error during ranging", error);
                    });
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
    }

    public String getLocalInfo() {
        try {
            if (isController) {
                UwbControllerSessionScope scope = (UwbControllerSessionScope) sessionScopeRef.get();
                return "Controller\nAddress: " + Shorts.fromByteArray(scope.getLocalAddress().getAddress()) +
                        "\nChannel: " + scope.getUwbComplexChannel().getChannel() +
                        "\nPreamble: " + scope.getUwbComplexChannel().getPreambleIndex();
            } else {
                UwbControleeSessionScope scope = (UwbControleeSessionScope) sessionScopeRef.get();
                return "Controlee\nAddress: " + Shorts.fromByteArray(scope.getLocalAddress().getAddress()) +
                        "\nSupports Distance: " + scope.getRangingCapabilities().isDistanceSupported() +
                        "\nSupports Azimuth: " + scope.getRangingCapabilities().isAzimuthalAngleSupported() +
                        "\nSupports Elevation: " + scope.getRangingCapabilities().isElevationAngleSupported();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving local info", e);
            return "Unknown";
        }
    }

    public StateFlow<Float> getDistance() {
        return distance;
    }

    public StateFlow<Float> getAngle() {
        return angle;
    }
}