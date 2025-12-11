package com.luxshare.fastsp;

import android.content.SharedPreferences;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * The interface Enhanced editor.
 *
 * @author Luxshare
 * @version version
 */
public interface EnhancedEditor extends SharedPreferences.Editor {

    /**
     * Put serializable enhanced editor.
     *
     * @param key   the key
     * @param value the value
     * @return the enhanced editor
     */
    EnhancedEditor putSerializable(String key, @Nullable Serializable value);
}
