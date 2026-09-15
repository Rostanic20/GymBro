package hr.rostanic20.gymbro.data

import hr.rostanic20.gymbro.db.Body_weight
import hr.rostanic20.gymbro.db.Food
import hr.rostanic20.gymbro.db.Food_log
import hr.rostanic20.gymbro.db.Progress_photo
import hr.rostanic20.gymbro.db.SelectAlternatives
import hr.rostanic20.gymbro.db.SelectFinishedSessions
import hr.rostanic20.gymbro.db.SelectFirstSetsForExercise
import hr.rostanic20.gymbro.db.SelectProgram
import hr.rostanic20.gymbro.db.SelectRecipeItems
import hr.rostanic20.gymbro.db.SelectTopSetHistory
import hr.rostanic20.gymbro.db.Set_entry
import hr.rostanic20.gymbro.db.Waist
import hr.rostanic20.gymbro.db.Workout_session

typealias ProgramRowEntity = SelectProgram
typealias AlternativeRowEntity = SelectAlternatives
typealias SessionEntity = Workout_session
typealias SetEntity = Set_entry
typealias TopSetRowEntity = SelectFirstSetsForExercise
typealias SessionSummaryEntity = SelectFinishedSessions
typealias TopSetHistoryEntity = SelectTopSetHistory
typealias FoodEntity = Food
typealias RecipeRowEntity = SelectRecipeItems
typealias FoodLogEntity = Food_log
typealias BodyWeightEntity = Body_weight
typealias WaistEntity = Waist
typealias PhotoEntity = Progress_photo
