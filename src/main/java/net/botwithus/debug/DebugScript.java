package net.botwithus.debug;

import net.botwithus.api.game.hud.Dialog;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.prayer.AncientBook;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.game.Client.GameState;
import net.botwithus.rs3.util.RandomGenerator;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.Distance;
import net.botwithus.rs3.game.Travel;
import net.botwithus.rs3.game.scene.entities.characters.PathingEntity;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static net.botwithus.rs3.game.Client.getLocalPlayer;
import static net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer.LOCAL_PLAYER;

public class DebugScript extends LoopingScript {
    public DebugScript(String name, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(name, scriptConfig, scriptDefinition);
        currentState = ScriptState.INTERACTING_TEMPLE; // Start with interacting with the temple
    }


    private boolean isNear(Coordinate coordinate, int proximity) {
        Coordinate playerLocation = LOCAL_PLAYER.getCoordinate();
        return Distance.between(playerLocation, coordinate) <= proximity;
    }

    @Override
    public boolean initialize() {
        startingConstitutionLevel = Skills.PRAYER.getSkill().getLevel();
        this.sgc = new DebugGraphicsContext(getConsole(), this);
        this.loopDelay = 590;
        return super.initialize();
    }
    public boolean isGameLoggedIn() {
        GameState currentState = Client.getGameState();
        return currentState == GameState.LOGGED_IN; // Assuming LOGGED_IN is a valid GameState enum value
    }
    public void startScript() {
        if (!scriptRunning) {
            scriptRunning = true;
            scriptStartTime = Instant.now();
            currentState = ScriptState.INTERACTING_TEMPLE;
            printlnn("Script started successfully.");
        }
    }

    public void stopScript() {
        if (scriptRunning) {
            scriptRunning = false;
            printlnn("Script stopped successfully.");
            currentState = ScriptState.INTERACTING_TEMPLE;
        }
    }

    private boolean scriptRunning = false;
    private Coordinate postTempleEntryCoordinate = null;
    private ScriptState currentState;
    private Npc npc;
    public boolean runScript = false;
    private long lastEffectCheckTime = 0;
    private final long effectCheckInterval = 30000; // 30 seconds interval for effect checks
    private int startingConstitutionLevel;
    private Instant scriptStartTime;
    private int startingXP;
    public int getStartingConstitutionLevel() {
        return startingConstitutionLevel;
    }


    public enum ScriptState {
        INTERACTING_TEMPLE, // Interacting with the Temple of Aminishi
        CHECKING_NPC, // Checking for the specified NPCs
        MOVETOFIRSTCOORD,
        MOVETOSECONDCOORD,
        MOVETOTHIRDCOORD,
        MOVING_TO_SARKHAN,
        MOVING_TO_QUIETEST,
        MOVING_TO_CHRONICLER,
        MOVING_TO_CHEF,
        MOVING_TO_XIANG,

        INTERACTING_DOOR, // Interacting with the Door


    }
    public String[] consoleMessages = new String[15]; //<-- However many lines you want
    private int consoleIndex = 0;
    //We can't @Override since println() is final.
    public void printlnn(String message){
        println(message);
        consoleMessages[consoleIndex] = Instant.now().toString() + ": " + message;
        consoleIndex = (consoleIndex + 1) % consoleMessages.length;

    }


