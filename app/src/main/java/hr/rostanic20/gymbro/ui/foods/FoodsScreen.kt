package hr.rostanic20.gymbro.ui.foods

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.formatCount
import hr.rostanic20.gymbro.ui.common.formatKg
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

@Composable
fun FoodsScreen(
    onBack: () -> Unit,
    onEditFood: (foodId: Long?) -> Unit,
    viewModel: FoodsViewModel = koinViewModel(),
) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.foods_title), onBack = onBack) {
            TextButton(onClick = { onEditFood(null) }) {
                Text(stringResource(R.string.foods_add))
            }
        }
        foods?.let { list ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.s16),
            ) {
                items(list, key = { it.id }) { food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditFood(food.id) }
                            .padding(start = spacing.s16, end = spacing.s4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = spacing.s12),
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
                        IconButton(onClick = { viewModel.toggleFavourite(food) }) {
                            Icon(
                                imageVector = if (food.isFavourite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = stringResource(
                                    if (food.isFavourite) R.string.favourite_remove else R.string.favourite_add,
                                ),
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun FoodEditScreen(
    foodId: Long?,
    onDone: () -> Unit,
    viewModel: FoodEditViewModel = koinViewModel(parameters = { parametersOf(foodId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    ObserveAsEvents(viewModel.events) { onDone() }
    if (state.loaded) {
        FoodEditContent(food = state.food, onBack = onDone, onSave = viewModel::save)
    }
}

@Composable
private fun FoodEditContent(food: Food?, onBack: () -> Unit, onSave: (FoodDraft) -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val initial = remember(food, locale) { food?.toForm(locale) ?: FoodForm() }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var kcal by rememberSaveable { mutableStateOf(initial.kcal) }
    var protein by rememberSaveable { mutableStateOf(initial.protein) }
    var carbs by rememberSaveable { mutableStateOf(initial.carbs) }
    var fat by rememberSaveable { mutableStateOf(initial.fat) }
    var unitName by rememberSaveable { mutableStateOf(initial.unitName) }
    var unitGrams by rememberSaveable { mutableStateOf(initial.unitGrams) }
    val draft = FoodForm(name, kcal, protein, carbs, fat, unitName, unitGrams).validate(food?.id)

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(if (food == null) R.string.food_new_title else R.string.food_edit_title),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.s16, end = spacing.s16, bottom = spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s12),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.food_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DecimalField(value = kcal, onValueChange = { kcal = it }, label = R.string.food_kcal)
            DecimalField(value = protein, onValueChange = { protein = it }, label = R.string.food_protein)
            DecimalField(value = carbs, onValueChange = { carbs = it }, label = R.string.food_carbs)
            DecimalField(value = fat, onValueChange = { fat = it }, label = R.string.food_fat)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s8)) {
                OutlinedTextField(
                    value = unitName,
                    onValueChange = { unitName = it },
                    label = { Text(stringResource(R.string.food_unit_name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                DecimalField(
                    value = unitGrams,
                    onValueChange = { unitGrams = it },
                    label = R.string.food_unit_grams,
                    modifier = Modifier.weight(1f),
                )
            }
            if (draft == null && name.isNotBlank()) {
                Text(
                    text = stringResource(R.string.food_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = { draft?.let(onSave) }, enabled = draft != null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    val spacing = LocalSpacing.current
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
        Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        actions()
    }
}
