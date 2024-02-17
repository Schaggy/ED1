package net.botwithus.debug;

import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class DebugGraphicsContext extends ScriptGraphicsContext {

    private final DebugScript script;
    private long scriptStartTime;
    private int startingXP;
    boolean isScriptRunning = false;
    private final int startingConstitutionLevel;



    public DebugGraphicsContext(ScriptConsole console, DebugScript script) {
        super(console);
        this.script = script;
        this.startingXP = Skills.CONSTITUTION.getSkill().getExperience();
        this.scriptStartTime = System.currentTimeMillis();
        this.startingConstitutionLevel = script.getStartingConstitutionLevel();
    }

    @Override
    public void drawSettings() {
        ImGui.PushStyleColor(21, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Button color
        ImGui.PushStyleColor(18, RGBToFloat(255), RGBToFloat(255), RGBToFloat(255), 1.0f); // Checkbox Tick color
        ImGui.PushStyleColor(5, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Border Colour
        ImGui.PushStyleColor(2, RGBToFloat(0), RGBToFloat(0), RGBToFloat(0), 0.9f); // Background color
        ImGui.PushStyleColor(7, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Checkbox Background color
        ImGui.PushStyleColor(11, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Header Colour
        ImGui.PushStyleColor(22, RGBToFloat(64), RGBToFloat(67), RGBToFloat(67), 1.0f); // Highlighted button color
        ImGui.PushStyleColor(27, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //ImGUI separator Colour
        ImGui.PushStyleColor(30, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour

        ImGui.SetWindowSize(600.f, 600.f);
        if (ImGui.Begin("Snows Token Farmer", 0)) {
            ImGui.PushStyleVar(11, 50.f, 5f); // Increase button size - left is width, right is height
            // Dual-state button logic
            if (isScriptRunning) {
                if (ImGui.Button("Stop Script")) {
                    script.stopScript();
                    isScriptRunning = false;
                }
            } else {
                if (ImGui.Button("Start Script")) {
                    script.startScript();
                    isScriptRunning = true;
                }
            }
            ImGui.PopStyleVar(3);

            ImGui.Separator();
            long elapsedTimeMillis = System.currentTimeMillis() - scriptStartTime;
            long elapsedSeconds = elapsedTimeMillis / 1000;
            long hours = elapsedSeconds / 3600;
            long minutes = (elapsedSeconds % 3600) / 60;
            long seconds = elapsedSeconds % 60;
            String displayTimeRunning = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            ImGui.SeparatorText("Time Running  " + displayTimeRunning);


            int currentLevel = Skills.CONSTITUTION.getSkill().getLevel();
            int levelsGained = currentLevel - startingConstitutionLevel; // Calculate levels gained

            ImGui.Text("Current Constitution Level: " + currentLevel + "  (" + levelsGained + " Gained)");
            int currentXP = Skills.CONSTITUTION.getSkill().getExperience();
            currentLevel = Skills.CONSTITUTION.getSkill().getLevel();
            int xpForNextLevel = Skills.CONSTITUTION.getExperienceAt(currentLevel + 1);
            int xpTillNextLevel = xpForNextLevel - currentXP;
            ImGui.Text("XP remaining: " + xpTillNextLevel);
            displayXPGained(Skills.CONSTITUTION);
            displayXpPerHour(Skills.CONSTITUTION);
            String timeToLevelStr = calculateTimeTillNextLevel();
            ImGui.Text(timeToLevelStr);

            ImGui.Separator();

            displayXpProgressBar();
            ImGui.PopStyleColor(100);

            ImGui.Separator();
            ImGui.PushStyleColor(0, RGBToFloat(255), RGBToFloat(255), RGBToFloat(255), 0.7f);

            ImGui.Text("ALPHA TESTING");
            ImGui.Separator();
            drawConsoleLogTab();

            ImGui.End();
        }
        ImGui.PopStyleColor(1);
    }

    private void displayXPGained(Skills skill) {
        int currentXP = skill.getSkill().getExperience();
        int xpGained = currentXP - startingXP;
        ImGui.Text("XP Gained: " + xpGained);
    }

    private void displayXpPerHour(Skills skill) {
        long elapsedTime = System.currentTimeMillis() - scriptStartTime;
        double hoursElapsed = elapsedTime / (1000.0 * 60 * 60);
        int currentXP = skill.getSkill().getExperience();
        int xpGained = currentXP - startingXP;
        double xpPerHour = hoursElapsed > 0 ? xpGained / hoursElapsed : 0;

        // Format XP per hour for readability
        String formattedXpPerHour = formatNumberForDisplay(xpPerHour);

        ImGui.Text("XP Per Hour: " + formattedXpPerHour);
    }

    /**
     * Formats a number for display by converting it into a string with
     * suffixes like 'k' for thousands, 'M' for millions, etc.
     * @param number The number to format.
     * @return A formatted string representing the number.
     */
    private String formatNumberForDisplay(double number) {
        if (number < 1000) {
            return String.format("%.0f", number); // No suffix
        } else if (number < 1000000) {
            return String.format("%.1fk", number / 1000); // Thousands
        } else if (number < 1000000000) {
            return String.format("%.1fM", number / 1000000); // Millions
        } else {
            return String.format("%.1fB", number / 1000000000); // Billions
        }
    }

    private void displayTimeRunning() {
        long elapsedTimeMillis = System.currentTimeMillis() - scriptStartTime;
        long elapsedSeconds = elapsedTimeMillis / 1000;
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;

        String timeRunningFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        ImGui.Text(timeRunningFormatted);
    }

    private static float RGBToFloat(int rgbValue) {
        return rgbValue / 255.0f;
    }

    private void displayXpProgressBar() {
        int currentXP = Skills.CONSTITUTION.getSkill().getExperience();
        int currentLevel = Skills.CONSTITUTION.getSkill().getLevel();
        int xpForNextLevel = Skills.CONSTITUTION.getExperienceAt(currentLevel + 1);
        int xpForCurrentLevel = Skills.CONSTITUTION.getExperienceAt(currentLevel);


        // Calculate the total XP needed to reach the next level from the current level
        int xpToNextLevel = xpForNextLevel - xpForCurrentLevel;
        // Calculate how much XP has been gained towards the next level
        int xpGainedTowardsNextLevel = currentXP - xpForCurrentLevel;
        // Calculate the progress towards the next level as a percentage
        float progress = (float) xpGainedTowardsNextLevel / xpToNextLevel;

        float[][] colors = {
                {1.0f, 0.0f, 0.0f, 1.0f}, // 0% Red
                {1.0f, 0.4f, 0.4f, 1.0f}, // 10% Lighter Red
                {1.0f, 0.6f, 0.0f, 1.0f}, // 20% Orange
                {1.0f, 0.7f, 0.4f, 1.0f}, // 30% Lighter Orange
                {1.0f, 1.0f, 0.0f, 1.0f}, // 40% Yellow
                {0.8f, 1.0f, 0.4f, 1.0f}, // 50% Very Light Green
                {0.6f, 1.0f, 0.6f, 1.0f}, // 60% Light Green
                {0.4f, 1.0f, 0.4f, 1.0f}, // 70% Green
                {0.3f, 0.9f, 0.3f, 1.0f}, // 80% Slightly Darker Green
                {0.2f, 0.8f, 0.2f, 1.0f}, // 90% Slightly More Darker Green
                {0.1f, 0.7f, 0.1f, 1.0f}  // 100% Even Darker Green (for completion)
        };


        int index = (int) (progress * 10);
        float blend = (progress * 10) - index;
        if (index >= colors.length - 1) {
            index = colors.length - 2;
            blend = 1;
        }
        float[] startColor = colors[index];
        float[] endColor = colors[index + 1];
        float[] currentColor = {
                startColor[0] + blend * (endColor[0] - startColor[0]),
                startColor[1] + blend * (endColor[1] - startColor[1]),
                startColor[2] + blend * (endColor[2] - startColor[2]),
                1.0f
        };
        ImGui.PushStyleColor(42, currentColor[0], currentColor[1], currentColor[2], currentColor[3]);


        ImGui.Text("XP Progress to Next Level:");
        ImGui.PushStyleColor(0, RGBToFloat(0), RGBToFloat(0), RGBToFloat(0), 0.0f); // Checkbox color
        ImGui.ProgressBar(String.format("%.2f%%", progress * 100), progress, 200, 15);
        ImGui.PopStyleColor(2);
    }

    private String calculateTimeTillNextLevel() {
        int currentXP = Skills.CONSTITUTION.getSkill().getExperience();
        int currentLevel = Skills.CONSTITUTION.getSkill().getLevel();
        int xpForNextLevel = Skills.CONSTITUTION.getExperienceAt(currentLevel + 1);
        int xpForCurrentLevel = Skills.CONSTITUTION.getExperienceAt(currentLevel);
        int xpGainedTowardsNextLevel = currentXP - xpForCurrentLevel;

        long currentTime = System.currentTimeMillis();
        int xpGained = currentXP - startingXP;
        long timeElapsed = currentTime - scriptStartTime; // Time elapsed since tracking started in milliseconds

        if (xpGained > 0 && timeElapsed > 0) {
            // Calculate the XP per millisecond
            double xpPerMillisecond = xpGained / (double) timeElapsed;
            // Estimate the time to level up in milliseconds
            long timeToLevelMillis = (long) ((xpForNextLevel - currentXP) / xpPerMillisecond);

            // Convert milliseconds to hours, minutes, and seconds
            long timeToLevelSecs = timeToLevelMillis / 1000;
            long hours = timeToLevelSecs / 3600;
            long minutes = (timeToLevelSecs % 3600) / 60;
            long seconds = timeToLevelSecs % 60;

            return String.format("Time to level: %02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return "Time to level: calculating...";
        }
    }
    private void XPtillNextLevel() {

        // Get the current XP in Herblore
        int currentXP = Skills.CONSTITUTION.getSkill().getExperience();
        // Get the current level in Herblore
        int currentLevel = Skills.CONSTITUTION.getSkill().getLevel();
        // Calculate the XP required for the next level
        int xpForNextLevel = Skills.CONSTITUTION.getExperienceAt(currentLevel + 1);
        // Calculate the difference between the XP required for the next level and the current XP
        int xpTillNextLevel = xpForNextLevel - currentXP;
    }
    private void drawConsoleLogTab() {
        for (String message : script.consoleMessages) {
            if (message != null){
                ImGui.Text(message);
            }
        }
    }
}