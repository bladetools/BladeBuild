package org.blade.build;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("manifest_filter")
public class BladePluginExtension {
    public String name;
    public String version;
    public String author;
    public String[] packageNames;
    public String[] install = new String[]{"app"};
    public boolean coldStartOnly = false;

    public BladePluginExtension() {

    }
}