    @Override
    public void onLoop() {
        if (scriptRunning) {
            long currentTime = System.currentTimeMillis();

            // Check if enough time has passed since the last activation check
            if ((currentTime - lastEffectCheckTime) > effectCheckInterval) {
                ensureEffectActive();
                lastEffectCheckTime = currentTime; // Update the timestamp
            }

            manageCombatAndHealth();
            switch (currentState) {
                case INTERACTING_TEMPLE:
                    interactWithTempleOfAminishi();
                    break;
                case CHECKING_NPC:
                    checkForSpecifiedNpcs();
                    break;
                case MOVETOFIRSTCOORD:
                    MovetoFirstCoord();
                    break;
                case MOVETOSECONDCOORD:
                    MovetoSecondCoord();
                    break;
                case MOVETOTHIRDCOORD:
                    MovetoThirdCoord();
                    break;
                case MOVING_TO_SARKHAN:
                    updateStateBasedOnNpcAvailability();
                    moveToSarkhan();
                    break;
                case MOVING_TO_QUIETEST:
                    updateStateBasedOnNpcAvailability();
                    moveToQuietest();
                    break;
                case MOVING_TO_CHRONICLER:
                    updateStateBasedOnNpcAvailability();
                    moveToChronicler();
                    break;
                case MOVING_TO_CHEF:
                    updateStateBasedOnNpcAvailability();
                    moveToChef();
                    break;
                case MOVING_TO_XIANG:
                    updateStateBasedOnNpcAvailability();
                    moveToXiang();
                    break;
                case INTERACTING_DOOR:
                    interactWithDoor();
                    break;
            }
        }
    }
    private void manageCombatAndHealth() {
        net.botwithus.rs3.game.scene.entities.characters.player.Player player = getLocalPlayer();
        if (player == null) {
            printlnn("Player is null!");
            return;
        }

        int currentHealth = getLocalPlayer().getCurrentHealth();
        int maxHealth = getLocalPlayer().getMaximumHealth();
        int healthPercent = (currentHealth * 100) / maxHealth;
        int prayerPoints = getLocalPlayer().getPrayerPoints();

        // Check health and drink Saradomin brew if below threshold
        if (currentHealth <= 7000) {
            printlnn("Health is low. Drinking Saradomin brew.");

            // Interact with the Saradomin brew in your inventory or action bar
            // The parameters here are an example, adjust according to your component's specifics
            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93716700);

            // Wait until health is over 13000 or a random delay between 1600 to 2100 milliseconds
            Execution.delayUntil(2100, () -> getLocalPlayer().getCurrentHealth() > 10000 || RandomGenerator.nextInt(1600, 2100) > 0);
        }

        // Check prayer and drink Super restore potion if below threshold
        if (prayerPoints <= 5000) {
            usePrayerOrRestorePots();
        }

