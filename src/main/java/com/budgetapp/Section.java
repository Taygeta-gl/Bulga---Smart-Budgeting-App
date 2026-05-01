package com.budgetapp;

public class Section {
    private static int idCounter = 0;
    private int id;
    private String name;
    private Importance importance;
    private String description;
    private double base_cost; // base cost of the section, if it has one, if not it will be 0, and we will
                              // calculate the total amount based on the subsections
    private Section[] subsections = new Section[0]; // Initialize with an empty array to avoid null pointer exceptions

    // Constructor if amount is provided
    public Section(String name, String description, double base_cost, Importance importance) {
        this.name = name;
        this.description = description;
        this.base_cost = base_cost; // for that option we will have parent + children, so we will do both
        this.importance = importance;
        idCounter++;
        this.id = ++idCounter;
    }

    // Constructor if amount is not provided, need to calculate
    public Section(String name, String description, Importance importance) {
        this.name = name;
        this.description = description;
        this.base_cost = 0; // Default value, can be updated later
        this.importance = importance;
        idCounter++;
        this.id = ++idCounter;
    }

    // Getters and Setters
    public int getId() {
        return idCounter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getbase_cost() {
        return base_cost;
    }

    public void setbase_cost(double base_cost) {
        this.base_cost = base_cost;
    }

    public Importance getImportance() {
        return importance;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
    }

    // there feels like a problem with this, because the return value is null
    public Section[] getSubsections() {
        return subsections;
    }
    // not gonna set subsections, we will add them one by one, so we will have an
    // addSubsection method

    // subsection no amount specified

    public void addSubsection(Section subsection) {
        Section[] newArray = new Section[subsections.length + 1];
        for (int i = 0; i < subsections.length; i++) {
            newArray[i] = subsections[i];
        }

        newArray[newArray.length - 1] = subsection;
        this.subsections = newArray;
    }

    public void removeSubsection(Section subsection) {
        if (subsections.length == 0)
            return;
        // Remove the subsection from the array of subsections, it is array, so we
        // adjust it
        Section[] newSubsections = new Section[subsections.length - 1];
        int index = 0;
        for (Section sub : subsections) {
            // removes by skipping it, when adding to new array
            if (sub != subsection) {
                newSubsections[index] = sub;
                index++;
            }
        }
        // Update the subsections array
        subsections = newSubsections;

    }

    public double getGrandTotal() {

        double total = this.base_cost;

        if (this.subsections != null) {
            for (Section sub : this.subsections) {
                total += sub.getGrandTotal();
            }
        }
        return total;
    }

    public String toString() {
        return "Section{name='" + name + "', importance=" + importance + ", base_cost=" + base_cost + ", grand_total="
                + getGrandTotal() + "}";
    }

}