package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.model.JobType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class JobMenuHolder implements InventoryHolder {
    public enum Mode { MAIN, PROGRESSION }

    private Inventory inventory;
    private final Mode mode;
    private final JobType jobType;

    public JobMenuHolder(Mode mode, JobType jobType) {
        this.mode = mode;
        this.jobType = jobType;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public Mode getMode() {
        return this.mode;
    }

    public JobType getJobType() {
        return this.jobType;
    }
}