        // Manage Soul Split activation
        if (LOCAL_PLAYER.inCombat() && !soulSplitActive) {
            updateSoulSplitActivation();
        } else if (!LOCAL_PLAYER.inCombat() && soulSplitActive) {
            deactivateSoulSplit();
        }
    }

    public void usePrayerOrRestorePots() {
        ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

        Item prayerOrRestorePot = items.stream()
                .filter(item -> item.getName() != null && (item.getName().toLowerCase().contains("prayer") || item.getName().toLowerCase().contains("restore")))
                .findFirst()
                .orElse(null);

        if (prayerOrRestorePot != null) {
            printlnn("Drinking " + prayerOrRestorePot.getName());
            boolean success = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93716713);
            Execution.delay(RandomGenerator.nextInt(1600, 2100));

            if (!success) {
                printlnn("Failed to use " + prayerOrRestorePot.getName());
            }
        } else {
            printlnn("No Prayer or Restore pots found.");
        }
    }

    private boolean darknessChecked = false;

    public boolean isDarknessActive() {
        Component darkness = ComponentQuery.newQuery(284).spriteId(30122).results().first();
        boolean darknessActive = darkness != null;

        if (!darknessChecked && darknessActive) {
            darknessChecked = true; // Ensure we only log this once
        }

        return darknessActive;
    }

    private void activateDarkness() {
        if (!isDarknessActive()) {
            boolean success = ActionBar.useAbility("Darkness");
            if (success) {
                printlnn("Activated Darkness via ActionBar.");
            } else {
                printlnn("Failed to activate Darkness via ActionBar.");
            }
        }
    }
    private static boolean overloadChecked = false;

    private boolean isOverloadActive() {
        boolean overloadActive = VarManager.getVarbitValue(26037) != 0;

        if (!overloadChecked && overloadActive) {
            overloadChecked = true;
        }

        return overloadActive;
    }

    private void useOverload() {
        if (!isOverloadActive()) {
            String[] overloadSalveVariants = new String[]{"Elder overload salve (6)", "Elder overload salve (5)",
                    "Elder overload salve (4)", "Elder overload salve (3)", "Elder overload salve (2)",
                    "Elder overload salve (1)"};

            for (String potionName : overloadSalveVariants) {
                if (ActionBar.containsItem(potionName)) {
                    boolean successfulDrink = ActionBar.useItem(potionName, "Drink");
                    if (successfulDrink) {
                        printlnn("Drank " + potionName + " to activate Overload.");
                        break;
                    }
                }
            }
        }
    }
    public void ensureEffectActive() {
        if (!isDarknessActive()) {
            activateDarkness();
        } else {
            printlnn("Darkness is already active.");
        }

        if (!isOverloadActive()) {
            useOverload();
        } else {
            printlnn("Overload is already active.");
        }
    }


    private boolean soulSplitActive = false; // Track the activation state

    private void updateSoulSplitActivation() {
        int soulSplitEnabled = VarManager.getVarbitValue(16779);
        boolean shouldBeActive = shouldActivateSoulSplit(); // Implement this based on your conditions

        if (shouldBeActive && soulSplitEnabled != 1) {
            activateSoulSplit();
        } else if (!shouldBeActive && soulSplitEnabled == 1) {
            deactivateSoulSplit();
        }
    }

    private void activateSoulSplit() {
        if (!soulSplitActive) {
            printlnn("Activating Soul Split.");
            if (ActionBar.useAbility("Soul Split")) {
                printlnn("Soul Split activated successfully.");
                soulSplitActive = true;
            } else {
                printlnn("Failed to activate Soul Split.");
            }
        }
    }

    private void deactivateSoulSplit() {
        if (soulSplitActive) {
            printlnn("Deactivating Soul Split.");
            // Assuming deactivation works the same way, toggle the ability again
            if (ActionBar.useAbility("Soul Split")) {
                printlnn("Soul Split deactivated.");
                soulSplitActive = false;
            } else {
                printlnn("Failed to deactivate Soul Split.");
            }
        }
    }

    private boolean shouldActivateSoulSplit() {
        return LOCAL_PLAYER.inCombat(); // Assuming a player object exists and has an inCombat method
    }
    private void updateStateBasedOnNpcAvailability() {
        if (isNpcAvailable("Sarkhan the Serpentspeaker")) {
            currentState = ScriptState.MOVING_TO_SARKHAN;
        } else if (isNpcAvailable("Oyu the Quietest")) {
            currentState = ScriptState.MOVING_TO_QUIETEST;
        } else if (isNpcAvailable("Olivia the Chronicler")) {
            currentState = ScriptState.MOVING_TO_CHRONICLER;
        } else if (isNpcAvailable("Ahoeitu the Chef")) {
            currentState = ScriptState.MOVING_TO_CHEF;
        } else if (isNpcAvailable("Xiang the Water-shaper")) {
            currentState = ScriptState.MOVING_TO_XIANG;
        } else {
            // No specified NPCs are available
            currentState = ScriptState.INTERACTING_DOOR;
        }
    }
    private boolean validatePreconditions() {
        if (!scriptRunning || !isGameLoggedIn()) {
            printlnn("Script is not running or game is not logged in.");
            return false;
        }
        if (postTempleEntryCoordinate == null) {
            printlnn("Post temple entry coordinate not set.");
            return false;
        }
        return true;
    }
    private void moveToClosestWaypoint(Coordinate[] waypoints) {
        Coordinate currentPlayerPosition = Client.getLocalPlayer().getCoordinate(); // Assuming a method to get player's current position
        Coordinate closestWaypoint = findClosestWaypoint(currentPlayerPosition, waypoints);

        for (int i = 0; i < waypoints.length; i++) {
            if (waypoints[i].equals(closestWaypoint)) {
                for (int j = i; j < waypoints.length; j++) {
                    final Coordinate targetWaypoint = waypoints[j];
                    printlnn("Moving to waypoint: " + targetWaypoint);

                    if (Client.getLocalPlayer().isMoving() && Distance.between(currentPlayerPosition, targetWaypoint) > 8) {
                        printlnn("Using Surge to reach waypoint: " + targetWaypoint);
                        useAbility("Surge"); // Hypothetical method to use an ability
                        Execution.delay(1200); // Assuming a delay to wait for the ability's effect before continuing
                    }

                    Travel.walkTo(targetWaypoint.getX(), targetWaypoint.getY());
                    boolean reached = Execution.delayUntil(30000, () -> Distance.between(Client.getLocalPlayer().getCoordinate(), targetWaypoint) <= 8);
                    if (!reached) {
                        printlnn("Failed to reach waypoint at [" + targetWaypoint.getX() + ", " + targetWaypoint.getY() + "] within the timeout.");
                        break; // Exit the loop if unable to reach a waypoint
                    }
                    waitForCombatToEnd();
                }
                break;
            }
        }
    }
    private void useAbility(String abilityName) {
        ActionBar.useAbility("Surge");
        printlnn("Ability " + abilityName + " used."); // Placeholder
    }
    private Coordinate[] waypointsToSarkhan() {
        return new Coordinate[]{
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(65, 67), RandomGenerator.nextInt(15, 17), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(64, 66), RandomGenerator.nextInt(25, 27), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(64, 66), RandomGenerator.nextInt(33, 35), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(64, 66), RandomGenerator.nextInt(41, 43), 0)
        };
    }


    private Coordinate[] waypointsToQuietest() {
        return new Coordinate[]{
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(65, 67), RandomGenerator.nextInt(15, 17), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(88, 90), RandomGenerator.nextInt(19, 21), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(91, 93), RandomGenerator.nextInt(32, 34), 0)
        };
    }


    private Coordinate[] waypointsToChef() {
        return new Coordinate[]{
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(64, 66), RandomGenerator.nextInt(14, 16), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(82, 84), RandomGenerator.nextInt(10, 12), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(86, 88), RandomGenerator.nextInt(-7, -5), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(95, 97), RandomGenerator.nextInt(-19, -17), 0)
        };
    }

    private Coordinate[] waypointsToXiang() {
        return new Coordinate[]{
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(65, 67), 15, 17),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(88,90), 19, 21),
        };
    }

    private Coordinate[] waypointsToChronicler() {
        return new Coordinate[]{
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(65, 67), RandomGenerator.nextInt(15, 17), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(88, 90), RandomGenerator.nextInt(19, 21), 0),
                postTempleEntryCoordinate.derive(RandomGenerator.nextInt(97, 99), RandomGenerator.nextInt(4, 6), 0)
        };
    }


    private boolean isNpcAvailable(String npcName) {
        // Check if a specific NPC is available
        return NpcQuery.newQuery().name(npcName).results().stream().findAny().isPresent();
    }
    private int getRandomOffset() {
        return net.botwithus.rs3.util.RandomGenerator.nextInt(-2, 3); // Assuming inclusive lower bound and exclusive upper bound
    }
    private void interactWithTempleOfAminishi() {
        if (!scriptRunning && !isGameLoggedIn()) return;
        assert getLocalPlayer() != null;

        Coordinate startingCoordinate = new Coordinate(2094, 11353, 0);
        if (isNear(startingCoordinate, 10)) {
            printlnn("Player is within range of the starting point.");

            boolean templeInteracted = SceneObjectQuery.newQuery().name("Temple of Aminishi").results().stream()
                    .findFirst()
                    .map(sceneObject -> sceneObject.interact("Enter"))
                    .orElse(false);

            if (templeInteracted) {
                printlnn("Interacted with Temple of Aminishi. Waiting for interface 1188 to open.");
                Execution.delayUntil((5000), () -> Interfaces.isOpen(1188));

                if (Interfaces.isOpen(1188)) {
                    println("Interface 1188 is open. Proceeding with dialog interactions.");
                    Execution.delay(RandomGenerator.nextInt(1000, 2000));
                    Dialog.interact("No.");
                    Execution.delay(RandomGenerator.nextInt(1000, 2000));
                    Dialog.interact("Normal mode");
                    // Delay to account for dialog interaction and possible movement
                    Execution.delay(RandomGenerator.nextInt(3000, 4000));

                    Coordinate postInteractionLocation = getCurrentLocation();
                    if (!postInteractionLocation.equals(startingCoordinate)) {
                        printlnn("Coordinate change detected. Interaction successful.");
                        postTempleEntryCoordinate = postInteractionLocation;
                        printlnn("Recorded post-temple entry coordinate: " + postTempleEntryCoordinate);
                        currentState = ScriptState.CHECKING_NPC; // Update the script state to check for specified NPCs
                    } else {
                        printlnn("No coordinate change detected. Interaction may have failed. Retrying...");
                        // Optionally, you can retry the interaction or handle the failure case differently here.
                    }
                } else {
                    printlnn("Failed to open interface 1188 after interacting with the temple.");
                }
            } else {
                printlnn("Failed to find or interact with the Temple of Aminishi.");
            }
        } else {
            printlnn("Player is not within range of the starting point.");
        }
    }

    private void checkForSpecifiedNpcs() {
        List<Npc> specifiedNpcs = NpcQuery.newQuery().name("Elite Sotapanna", "Sarkhan the Serpentspeaker", "Oyu the Quietest", "Olivia the Chronicler", "Ahoeitu the Chef", "Xiang the Water-shaper").results().stream().toList();

        // Collect names of available NPCs, excluding "Elite Sotapanna"
        List<String> availableNpcNames = specifiedNpcs.stream()
                .filter(npc -> !npc.getName().equals("Elite Sotapanna"))
                .map(Npc::getName)
                .collect(Collectors.toList());

        if (availableNpcNames.size() < 2) {
            printlnn("Less than 2 specified NPCs found, Resetting Instance.");
            currentState = ScriptState.INTERACTING_DOOR; // Update the script state to move to the first wall
        } else {
            printlnn("At least 2 specified NPCs found: " + String.join(", ", availableNpcNames) + ". Preparing to go to first Wall.");
            currentState = ScriptState.MOVETOFIRSTCOORD; // Update the script state to interact with the door
        }
    }
    private void MovetoFirstCoord() {
        if (!validatePreconditions()) return;

        Coordinate waypoint1 = postTempleEntryCoordinate.derive(RandomGenerator.nextInt(25, 27), RandomGenerator.nextInt(4, 6), 0);
        printlnn("Moving to first waypoint at: " + waypoint1);
        moveToWaypoint(waypoint1);
        Execution.delayUntil (30000, () -> isNear(waypoint1, 2));
        currentState = ScriptState.MOVETOSECONDCOORD;
    }

    private void MovetoSecondCoord() {
        if (!validatePreconditions()) return;

        Coordinate waypoint2 = postTempleEntryCoordinate.derive(RandomGenerator.nextInt(35, 37), RandomGenerator.nextInt(15, 17), 0);
        printlnn("Moving to second waypoint at: " + waypoint2);
        moveToWaypoint(waypoint2);
        Execution.delayUntil (30000, () -> isNear(waypoint2, 2));
        currentState = ScriptState.MOVETOTHIRDCOORD;
    }

    private void MovetoThirdCoord() {
        if (!validatePreconditions()) return;

        Coordinate waypoint3 = postTempleEntryCoordinate.derive(RandomGenerator.nextInt(52, 54), RandomGenerator.nextInt(13, 15), 0);
        printlnn("Moving to third waypoint at: " + waypoint3);
        moveToWaypoint(waypoint3);
        Execution.delayUntil (30000, () -> isNear(waypoint3, 2));
        updateStateBasedOnNpcAvailability();
    }
    private void moveToWaypoint(Coordinate waypoint) {
        if (!validatePreconditions()) return;
        manageCombatAndHealth();

        final long timeout = 15000; // 15 seconds timeout
        final long combatTimeout = 5000; // 5 seconds out of combat before forcing the next waypoint
        final long startTime = System.currentTimeMillis();
        final int nearDistance = 2; // Distance to consider we have reached the waypoint
        long lastInCombatTime = System.currentTimeMillis(); // Track the last time in combat
        boolean hasEngagedInCombat = false; // Flag to track if the player has engaged in combat

        printlnn("Attempting to move to waypoint: " + waypoint);
        Travel.walkTo(waypoint.getX(), waypoint.getY());

        while (System.currentTimeMillis() - startTime < timeout && scriptRunning) {
            if (isNear(waypoint, nearDistance)) {
                printlnn("Reached waypoint at: " + waypoint);
                waitForCombatToEnd();
                updateStateBasedOnNpcAvailability();
                return; // Exit the method once the waypoint is reached
            }

            if (isInCombat()) {
                hasEngagedInCombat = true; // Set flag to true as player has engaged in combat
                lastInCombatTime = System.currentTimeMillis(); // Update last in combat time
            } else if (hasEngagedInCombat && (System.currentTimeMillis() - lastInCombatTime) > combatTimeout) {
                printlnn("Out of combat for more than 5 seconds, forcing move to next waypoint.");
                return; // Exit to allow moving to the next waypoint
            }

            // Check for scriptRunning inside the loop to ensure immediate response to stop request
            if (!scriptRunning) {
                printlnn("Script stopped by user.");
                return; // Exit the method if the script is no longer running
            }

            Execution.delay(1000); // Wait a bit before checking the conditions again
        }

        printlnn("Failed to reach waypoint at: " + waypoint + " within timeout.");
    }

    private void waitForCombatToEnd() {
        if (!scriptRunning || !isGameLoggedIn()) return;
        printlnn("Checking for nearby NPCs to attack.");
        manageCombatAndHealth();

        List<Npc> nearbyNpcs = NpcQuery.newQuery()
                .name("Elite Sotapanna", "Sarkhan the Serpentspeaker", "Oyu the Quietest",
                        "Olivia the Chronicler", "Ahoeitu the Chef", "Xiang the Water-shaper")
                .results().stream()
                .filter(npc -> Distance.between(getLocalPlayer().getCoordinate(), npc.getCoordinate()) <= 5)
                .sorted(Comparator.comparingInt(Npc::getCurrentHealth))
                .collect(Collectors.toList());

        if (!nearbyNpcs.isEmpty()) {
            // Attack the NPC with the lowest health
            Npc targetNpc = nearbyNpcs.get(0);
            printlnn("Attacking NPC: " + targetNpc.getName() + " at location: " + targetNpc.getCoordinate());
            // Assume attackNpc(Npc npc) is a method to initiate combat with an NPC
            attackNpc(targetNpc);


            // Wait for combat to end get
            printlnn("Waiting for combat to end.");
            Execution.delayUntil(300000, () -> !isInCombat()); // Extend the wait time if necessary
            updateStateBasedOnNpcAvailability();
        } else {
            printlnn("No suitable NPCs found.");
        }

        printlnn("Combat ended or no NPCs to engage, resuming movement.");
    }

    // Conceptual method to get NPCs within a radius of the player
    private List<Npc> getNpcsWithinRadius(int radius) {
        Coordinate playerLocation = getLocalPlayer().getCoordinate();
        return NpcQuery.newQuery()
                .inside(playerLocation.getArea()) // Assuming such method exists to define an area around the player
                .results().stream()
                .filter(npc -> npc.getCurrentHealth() > 0) // Filter out NPCs that are already dead
                .collect(Collectors.toList());
    }

    // Conceptual method to attack an NPC
    private void attackNpc(Npc npc) {
        manageCombatAndHealth();
        // Assuming an interact method that allows specifying the type of interaction, such as "Attack"
        npc.interact("Attack");
        // Wait for the interaction to be acknowledged by the game, might need a delay or verification
        Execution.delay(1000); // Conceptual delay to ensure the attack command is processed
    }





    // Example method to check if the player is in combat
    private boolean isInCombat() {
        manageCombatAndHealth();
        if (getLocalPlayer() != null) {
            return getLocalPlayer().inCombat();
        }
        // Implementation to check if the player is currently in combat
        // This could involve checking player's health, status effects, or specific game mechanics
        return false; // Placeholder, replace with actual logic
    }
    private void moveToSarkhan() {
        if (!validatePreconditions()) return;
        manageCombatAndHealth();
        moveToClosestWaypoint(waypointsToSarkhan());
        // Update script state or perform further actions as needed
        updateStateBasedOnNpcAvailability();
    }

    private void moveToQuietest() {
        manageCombatAndHealth();
        if (!validatePreconditions()) return;
        moveToClosestWaypoint(waypointsToQuietest());
        updateStateBasedOnNpcAvailability();
    }


    private void moveToXiang() {
        manageCombatAndHealth();
        if (!validatePreconditions()) return;
        moveToClosestWaypoint(waypointsToXiang());
        updateStateBasedOnNpcAvailability();
        // Further actions or state updates as required
    }

    private void moveToChef() {
        manageCombatAndHealth();
        if (!validatePreconditions()) return;
        moveToClosestWaypoint(waypointsToChef());
        updateStateBasedOnNpcAvailability();
        // Update script state or perform further actions as needed
    }

    private void moveToChronicler() {
        manageCombatAndHealth();
        if (!validatePreconditions()) return;
        moveToClosestWaypoint(waypointsToChronicler());
        updateStateBasedOnNpcAvailability();
        // Further actions or state updates as required
    }


    private Coordinate[] interactWithDoor() {
        if (!scriptRunning || !isGameLoggedIn()) return new Coordinate[0];

        if (postTempleEntryCoordinate == null) {
            printlnn("Post-temple entry coordinate is not recorded.");
            return new Coordinate[0];
        }

        Coordinate[] waypoints = new Coordinate[]{
                postTempleEntryCoordinate.derive(88, -8, 0),
                postTempleEntryCoordinate.derive(83, 12, 0),
                postTempleEntryCoordinate.derive(58, 14, 0),
                postTempleEntryCoordinate.derive(29, 3, 0),
                postTempleEntryCoordinate
        };

        // Check if the player is already at the postTempleEntryCoordinate
        if (Distance.between(getCurrentLocation(), postTempleEntryCoordinate) <= 8) {
            printlnn("At the door's location. Attempting to interact directly with the door.");
            interactDirectlyWithDoor(); // Direct interaction with the door
            currentState = ScriptState.INTERACTING_TEMPLE; // Proceed to the next state
            return new Coordinate[0];
        } else {
            printlnn("Moving to the door's location.");
            navigateUsingWaypointsToDoor(waypoints);
        }

        // After navigating to the door, interact with it
        interactDirectlyWithDoor(); // This function encapsulates the door interaction logic
        currentState = ScriptState.INTERACTING_TEMPLE; // Update the script state after interacting with the door
        return new Coordinate[0]; // Return an empty array to indicate completion
    }

    private void navigateUsingWaypointsToDoor(Coordinate[] waypoints) {
        if (!validatePreconditions()) return;

        Coordinate currentLocation = getCurrentLocation();
        // Find the closest waypoint to start with
        Coordinate closestWaypoint = findClosestWaypoint(currentLocation, waypoints);
        int startIndex = java.util.Arrays.asList(waypoints).indexOf(closestWaypoint);

        for (int i = startIndex; i < waypoints.length; i++) {
            if (!scriptRunning) {
                printlnn("Script stopped by user. Halting navigation.");
                return;
            }

            Coordinate waypoint = waypoints[i];
            printlnn("Navigating to waypoint at [" + waypoint.getX() + ", " + waypoint.getY() + "].");
            Travel.walkTo(waypoint.getX(), waypoint.getY());

            // Wait until the waypoint is reached or a timeout occurs
            boolean reached = Execution.delayUntil(30000, () -> isNear(waypoint, 3));
            if (!reached) {
                printlnn("Failed to reach waypoint at [" + waypoint.getX() + ", " + waypoint.getY() + "] within the timeout.");
                // Decide whether to try the next waypoint, retry, or exit based on your script logic
                break;
            }

            // Optionally wait for combat to end if necessary
            waitForCombatToEnd();
        }

        // After navigating to the last waypoint, attempt to interact with the door
        interactDirectlyWithDoor();
    }

    private void interactDirectlyWithDoor() {
        if (!validatePreconditions()) return;

        boolean doorInteracted = SceneObjectQuery.newQuery().name("Door").results().stream()
                .findFirst()
                .map(sceneObject -> sceneObject.interact("Enter"))
                .orElse(false);

        if (doorInteracted) {
            printlnn("Interacted with Door at post-temple entry location.");
            Execution.delay(5000); // Adjust based on your needs
        } else {
            printlnn("Failed to find or interact with the Door at post-temple entry location.");
        }
    }

    private Coordinate getCurrentLocation() {
        // Attempt to get the local player object
        net.botwithus.rs3.game.scene.entities.characters.player.Player localPlayer = getLocalPlayer();

        // Check if the local player object is not null
        if (localPlayer != null) {
            // Return the current coordinate of the local player
            return localPlayer.getCoordinate();
        } else {
            // Return a default coordinate if the local player object is null
            // Consider handling this case appropriately, as it means the player object could not be retrieved
            printlnn("Local player object is null.");
            return new Coordinate(0, 0, 0); // Placeholder value, indicating an error or inability to retrieve the player's location
        }
    }

    private Coordinate findClosestWaypoint(Coordinate currentLocation, Coordinate[] waypoints) {
        Coordinate closest = null;
        double minDistance = Double.MAX_VALUE;
        for (Coordinate waypoint : waypoints) {
            double distance = Distance.between(currentLocation, waypoint);
            if (distance < minDistance) {
                closest = waypoint;
                minDistance = distance;
            }
        }
        return closest;
    }
}


    // Utility method to get a random offset within ±2



