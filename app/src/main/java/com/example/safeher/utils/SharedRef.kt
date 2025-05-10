package com.example.safeher.utils


import android.content.Context
import android.content.Context.MODE_PRIVATE

fun Context.getStringShareRef(resource: String, library:String): String {
    val sharedPref = this.getSharedPreferences(library, MODE_PRIVATE)
    return sharedPref?.getString(resource, "") ?: ""
}
fun Context.getStringListShareRef(resource: String, library:String): List<String> {
    val sharedPref = this.getSharedPreferences(library, MODE_PRIVATE)
    return sharedPref?.getStringSet(resource, setOf())?.toList() ?: listOf()
}
fun Context.setStringShareRef(resource: String, value: String, library:String) {
    val sharedPref = this.getSharedPreferences(library, MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString(resource, value)
        apply()
    }
}
fun Context.setStringListShareRef(resource: String, value: List<String>, library:String) {
    val sharedPref = this.getSharedPreferences(library, MODE_PRIVATE)
    with(sharedPref.edit()) {
        putStringSet(resource, value.toSet())
        apply()
    }
}
fun Context.getBooleanShareRef(resource: String, library:String): Boolean {
    val sharedPref = this.getSharedPreferences(library, MODE_PRIVATE)
    return sharedPref?.getBoolean(resource, false) == true
}
fun Context.setBooleanShareRef(resource: String, value: Boolean, library:String) {
    val sharedPref = this.getSharedPreferences(library, MODE_PRIVATE)
    with(sharedPref.edit()) {
        putBoolean(resource, value)
        apply()
    }
}
/*
fun Context.setRecipeShareRef(recipe: Recipe){
    val sharedPref = this.getSharedPreferences("recipeDetails", MODE_PRIVATE)
    sharedPref.edit() {
        val recipeJson = Gson().toJson(recipe)
        putString("recipe", recipeJson)
        apply()
    }
}
fun Context.getRecipeShareRef(): Recipe? {
    val sharedPref = this.getSharedPreferences("recipeDetails", MODE_PRIVATE)
    val recipeJson = sharedPref.getString("recipe", "")
    return Gson().fromJson(recipeJson, Recipe::class.java)
}*/
