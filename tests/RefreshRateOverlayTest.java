package com.ltpoxcos;

import java.io.IOException;

public final class RefreshRateOverlayTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        check(RefreshRateOverlay.parseEnabledState("Result: Parcel(00000000 00000001 '........')"));
        check(!RefreshRateOverlay.parseEnabledState("Result: Parcel(00000000 00000000 '........')"));
        expectInvalid("");
        expectInvalid("Result: Parcel()");
        expectInvalid("Result: Parcel(00000000 00000002)");
        System.out.println("PASS " + checks + " SurfaceFlinger refresh-rate overlay response assertions");
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("refresh-rate overlay response check " + checks);
    }

    private static void expectInvalid(String response) throws Exception {
        try {
            RefreshRateOverlay.parseEnabledState(response);
            throw new AssertionError("Invalid SurfaceFlinger response was accepted");
        } catch (IOException expected) {
            checks++;
        }
    }

    private RefreshRateOverlayTest() {}
}
