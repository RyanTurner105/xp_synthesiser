package com.mcdart.xp_synthesiser.util;

import com.mcdart.xp_synthesiser.Config;
import net.minecraft.server.commands.ExperienceCommand;
import net.minecraft.world.entity.ExperienceOrb;

public class HelperFunctions {

    // Gets the per-tick cost of progressing the kill recorder.
    // Most people will be able to receive XP at a rate between 1-3XP per 10 ticks. This will generally cost between 1-3k energy per tick.
    // Using many methods, you can get up to 10XP per 10 ticks. This is considered costly by the algorithm.
    // Cost is:
    // - A base cost of 10000 energy per XP point
    // - A scaling cost that essentially increases the cost by the same factor of the ratio of XP to ticks
    // - A punishing factor that vastly increases costs as the XP gets high while the ticks does not
    // - Divides that by the number of ticks to get a tick cost.
    // Some examples (CPT = Cost Per Tick, TC = Total Cost):
    // - totalXP: 10, totalTicks: 100 (2 monster kills in 5 seconds) - CPT: 1,001, TC: 100,100
    // - totalXP: 100, totalTicks: 100 (20 monster kills in 5 seconds) - CPT: 11,000, TC: 1,100,000
    // - totalXP: 100, totalTicks: 1000 (20 monster kills in 50 seconds) - CPT: 1,010, TC: 1,010,000
    // - totalXP: 1000, totalTicks: 100 (some exploit) - CPT: 1,100,000, TC: 110,000,000
    public static double getTickCost(int totalXp, int totalTicks) {
        int defaultCost = Config.GENERAL.xpPointCost.getAsInt(); // Default is 10000

        double baseCost = (double)totalXp * defaultCost / (double)totalTicks;
        double scalingCost = (double)(totalXp^3) * defaultCost / ((double)(totalTicks^2) * ((double) defaultCost / 10));

        return baseCost + scalingCost;
    }

    // Returns the current level and progress to the next level as a double.
    // Driven by minecraft wiki formulas on experience.
    public static double getLevelFromXP(int xp) {
        if (xp > 0 && xp <= 352) {
            // Levels 0 to 16
            return Math.sqrt(xp + 9) - 3;
        } else if (xp > 352 && xp <= 1507) {
            // Levels 17-31
            return 81.0 / 10.0 + Math.sqrt(2.0/5.0 * (xp - 7839.0/40.0));
        } else if (xp > 1507){
            // Levels 32+
            return 325.0 / 18.0 + Math.sqrt(2.0/9.0 * (xp - 54215.0/72.0));
        }
        return 0;
    }

    // Returns the current xp as an int.
    // Driven by minecraft wiki formulas on experience.
    public static int getXPfromLevel(double level) {
        if (level > 0 && level <= 16) {
            // Levels 0 to 16
            return (int) Math.round(Math.pow(level, 2) + 6 * level);
        } else if (level > 16 && level <= 31) {
            // Levels 17-31
            return (int) Math.round(2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else if (level > 31){
            // Levels 32+
            return (int) Math.round(4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }
        return 0;
    }

    // Rounds a number to the number of significant figures provided
    public static double roundTo(double num, int sigFigs) {
        return (double) Math.round(num * (Math.pow(10, sigFigs))) / (Math.pow(10, sigFigs));
    }

}
