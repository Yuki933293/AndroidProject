package com.luxshare.fastsp;

import android.content.SharedPreferences;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * The interface Enhanced shared preferences.
 *
 * @author Luxshare
 * @version version
 */
public interface EnhancedSharedPreferences extends SharedPreferences {

    /**
     * Gets serializable.
     *
     * @param key      the key
     * @param defValue the def value
     * @return the serializable
     */
    Serializable getSerializable(String key, @Nullable Serializable defValue);
}
