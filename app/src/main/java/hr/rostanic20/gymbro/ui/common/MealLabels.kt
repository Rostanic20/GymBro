package hr.rostanic20.gymbro.ui.common

import androidx.annotation.StringRes
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.CanteenPlate
import hr.rostanic20.gymbro.domain.model.Meal

@get:StringRes
val Meal.labelRes: Int
    get() = when (this) {
        Meal.BREAKFAST_SHAKE -> R.string.meal_breakfast_shake
        Meal.DESK_SNACK -> R.string.meal_desk_snack
        Meal.LUNCH -> R.string.meal_lunch
        Meal.PRE_GYM -> R.string.meal_pre_gym
        Meal.POST_GYM_SHAKE -> R.string.meal_post_gym_shake
        Meal.DINNER -> R.string.meal_dinner
    }

@get:StringRes
val Meal.tipRes: Int
    get() = when (this) {
        Meal.BREAKFAST_SHAKE -> R.string.meal_tip_breakfast_shake
        Meal.DESK_SNACK -> R.string.meal_tip_desk_snack
        Meal.LUNCH -> R.string.meal_tip_lunch
        Meal.PRE_GYM -> R.string.meal_tip_pre_gym
        Meal.POST_GYM_SHAKE -> R.string.meal_tip_post_gym_shake
        Meal.DINNER -> R.string.meal_tip_dinner
    }

@get:StringRes
val CanteenPlate.labelRes: Int
    get() = when (this) {
        CanteenPlate.MEAT_OR_FISH -> R.string.canteen_meat
        CanteenPlate.STEW -> R.string.canteen_stew
        CanteenPlate.PASTA_LITTLE_MEAT -> R.string.canteen_pasta
        CanteenPlate.SOUP_NO_MEAT -> R.string.canteen_soup
    }

@get:StringRes
val CanteenPlate.tipRes: Int
    get() = when (this) {
        CanteenPlate.MEAT_OR_FISH -> R.string.canteen_tip_meat
        CanteenPlate.STEW -> R.string.canteen_tip_stew
        CanteenPlate.PASTA_LITTLE_MEAT -> R.string.canteen_tip_pasta
        CanteenPlate.SOUP_NO_MEAT -> R.string.canteen_tip_soup
    }
