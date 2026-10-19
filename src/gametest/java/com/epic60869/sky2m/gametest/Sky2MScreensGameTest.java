package com.epic60869.sky2m.gametest;

import com.epic60869.sky2m.sim.roomsim.SimBuilder;
import com.epic60869.sky2m.sim.roomsim.SimState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Loads a Dungeon Sim room from the title screen, as /s2 sim does, and checks the sim's own commands work inside it
 * (/simitem opens the toolkit). Run with `gradlew runClientGameTest`; screenshots land in
 * build/run/clientGameTest/screenshots.
 */
public class Sky2MScreensGameTest implements FabricClientGameTest {
    private static final String ROOM = "Mural";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> SimBuilder.buildSingleRoom(mc, ROOM));
        ctx.waitFor(mc -> SimState.canAct(mc), 2400);
        ctx.waitTicks(100);
        shot(ctx, "40-sim-room");

        ctx.runOnClient(mc -> mc.player.connection.sendCommand("simitem"));
        ctx.waitTicks(10);
        String screen = ctx.computeOnClient(mc -> mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
        System.out.println("[Sky2M gametest] after /simitem the screen is " + screen);
        if (!"SimItemsScreen".equals(screen)) throw new AssertionError("/simitem didn't open the sim toolkit (screen: " + screen + ")");
        shot(ctx, "41-simitem");
        ctx.setScreen(() -> null);
        ctx.waitTicks(5);

    }

    private static void shot(ClientGameTestContext ctx, String name) {
        System.out.println("[Sky2M gametest] screenshot " + name + " -> " + ctx.takeScreenshot(name));
    }
}
