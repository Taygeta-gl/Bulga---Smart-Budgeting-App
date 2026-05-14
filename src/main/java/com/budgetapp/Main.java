/** 
 * @author: Saadat Emilbekova
 * @gmail: saadat.universe@gmail.com
 * @date: 2026-05-14
 */

package com.budgetapp;

public class Main {

    public static void main(String[] args) {
        // Create the main section
        Section mainSection = new Section("Main Section", "This is the main section", 1000, Importance.HIGH);

        // Create subsections
        Section subsection1 = new Section("Subsection 1", "This is the first subsection", 500, Importance.MEDIUM);
        Section subsection2 = new Section("Subsection 2", "This is the second subsection", 300, Importance.LOW);

        // Add subsections to the main section
        mainSection.addSubsection(subsection1);
        mainSection.addSubsection(subsection2);

        // Print the main section and its grand total
        System.out.println(mainSection);
    }
}