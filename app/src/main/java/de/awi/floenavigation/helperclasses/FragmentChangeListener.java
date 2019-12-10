package de.awi.floenavigation.helperclasses;

import androidx.fragment.app.Fragment;

/**
 * This Interface provides Activities and {@link Fragment}s with a method to replace the currently running fragment with another {@link Fragment}.
 *
 * @see Fragment
 * @see android.app.Activity
 */
public interface FragmentChangeListener {
    /**
     * Replaces the currently running Fragment with another Fragment.
     * @param fragment the fragment to replace the currently running fragment with.
     */
    public void replaceFragment(Fragment fragment);
}
