package mealplanner;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:postgresql:meals_db";
    private static final String USER = "postgres";
    private static final String PASS = "1111";
    private Connection connection;
    public DatabaseManager() throws SQLException {
        this.connection = DriverManager.getConnection(DB_URL, USER, PASS);
        this.connection.setAutoCommit(true);
        createTables();
    }

    public boolean isPlanAvailable() throws SQLException {
        String query = "SELECT COUNT(*) FROM plan";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public List<Meal> getMealsByDay(String day) throws SQLException {
        List<Meal> meals = new ArrayList<>();
        String query = "SELECT * FROM meals JOIN plan ON meals.meal_id = plan.meal_id WHERE plan.day = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, day);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int meal_id = rs.getInt("meal_id");
                String category = rs.getString("category");
                String mealName = rs.getString("meal");
                String[] ingredients = getIngredients(meal_id);
                meals.add(new Meal(meal_id, category, mealName, ingredients));
            }
        }
        return meals;
    }


    public void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS meals (" +
                                    "meal_id INTEGER PRIMARY KEY," +
                                    "category VARCHAR(255)," +
                                    "meal VARCHAR(255)" +
                                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ingredients (" +
                                    "ingredient_id INTEGER PRIMARY KEY," +
                                    "ingredient VARCHAR(255)," +
                                    "meal_id INTEGER REFERENCES meals(meal_id)" +
                                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS plan (" +
                                    "plan_id INTEGER PRIMARY KEY," +
                                    "day varchar(255)," +
                                    "meal_id INTEGER REFERENCES meals(meal_id)," +
                                    "meal_category varchar(255)," +
                                    "meal_option varchar(255)" +
                                    ")");
        }
    }
    public void dropTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS plan");
            System.out.println("Drop plan");
            statement.executeUpdate("DROP TABLE IF EXISTS ingredients");
            System.out.println("Drop ingredients");
            statement.executeUpdate("DROP TABLE IF EXISTS meals");
            System.out.println("Drop meals");
        }
    }

    public void addMeal(Meal meal) throws SQLException {
        int nextMealId = getNextMealId();
        String query = "INSERT INTO meals (meal_id, category, meal) VALUES (?, ?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, nextMealId);
            pstmt.setString(2, meal.getCategory());
            pstmt.setString(3, meal.getName());
            pstmt.executeUpdate();
            for (String ingredient : meal.getIngredients()) {
                addIngredient(nextMealId, ingredient);
            }
        }
    }

    private int getNextMealId() throws SQLException {
        String query = "SELECT MAX(meal_id) FROM meals;";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getInt(1) + 1;
            } else {
                return 1;  // Falls die Tabelle leer ist, starte mit 1
            }
        }
    }
//ANFANG KOPIE
public void addPlanBreakfast(String wochentag, int mealId) throws SQLException {
    int nextPlanId = getNextPlanId();
    String query = "INSERT INTO plan (plan_id, day, breakfast) VALUES (?, ?, ?);";
    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
        pstmt.setInt(1, nextPlanId);
        pstmt.setString(2, wochentag);
        pstmt.setInt(3, mealId);
        pstmt.executeUpdate();
    }
}



//ENDE KOPIE
    public void addIngredient(int meal_id, String ingredient) throws SQLException {
        int nextIngredientId = getNextIngredientId();
        String query = "INSERT INTO ingredients (ingredient_id, ingredient, meal_id) VALUES (?, ?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, nextIngredientId);
            pstmt.setString(2, ingredient);
            pstmt.setInt(3, meal_id);
            pstmt.executeUpdate();
        }
    }
    private int getNextIngredientId() throws SQLException {
        String query = "SELECT MAX(ingredient_id) FROM ingredients;";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getInt(1) + 1;
            } else {
                return 1;  // Falls die Tabelle leer ist, starte mit 1
            }
        }
    }
    public List<Meal> getMeals() throws SQLException {
        List<Meal> meals = new ArrayList<>();
        String query = "SELECT * FROM meals";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                int meal_id = rs.getInt("meal_id");
                String category = rs.getString("category");
                String mealName = rs.getString("meal");
                String[] ingredients = getIngredients(meal_id);
                meals.add(new Meal(meal_id, category, mealName, ingredients));
            }
        }
        return meals;
    }
    public List<Meal> getMealsByCategory(String desiredCategory) throws SQLException {
        List<Meal> meals = new ArrayList<>();
        String query = "SELECT * FROM meals WHERE category = ?";

        try(PreparedStatement pstmt = connection.prepareStatement(query))
        {
            pstmt.setString(1, desiredCategory);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int meal_id = rs.getInt("meal_id");
                String category = rs.getString("category");
                String mealName = rs.getString("meal");
                String[] ingredients = getIngredients(meal_id);
                meals.add(new Meal(meal_id, category, mealName, ingredients));
            }
        }
        return meals;
    }
    public String[] getIngredients(int meal_id) throws SQLException {
        List<String> ingredientsList = new ArrayList<>();
        String query = "SELECT ingredient FROM ingredients WHERE meal_id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, meal_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ingredientsList.add(rs.getString("ingredient"));
            }
        }
        return ingredientsList.toArray(new String[0]);
    }
    // Methode zum Schließen der Datenbankverbindung
    public void closeConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
    }

    public void addPlan(int meal_id, String day, String meal_category, String meal_option)
    {
        //Neue Plan ID holen
        int nextPlanId = getNextPlanId();
        //SQL Query vorbereiten
        String query = null;
        query = "INSERT INTO plan (plan_id, day, meal_id, meal_category, meal_option) VALUES (?, ?, ?, ?, ?);";


        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, nextPlanId);
            pstmt.setString(2, day);
            pstmt.setInt(3, meal_id);
            pstmt.setString(4, meal_category);//Logik finden wie category geholt wird
            pstmt.setString(5, meal_option);//Logik wie meal_option gefunden wird
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteOldPlan() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM plan");
        }
    }


    public String getPlanByDay(String day, String timeToEat) {
        String query = "SELECT meal_option FROM plan WHERE day = ? AND meal_category = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, day);  // Setze den Wert für den ersten Platzhalter (?)
            pstmt.setString(2, timeToEat);  // Setze den Wert für den zweiten Platzhalter (?)
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("meal_option");  // Gibt den Wert der Spalte 'meal_option' zurück
            }
        } catch (SQLException e) {
            e.printStackTrace();  // Fehlerbehandlung, z.B. Ausgabe der Fehlermeldung
        }
        return null;  // Gibt null zurück, wenn kein Eintrag gefunden wurde oder ein Fehler aufgetreten ist
    }


    private int getNextPlanId()
    {
        String query = "SELECT MAX(plan_id) FROM plan;";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getInt(1) + 1;
            } else {
                return 1;  // Falls die Tabelle leer ist, starte mit 1
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

}
