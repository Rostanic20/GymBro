package hr.rostanic20.gymbro.ui.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.CanteenPlate
import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodLogEntry
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.Recipe
import hr.rostanic20.gymbro.domain.nutrition
import hr.rostanic20.gymbro.domain.nutritionFor
import hr.rostanic20.gymbro.domain.total
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.Tag
import hr.rostanic20.gymbro.ui.common.formatCount
import hr.rostanic20.gymbro.ui.common.formatKg
import hr.rostanic20.gymbro.ui.common.labelRes
import hr.rostanic20.gymbro.ui.common.parseKg
import hr.rostanic20.gymbro.ui.common.rememberDayFormatter
import hr.rostanic20.gymbro.ui.common.tipRes
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private const val MIN_GRAMS = 1.0
private const val MAX_GRAMS = 2000.0
private const val DEFAULT_GRAMS = 100.0

data class MealActions(
    val logRecipe: (Recipe) -> Unit,
    val logFood: (Food, Double) -> Unit,
    val logCanteen: (CanteenPlate, String) -> Unit,
    val repeatYesterday: () -> Unit,
    val deleteEntry: (Long) -> Unit,
)

@Composable
fun MealScreen(
    epochDay: Long,
    slot: Int,
    onBack: () -> Unit,
    viewModel: MealViewModel = koinViewModel(parameters = { parametersOf(epochDay, slot) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    val actions = remember(viewModel) {
        MealActions(
            logRecipe = viewModel::logRecipe,
            logFood = viewModel::logFood,
            logCanteen = viewModel::logCanteen,
            repeatYesterday = viewModel::repeatYesterday,
            deleteEntry = viewModel::deleteEntry,
        )
    }
    state?.let { MealContent(state = it, onBack = onBack, actions = actions) }
}

@Composable
private fun MealContent(state: MealUiState, onBack: () -> Unit, actions: MealActions) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val resources = LocalResources.current
    val dayFormatter = rememberDayFormatter()
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    var pickingFood by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(state.meal.labelRes), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(
                        R.string.meal_subtitle,
                        state.meal.time.format(timeFormatter),
                        state.date.format(dayFormatter),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = spacing.s16, end = spacing.s16, bottom = spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s16),
        ) {
            item {
                Text(
                    text = stringResource(state.meal.tipRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { TotalCard(total = state.entries.total()) }
            if (state.entries.isNotEmpty()) {
                item { EntriesCard(entries = state.entries, onDelete = actions.deleteEntry) }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s8),
                ) {
                    OutlinedButton(
                        onClick = actions.repeatYesterday,
                        enabled = state.yesterdayEntries > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.repeat_yesterday))
                    }
                    Button(onClick = { pickingFood = true }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.add_food))
                    }
                }
            }
            if (state.meal == Meal.LUNCH) {
                item {
                    CanteenCard(onLog = { plate -> actions.logCanteen(plate, resources.getString(plate.labelRes)) })
                }
            }
            item {
                Text(text = stringResource(R.string.recipes_title), style = MaterialTheme.typography.titleMedium)
            }
            items(state.recipes, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    planned = recipe.id == state.meal.suggestedRecipeId,
                    onLog = { actions.logRecipe(recipe) },
                )
            }
        }
    }

    if (pickingFood) {
        FoodPickerDialog(
            foods = state.foods,
            onDismiss = { pickingFood = false },
            onLog = { food, grams ->
                actions.logFood(food, grams)
                pickingFood = false
            },
        )
    }
}

@Composable
private fun TotalCard(total: Nutrition) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                R.string.meal_total,
                formatCount(total.kcal.roundToInt(), locale),
                formatCount(total.proteinG.roundToInt(), locale),
                formatCount(total.carbsG.roundToInt(), locale),
                formatCount(total.fatG.roundToInt(), locale),
            ),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(spacing.s16),
        )
    }
}

