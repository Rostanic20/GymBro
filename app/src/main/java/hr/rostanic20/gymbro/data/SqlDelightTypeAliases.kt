package hr.rostanic20.gymbro.data

import hr.rostanic20.gymbro.db.SelectAlternatives
import hr.rostanic20.gymbro.db.SelectFirstSetsForExercise
import hr.rostanic20.gymbro.db.SelectProgram
import hr.rostanic20.gymbro.db.Set_entry
import hr.rostanic20.gymbro.db.Workout_session

typealias ProgramRowEntity = SelectProgram
typealias AlternativeRowEntity = SelectAlternatives
typealias SessionEntity = Workout_session
typealias SetEntity = Set_entry
typealias TopSetRowEntity = SelectFirstSetsForExercise
