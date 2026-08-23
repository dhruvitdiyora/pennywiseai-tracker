package com.pennywiseai.shared.data.bootstrap

object DefaultCategoryData {
    /**
     * @param iconName drawable name in the app's `drawable-nodpi` set, resolved at
     *   runtime by `IconResolutionUtils.nameToResId`. A *name*, never a resource id:
     *   Android renumbers ids between builds, so an id stored in the database or a
     *   backup would eventually point at the wrong picture.
     */
    data class CategorySeed(
        val name: String,
        val colorHex: String,
        val isIncome: Boolean,
        val iconName: String
    )

    val ALL: List<CategorySeed> = listOf(
        CategorySeed("Food & Dining",      "#FC8019", false, "type_food_dining"),
        CategorySeed("Groceries",          "#5AC85A", false, "type_groceries_basket"),
        CategorySeed("Transportation",     "#000000", false, "type_travel_transport_bus"),
        CategorySeed("Shopping",           "#FF9900", false, "type_shopping_shopping_bags"),
        CategorySeed("Bills & Utilities",  "#4CAF50", false, "type_tool_electronic_light_bulb"),
        CategorySeed("Entertainment",      "#E50914", false, "type_tool_electronic_clapper_board"),
        CategorySeed("Healthcare",         "#10847E", false, "type_health_pill"),
        CategorySeed("Investments",        "#00D09C", false, "type_finance_chart_increasing"),
        CategorySeed("Banking",            "#004C8F", false, "type_finance_bank"),
        CategorySeed("Personal Care",      "#6A4C93", false, "type_groceries_soap"),
        CategorySeed("Education",          "#673AB7", false, "type_event_and_place_graduation_cap"),
        CategorySeed("Mobile",             "#2A3890", false, "type_tool_electronic_mobile_phone"),
        CategorySeed("Fitness",            "#FF3278", false, "type_sports_flexed_biceps_light"),
        CategorySeed("Insurance",          "#0066CC", false, "type_finance_insurance"),
        CategorySeed("Travel",             "#00BCD4", false, "type_travel_transport_airplane"),
        CategorySeed("Salary",             "#4CAF50", true, "type_finance_money_bag"),
        CategorySeed("Income",             "#4CAF50", true, "type_finance_money_with_wings"),
        CategorySeed("Others",             "#757575", false, "type_tool_electronic_gear")
    )

    /**
     * @param parentName matched against [CategorySeed.name] at seed time. Cashiro
     *   files its subcategory seeds under a category called "Food & Drinks",
     *   which does not exist here — ours is "Food & Dining" — so copying its list
     *   verbatim would have seeded nothing at all, silently.
     */
    data class SubcategorySeed(
        val parentName: String,
        val name: String,
        val iconName: String
    )

    val SUBCATEGORIES: List<SubcategorySeed> = listOf(
        SubcategorySeed("Food & Dining",     "Eat out",          "type_food_dining"),
        SubcategorySeed("Food & Dining",     "Takeaway",         "type_food_takeout"),
        SubcategorySeed("Food & Dining",     "Tea & Coffee",     "type_beverages_coffee"),
        SubcategorySeed("Food & Dining",     "Fast food",        "type_food_hamburger"),
        SubcategorySeed("Food & Dining",     "Snacks",           "type_snack_popcorn"),

        SubcategorySeed("Transportation",    "Fuel",             "type_travel_transport_fuel_pump"),
        SubcategorySeed("Transportation",    "Cab",              "type_travel_transport_taxi"),
        SubcategorySeed("Transportation",    "Public transport", "type_travel_transport_bus"),

        SubcategorySeed("Bills & Utilities", "Electricity",      "type_tool_electronic_light_bulb"),
        SubcategorySeed("Bills & Utilities", "Internet",         "type_tool_electronic_laptop"),
        SubcategorySeed("Bills & Utilities", "Rent",             "type_event_and_place_house")
    )
}