@Composable
private fun EntriesCard(entries: List<FoodLogEntry>, onDelete: (Long) -> Unit) {
    val spacing = LocalSpacing.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = spacing.s16, end = spacing.s4, top = spacing.s4, bottom = spacing.s4)) {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = spacing.s8),
                    ) {
                        Text(text = entry.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = entryDetail(entry),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(entry.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_entry),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun entryDetail(entry: FoodLogEntry): String {
    val locale = LocalLocale.current.platformLocale
    val nutrition = stringResource(
        R.string.meal_entry_nutrition,
        formatCount(entry.nutrition.kcal.roundToInt(), locale),
        formatCount(entry.nutrition.proteinG.roundToInt(), locale),
    )
    return entry.grams?.let { stringResource(R.string.meal_entry_with_grams, formatKg(it, locale), nutrition) }
        ?: nutrition
}

@Composable
private fun CanteenCard(onLog: (CanteenPlate) -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = spacing.s8)) {
            Text(
                text = stringResource(R.string.canteen_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = spacing.s16, vertical = spacing.s8),
            )
            CanteenPlate.entries.forEach { plate ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLog(plate) }
                        .padding(horizontal = spacing.s16, vertical = spacing.s8),
                    verticalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Text(text = stringResource(plate.labelRes), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(
                            R.string.canteen_estimate,
                            formatCount(plate.nutrition.proteinG.roundToInt(), locale),
                            formatCount(plate.nutrition.kcal.roundToInt(), locale),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(plate.tipRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, planned: Boolean, onLog: () -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val nutrition = recipe.nutrition
    val itemLabels = recipe.items.map { stringResource(R.string.recipe_item, it.food.name, formatKg(it.grams, locale)) }
    Card(onClick = onLog, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(spacing.s16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s12),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.s4),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s8),
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (planned) {
                        Tag(
                            text = stringResource(R.string.recipe_planned),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    text = itemLabels.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.meal_kcal, formatCount(nutrition.kcal.roundToInt(), locale)),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.protein_grams, formatCount(nutrition.proteinG.roundToInt(), locale)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(imageVector = Icons.Outlined.Add, contentDescription = stringResource(R.string.log_food))
        }
    }
}

@Composable
private fun FoodPickerDialog(foods: List<Food>, onDismiss: () -> Unit, onLog: (Food, Double) -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = foods.firstOrNull { it.id == selectedId }
    var gramsText by rememberSaveable(selectedId) {
        mutableStateOf(formatKg(selected?.unitGrams ?: DEFAULT_GRAMS, locale))
    }
    val grams = parseKg(gramsText)?.takeIf { it in MIN_GRAMS..MAX_GRAMS }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(selected?.name ?: stringResource(R.string.food_picker_title)) },
        text = {
            if (selected == null) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s8)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.food_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val filtered = foods.filter { it.name.contains(query.trim(), ignoreCase = true) }
                    LazyColumn(modifier = Modifier.heightIn(max = dimensionResource(R.dimen.food_picker_max_height))) {
                        items(filtered, key = { it.id }) { food ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedId = food.id }
                                    .padding(vertical = spacing.s8),
                            ) {
                                Text(text = food.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = stringResource(
                                        R.string.food_per_100g,
                                        formatCount(food.per100g.kcal.roundToInt(), locale),
                                        formatKg(food.per100g.proteinG, locale),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s8)) {
                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it },
                        label = { Text(stringResource(R.string.food_grams)) },
                        isError = grams == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val unitName = selected.unitName
                    val unitGrams = selected.unitGrams
                    if (unitName != null && unitGrams != null) {
                        AssistChip(
                            onClick = { gramsText = formatKg(unitGrams, locale) },
                            label = { Text(stringResource(R.string.food_unit_chip, unitName, formatKg(unitGrams, locale))) },
                        )
                    }
                    grams?.let {
                        val nutrition = selected.nutritionFor(it)
                        Text(
                            text = stringResource(
                                R.string.meal_entry_nutrition,
                                formatCount(nutrition.kcal.roundToInt(), locale),
                                formatCount(nutrition.proteinG.roundToInt(), locale),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    TextButton(onClick = { selectedId = null }) {
                        Text(stringResource(R.string.food_pick_another))
                    }
                }
            }
        },
        confirmButton = {
            if (selected != null) {
                TextButton(onClick = { grams?.let { onLog(selected, it) } }, enabled = grams != null) {
                    Text(stringResource(R.string.log_food))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
