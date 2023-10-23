package mealplanner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final List<Meal> meals = new ArrayList<>();
    private static final Scanner scanner = new Scanner(System.in);
    private static DatabaseManager dbManager;  // Neu

    public static void main(String[] args) throws SQLException
    {
        try {
            dbManager = new DatabaseManager();
            dbManager.createTables();
            meals.addAll(dbManager.getMeals());  // Neu
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Database connection failed. Exiting...");
            System.exit(-1);
        }

        while (true) {
            System.out.println("What would you like to do (add, show, plan, save, exit)?");
            String command = scanner.nextLine();
            command = command.trim().toLowerCase();
            //scanner.nextLine(); Hier debuggen

            switch (command) {
                // Nur um tables zum Testen zu löschen!
                case "drop":
                    try
                    {
                        dbManager.dropTables();
                    } catch (SQLException e){
                        e.printStackTrace();
                    }
                    break;
                case "add":
                    addMeal();
                    break;
                case "show":
                    showMeals();
                    break;
                case "plan":
                    planMeals();
                    break;
                case "save":
                    saveShoppingListToText();
                    break;
                case "exit":
                    System.out.println("Bye!");
                    try {
                        dbManager.closeConnection();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return;
                default:
                    //System.out.println("Invalid command. Try again.");
            }
        }
    }

    private static void saveShoppingListToText() throws SQLException
    {
        if (dbManager.isPlanAvailable()) {//mit Wert ersetzen, ob ein Plan vorhanden ist!
            System.out.println("Input a filename:");
            String fileName = scanner.nextLine();

            //Hash Map (Key als Zutat, Value als Anzahl)
            HashMap<String, Integer> shoppingList = new HashMap<String, Integer>();

            //Schleife durch alle Tage
            String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            for (String day : daysOfWeek)
            {
                try
                {
                    List<Meal> meals = dbManager.getMealsByDay(day);
                    for (Meal meal : meals) {
                        String[] ingredients = meal.getIngredients();
                        for (String ingredient : ingredients) {
                            //Zählt die Zutaten
                            shoppingList.put(ingredient, shoppingList.getOrDefault(ingredient, 0) + 1);
                        }
                    }
                }catch (SQLException e) {
                    e.printStackTrace();
                    System.out.println("Failed to retrieve meals for " + day);
                    return;
                }
            }
            /*Hash Map --> .txt in folgenden Format
            eggs
            tomato x3
            beef
            broccoli
            salmon
            chicken x2
             */
            // Schreiben der Hash Map in eine .txt-Datei
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                for (Map.Entry<String, Integer> entry : shoppingList.entrySet()) {
                    String ingredient = entry.getKey();
                    Integer count = entry.getValue();
                    if (count > 1) {
                        writer.write(ingredient + " x" + count);
                    } else {
                        writer.write(ingredient);
                    }
                    writer.newLine();
                }
                System.out.println("Saved!");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to save shopping list.");
            }
        } else {
            System.out.println("Unable to save. Plan your meals first.");
        }
    }

    private static void planMeals() {

        try
        {
            dbManager.deleteOldPlan();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};//Auswahl für wie viele Wochentage geplant werden soll
        // Schleife über die Wochentage
        for (String day : daysOfWeek) {
            System.out.println(day);
            try {
                Meal selectedBreakfast = selectMealForCategory("breakfast", day);
                dbManager.addPlan(selectedBreakfast.getMealId(), day, selectedBreakfast.getCategory(), selectedBreakfast.getName());
                Meal selectedLunch = selectMealForCategory("lunch", day);
                dbManager.addPlan(selectedLunch.getMealId(), day, selectedLunch.getCategory(), selectedLunch.getName());
                Meal selectedDinner = selectMealForCategory("dinner", day);
                dbManager.addPlan(selectedDinner.getMealId(), day, selectedDinner.getCategory(), selectedDinner.getName());

                System.out.println("Yeah! We planned the meals for " + day + ".");
                System.out.println(); // Leerzeile für die Übersicht

            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Failed to retrieve meals from database.");
            }
        }
        // Ausgabe-Methode...
        printPlanWeek();
    }
    private static void printPlanWeek() {
        // Array mit den Namen der Wochentage
        String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        // Schleife über die Wochentage
        for (String day : daysOfWeek) {
            System.out.println(day);  // Gibt den Namen des aktuellen Wochentags aus

            // Abfrage und Ausgabe der Mahlzeiten für den aktuellen Wochentag
            System.out.println("Breakfast: " + dbManager.getPlanByDay(day, "breakfast"));
            System.out.println("Lunch: " + dbManager.getPlanByDay(day, "lunch"));
            System.out.println("Dinner: " + dbManager.getPlanByDay(day, "dinner"));

            System.out.println();  // Leerzeile für die Übersicht
        }
    }

    private static Meal selectMealForCategory(String category, String day) throws SQLException {
        List<Meal> meals = dbManager.getMealsByCategory(category);
        Meal selectedMeal = null;

        printMealsInAlphabeticalOrder(meals);
        System.out.println("Choose the " + category + " for " + day + " from the list above: ");

        while (selectedMeal == null) {

            String selectedMealName = scanner.nextLine();
            //scanner.nextLine(); falls nextLine(); nicht hilft hier einfügen
            for (Meal meal : meals) {
                if (meal.getName().equals(selectedMealName)) {
                    selectedMeal = meal;
                    //System.out.println("Meal ID: " + meal.getMealId());
                    //System.out.println("Meal Name: " + meal.getName());
                    //dbManager.addPlan(meal.getMealId(), day.toLowerCase());
                    break; // Schleife verlassen, da wir die gewünschte Mahlzeit gefunden haben
                }

            }
            if (selectedMeal == null) {
                System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
            }

            // Schleife startet neu da die Auswahl nicht gepasst hat
        }

        return selectedMeal;
    }

    // Hilfsmethode, um Mahlzeiten nach Kategorie zu filtern
    private static List<Meal> filterMealsByCategory(String category) {
        List<Meal> mealsByCategory = new ArrayList<>();
        for (Meal meal : meals) {
            if (meal.getCategory().equalsIgnoreCase(category)) {
                mealsByCategory.add(meal);
            }
        }
        return mealsByCategory;
    }

    // Hilfsmethode, um Mahlzeiten in alphabetischer Reihenfolge auszugeben
    private static void printMealsInAlphabeticalOrder(List<Meal> mealsToPrint) {
        mealsToPrint.sort(Comparator.comparing(Meal::getName));
        for (Meal meal : mealsToPrint) {
            System.out.println(meal.getName());
        }
    }

    // Hilfsmethode, um eine Mahlzeit anhand der mealId zu finden
    private static Meal findMealById(List<Meal> mealsToSearch, String mealName) {
        for (Meal meal : mealsToSearch) {
            if (meal.getName().equals(mealName)) {
                return meal;
            }
        }
        return null; // Mahlzeit wurde nicht gefunden (Sollte besser behandelt werden)
    }


    public static void addMeal() {
        String category;
        String mealName;
        String[] ingredients;

        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        while (true) {
            category = scanner.nextLine().toLowerCase().trim();
            if (category.equals("breakfast") || category.equals("lunch") || category.equals("dinner")) {
                break;
            } else {
                System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            }
        }

        System.out.println("Input the meal's name:");
        while (true) {
            mealName = scanner.nextLine();
            if (mealName.matches("^[a-zA-Z]+[a-zA-Z\\s]*$")) {
                break;
            } else {
                System.out.println("Wrong format. Use letters only!");
            }
        }

        System.out.println("Input the ingredients:");
        while (true) {
            String input = scanner.nextLine().trim();  // Entferne Whitespace am Anfang und am Ende
            String[] tempIngredients = input.split(",\\s*");

            boolean valid = true;
            for (String ingredient : tempIngredients) {
                if (!ingredient.matches("^[a-zA-Z]+(\\s*[a-zA-Z]*)*$") || ingredient.isEmpty()) {
                    valid = false;
                    break;
                }
            }

            if (valid && tempIngredients.length > 0 && !input.endsWith(", ") && !input.endsWith(",")) {
                ingredients = tempIngredients;
                break;
            } else {
                System.out.println("Wrong format. Use letters only!");
            }
        }

        try {
            dbManager.addMeal(new Meal(1, category, mealName, ingredients));  // Aktualisiert
            System.out.println("The meal has been added!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to add meal to database.");
        }
    }

    public static void showMeals() {
        String categoryToPrint;
        System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
        while (true) {
            categoryToPrint = scanner.nextLine().toLowerCase().trim();
            if (categoryToPrint.equals("breakfast") || categoryToPrint.equals("lunch") || categoryToPrint.equals("dinner")) {
                break;
            } else {
                System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            }
        }
        try {
            List<Meal> meals = dbManager.getMealsByCategory(categoryToPrint);  // Aktualisiert
            if (meals.isEmpty()) {
                System.out.println("No meals found.");
            } else {
                System.out.println("Category: " + categoryToPrint);
                for (Meal meal : meals) {
                    System.out.println();
                    //System.out.println("Category: " + meal.getCategory());
                    System.out.println("Name: " + meal.getName());
                    System.out.println("Ingredients:");
                    for (String ingredient : meal.getIngredients()) {
                        System.out.println(ingredient.trim());
                    }
                }
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to retrieve meals from database.");
        }
    }
}
