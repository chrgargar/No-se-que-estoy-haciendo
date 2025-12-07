package com.cobblemon.mdks.cobblepass.util;

import java.util.Arrays;
import java.util.List;

public class Constants {
    // Mod Info
    public static final String MOD_ID = "cobblepass";
    public static final String MOD_NAME = "Cobblemon Battle Pass";
    
    // Command Info
    public static final List<String> COMMAND_ALIASES = Arrays.asList("bp", "pass", "bpass");
    
    // Message Prefixes
    public static final String PREFIX = "§7[§eBattlePass§7] §f";
    public static final String ERROR_PREFIX = "§7[§eBattlePass§7] §f";
    public static final String SUCCESS_PREFIX = "§7[§eBattlePass§7] §f";
    
    // Permission Nodes
    public static final String PERM_COMMAND_BASE = "battlepass.command";
    public static final String PERM_COMMAND_ADMIN = "battlepass.admin";
    public static final String PERM_COMMAND_RELOAD = "battlepass.reload";
    public static final String PERM_COMMAND_START_SEASON = "battlepass.season.start";
    
    // Config Paths
    public static final String CONFIG_DIR = "config/cobblepass";
    public static final String CONFIG_PATH = CONFIG_DIR;
    public static final String CONFIG_FILE = "config.json";
    public static final String TIERS_FILE = "tiers.json";
    public static final String PLAYERS_PATH = CONFIG_DIR + "/players";
    public static final String PLAYER_DATA_DIR = PLAYERS_PATH;
    
    // Version Info
    public static final String CONFIG_VERSION = "1.0";
    public static final String PLAYER_DATA_VERSION = "1.0";
    
    // Battle Pass Constants
    public static final int MAX_LEVEL = 100;
    public static final int XP_PER_LEVEL = 1000;
    public static final double XP_MULTIPLIER = 1.1;
    
    // Default Values
    public static final int DEFAULT_MAX_LEVEL = 100;
    public static final int DEFAULT_XP_PER_LEVEL = 1000;
    public static final int DEFAULT_CATCH_XP = 100;
    public static final int DEFAULT_DEFEAT_XP = 50;
    public static final int DEFAULT_EVOLVE_XP = 150;
    public static final int DEFAULT_LEVELUP_XP = 75;
    public static final long DEFAULT_PREMIUM_COST = 1000;
    public static final boolean DEFAULT_ENABLE_PERMISSION_NODES = true;
    
    // Time Constants
    public static final long MILLIS_PER_SECOND = 1000L;
    public static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    public static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
    public static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;
    
    // Battle Pass Messages
    public static final String MSG_XP_GAINED = PREFIX + "¡Ganaste §e%d§f XP! (§e%d§f/§e%d§f)";
    public static final String MSG_LEVEL_UP = SUCCESS_PREFIX + "¡Alcanzaste el nivel §e%d§f!";
    public static final String MSG_REWARD_CLAIM = SUCCESS_PREFIX + "¡Reclamaste la recompensa del nivel §e%d§f!";
    public static final String MSG_PREMIUM_UNLOCKED = SUCCESS_PREFIX + "¡Desbloqueaste el Pase de Batalla Premium!";
    public static final String MSG_NO_REWARD = ERROR_PREFIX + "¡No hay recompensa disponible en el nivel §e%d§f!";
    public static final String MSG_ALREADY_CLAIMED = ERROR_PREFIX + "¡Ya reclamaste esta recompensa!";
    public static final String MSG_ALREADY_CLAIMED_LEVEL = ERROR_PREFIX + "¡Ya reclamaste la recompensa del nivel §e%d§f!";
    public static final String MSG_LEVEL_NOT_REACHED = ERROR_PREFIX + "¡Aún no has alcanzado el nivel §e%d§f!";
    public static final String MSG_NOT_PREMIUM = ERROR_PREFIX + "¡Esta es una recompensa premium! Contacta a un administrador para obtener acceso premium.";
    public static final String MSG_CONFIG_RELOADED = SUCCESS_PREFIX + "¡Configuración recargada!";
    
    // Season Messages
    public static final String MSG_SEASON_STARTED = SUCCESS_PREFIX + "¡La Temporada %d del Pase de Batalla ha comenzado!";
    public static final String MSG_SEASON_ALREADY_ACTIVE = ERROR_PREFIX + "¡Ya hay una temporada activa! (Temporada %d)";
    public static final String MSG_NO_ACTIVE_SEASON = ERROR_PREFIX + "¡No hay ninguna temporada activa actualmente!";
    public static final String MSG_SEASON_TIME_REMAINING = PREFIX + "La Temporada %d termina en %s";
}
