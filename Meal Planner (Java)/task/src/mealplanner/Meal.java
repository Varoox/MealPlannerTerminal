package mealplanner;

public class Meal {

    private final int mealId;  // Neu
    private final String category;
    private final String name;
    private final String[] ingredients;

    public Meal(int mealId, String category, String name, String[] ingredients) {  // Aktualisiert
        this.mealId = mealId;  // Neu
        this.category = category;
        this.name = name;
        this.ingredients = ingredients;
    }

    public int getMealId() {  // Neu
        return mealId;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String[] getIngredients() {
        return ingredients;
    }
}

